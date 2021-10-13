package de.imise.integrator.controller

import de.imise.integrator.extensions.ResponseType
import de.imise.integrator.view.MainView
import tornadofx.*
import java.io.File
import java.nio.file.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.system.exitProcess

data class InMemoryFile(val baseName: String, val content: ByteArray, val extension: String)

class RemoteController : Controller() {
    enum class ConnectionTool {
        PLINK, PSCP
    }

    private val mainView: MainView by inject()
    private val logging: LoggingController by inject()

    fun waitForFile(fi: File): Boolean {
        val name = Paths.get(fi.absolutePath)
        val targetDir = name.parent
        if (fi.exists()) {
            return true
        }
        FileSystems.getDefault().newWatchService().use { watchService ->
            targetDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE)
            val watchKey = watchService.poll()
            do {
                for (event in watchKey.pollEvents()) {
                    if ((event.context() as Path) == name) {
                        return true
                    }
                }
                val valid = watchKey.reset()
            } while (valid)
        }
        return false
    }

    inner class FileTransfer {
        private val tmpFolder = "/home/${mainView.usernameField.text}/.tmp"
        private val connection = "${mainView.usernameField.text}@${mainView.hostField.text}"
        private val subFolderTransfer = mainView.bratTransferSubfolderField.text.takeIf { !it.isNullOrBlank() } ?: mainView.pipelineNameField.text
        private val finalDestinationTransfer = "${mainView.bratDataFolderField.text.trimEnd('/')}/${subFolderTransfer?.trimEnd('/')}/"
        private val subFolderReceive = mainView.bratReceiveSubfolderField.text
        private val finalDestinationReceive = "${mainView.bratDataFolderField.text.trimEnd('/')}/${subFolderReceive.trimEnd('/')}"
        private fun Process.log() = also { this.inputStream.bufferedReader().use { logging.logBrat(it.readText()) } }

        private fun processBuilder(connection: ConnectionTool): Array<String> {
            return when (connection) {
                ConnectionTool.PLINK -> arrayOf("plink.exe", "-no-antispoof")
                ConnectionTool.PSCP -> arrayOf("pscp.exe")
            }.plus(listOf("-P", mainView.remotePortField.text, "-pw", mainView.passwordField.text))
        }

        private fun temporaryFileStorage(fi: File) {
            // Create temporary folder
            ProcessBuilder(
                *processBuilder(ConnectionTool.PLINK),
                connection,
                "mkdir --parents $tmpFolder"
            )
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
                .log()
                .waitFor()

            // Transfer Bulk.zip to tmp folder
            ProcessBuilder(
                *processBuilder(ConnectionTool.PSCP),
                fi.absolutePath,
                "$connection:$tmpFolder"
            )
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
                .log()
                .waitFor()

            // Unzip Bulk.zip
            ProcessBuilder(
                *processBuilder(ConnectionTool.PLINK),
                connection,
                "unzip $tmpFolder/${fi.name} -d $tmpFolder/"
            )
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
                .log()
                .waitFor()

            logging.logBrat("Transferred files to remote...")
        }

        private fun transferFileIndirectly(fi: File): Process {
            temporaryFileStorage(fi)
            val dollar = "$"
            val commandFile = File("command_remote_transfer.sh")

            commandFile.outputStream().bufferedWriter().use {
                it.write(
                    """
                        mkdir --parents $finalDestinationTransfer
                        find $tmpFolder/${mainView.pipelineNameField.text}/ -maxdepth 1 -iname '*' -type 'f' -execdir touch $finalDestinationTransfer{} \;
                        for ext in txt ann json
                        do
                            for file in $tmpFolder/${mainView.pipelineNameField.text}/*.${dollar}ext
                            do
                                cat "${dollar}file" > $finalDestinationTransfer"${dollar}( basename ${dollar}file )"
                            done
                        done
                    """.trimIndent()
                )
            }
            return ProcessBuilder(
                *processBuilder(ConnectionTool.PLINK),
                connection, "-m", commandFile.absolutePath
            )
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
                .log()
        }

        fun transferData(response: List<ResponseType>) {
            val bulkZip = File("bulk.zip")
            bulkZip.outputStream().use {  fos ->
                ZipOutputStream(fos).use { zos ->
                    zos.putNextEntry(ZipEntry("${mainView.pipelineNameField.text}/"))
                    OutputTransformationController.transformToBrat(response as List<AverbisResponse>).forEach { pair ->
                        pair.toList().forEach { entry ->
                            val zipEntry = ZipEntry("${mainView.pipelineNameField.text}/${entry.fileName}.${entry.extension}")
                            zos.putNextEntry(zipEntry)
                            zos.write(entry.content.toByteArray())
                            zos.closeEntry()
                        }
                    }
                    OutputTransformationController.getFilteredJson(response).forEach {
                        val zipEntry = ZipEntry("${mainView.pipelineNameField.text}/${it.fileName}.${it.extension}")
                        zos.putNextEntry(zipEntry)
                        zos.write(it.content.toByteArray())
                        zos.closeEntry()
                    }
                }
            }
            waitForFile(bulkZip) //ToDo:
            transferFileIndirectly(bulkZip).waitFor()
            logging.logBrat("Extracted files to brat folder...")
            // ToDo: delete .tmp on remote; or at least content thereof
            // ToDo: remove "command_***.sh" from local dir
            bulkZip.delete()
        }

        fun getDataFromRemote() : List<InMemoryFile> {
            // ToDo: create .tmp beforehand (if it doesn't exist)
            val bulkZipName = "bratBulk.zip"
            // Zip all files
            ProcessBuilder(
                *processBuilder(ConnectionTool.PLINK),
                connection,
                "zip -r -j", "${tmpFolder}/${bulkZipName}", finalDestinationReceive
            )
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
                .log()
                .waitFor()

            // Transfer bratBulk.zip from remote .tmp/ to local
            ProcessBuilder(
                *processBuilder(ConnectionTool.PSCP),
                "$connection:$tmpFolder/${bulkZipName}",
                Paths.get("").toAbsolutePath().toString()
            )
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
                .log()
                .waitFor()

            // Unzip and return List of files
            val returnList = mutableListOf<InMemoryFile>()
            File(bulkZipName).inputStream().use { fis ->
                ZipInputStream(fis).use { zis ->
                    generateSequence { zis.nextEntry }
                        .filterNot { it.isDirectory }
                        .filter { listOf("json", "ann").contains(it.name.substringAfterLast(".")) }
                        .map {
                            InMemoryFile(
                                baseName = it.name.substringBeforeLast(".").substringAfterLast("/"),
                                content = zis.readBytes(),
                                extension = it.name.substringAfterLast(".")
                            )
                        }.forEach { returnList.add(it) }
                }
            }
            return returnList.toList()
        }
    }
}