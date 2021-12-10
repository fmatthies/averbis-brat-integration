package de.imise.integrator.controller

import de.imise.integrator.model.BratReceive
import de.imise.integrator.model.BratSetup
import de.imise.integrator.model.BratTransfer
import de.imise.integrator.view.MainView
import tornadofx.*
import java.io.File
import java.nio.file.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class InMemoryFile(val baseName: String, val content: ByteArray, val extension: String)

class RemoteController : Controller() {
    enum class ConnectionTool {
        PLINK, PSCP
    }

    val BRAT_BULKZIP_NAME = "bratBulk.zip"
    val BULKZIP_NAME = "bulk.zip"

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
        private var tmpFolder: String? = null
        private var connection: String? = null
        private var finalDestinationTransfer: String? = null

        private var finalDestinationReceive: String? = null

        private fun Process.log() = also { this.inputStream.bufferedReader().use { logging.logBrat(it.readText()) } }

        private fun setup(bratSetup: BratSetup) {
            tmpFolder = "/home/${bratSetup.username}/.tmp"
            connection = "${bratSetup.username}@${bratSetup.host}"
        }

        fun setupTransfer(bratSetup: BratSetup, bratTransfer: BratTransfer) {
            setup(bratSetup)
            val subFolderTransfer = bratTransfer.subfolder.takeIf { !it.isNullOrBlank() } ?: mainView.pipelineNameField.text
            finalDestinationTransfer = "${bratSetup.dataFolder?.trimEnd('/')}/${subFolderTransfer?.trimEnd('/')}/"
        }

        fun setupReceive(bratSetup: BratSetup, bratReceive: BratReceive) {
            setup(bratSetup)
            val subfolderReceive = bratReceive.subfolder
            finalDestinationReceive = "${bratSetup.dataFolder.trimEnd('/')}/${subfolderReceive.trimEnd('/')}"
        }

        private fun processBuilder(connection: ConnectionTool): Array<String> {
            return when (connection) {
                ConnectionTool.PLINK -> arrayOf("exec\\plink.exe", "-no-antispoof")
                ConnectionTool.PSCP -> arrayOf("exec\\pscp.exe")
            }.plus(listOf("-P", mainView.remotePortField.text, "-pw", mainView.passwordField.text))
        }

        private fun temporaryFileStorage(fi: File) {
            /* Create temporary folder */
            ProcessBuilder(
                *processBuilder(ConnectionTool.PLINK),
                connection,
                "mkdir --parents $tmpFolder"
            )
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
                .log()
                .waitFor()

            /* Transfer Bulk.zip to tmp folder */
            ProcessBuilder(
                *processBuilder(ConnectionTool.PSCP),
                fi.absolutePath,
                "$connection:$tmpFolder"
            )
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
                .log()
                .waitFor()

            /* Unzip Bulk.zip */
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

        private fun transferFileIndirectly(fi: File, commandFile: File): Process {
            temporaryFileStorage(fi)
            val dollar = "$"

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

        fun transferData(response: List<AverbisResponse>, bratAnnotationValues: List<String>) {
            val bulkZip = File(BULKZIP_NAME)
            val commandFile = File("command_remote_transfer.sh")

            bulkZip.outputStream().use {  fos ->
                ZipOutputStream(fos).use { zos ->
                    zos.putNextEntry(ZipEntry("${mainView.pipelineNameField.text}/"))
                    OutputTransformationController.transformToBrat(response, bratAnnotationValues).forEach { pair ->
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
            waitForFile(bulkZip)
            transferFileIndirectly(bulkZip, commandFile).waitFor()
            logging.logBrat("Extracted files to brat folder...")
            ProcessBuilder(
                *processBuilder(ConnectionTool.PLINK),
                connection,
                "rm -r $tmpFolder/*"
            )
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
                .waitFor()
            commandFile.delete()
            bulkZip.delete()
        }

        fun getDataFromRemote() : List<InMemoryFile> {
            val bulkZipName = BRAT_BULKZIP_NAME
            /* Zip all files */
            ProcessBuilder(
                *processBuilder(ConnectionTool.PLINK),
                connection,
                "mkdir --parents $tmpFolder", "&&",
                "zip -r -j", "${tmpFolder}/${bulkZipName}", finalDestinationReceive
            )
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
                .log()
                .waitFor()

            /* Transfer bratBulk.zip from remote .tmp/ to local */
            ProcessBuilder(
                *processBuilder(ConnectionTool.PSCP),
                "$connection:$tmpFolder/${bulkZipName}",
                Paths.get("").toAbsolutePath().toString()
            )
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
                .log()
                .waitFor()

            /* Delete bratBulk.zip from remote .tmp folder */
            ProcessBuilder(
                *processBuilder(ConnectionTool.PLINK),
                connection,
                "rm $tmpFolder/$bulkZipName"
            )
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
                .waitFor()

            /* Unzip and return List of files */
            val returnList = mutableListOf<InMemoryFile>()
            val bulkZipFile = File(bulkZipName)

            if (bulkZipFile.exists()) {
                bulkZipFile.inputStream().use { fis ->
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
            }
            return returnList.toList()
        }
    }
}