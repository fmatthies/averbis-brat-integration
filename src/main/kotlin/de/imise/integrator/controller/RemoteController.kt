package de.imise.integrator.controller

import de.imise.integrator.extensions.ResponseType
import de.imise.integrator.view.MainView
import tornadofx.*
import java.io.File
import java.nio.file.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.system.exitProcess

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
        private val finalDestination = "${mainView.bratDataFolderField.text}/" +
                (mainView.bratTransferSubfolderField.text.takeIf { !it.isNullOrBlank() } ?: mainView.pipelineNameField.text)
        private fun Process.log() = run { this.inputStream.bufferedReader().use { logging.logBrat(it.readText()) } }

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
                "mkdir --parents $tmpFolder")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
                .log()

            // Transfer Bulk.zip to tmp folder
            ProcessBuilder(
                *processBuilder(ConnectionTool.PSCP),
                fi.absolutePath,
                "$connection:$tmpFolder")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
                .log()

            // Unzip Bulk.zip
            ProcessBuilder(
                *processBuilder(ConnectionTool.PLINK),
                connection,
                "unzip $tmpFolder/${fi.name} -d $tmpFolder/")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
                .log()

            logging.logBrat("Transferred files to remote...")
        }

        private fun transferFileIndirectly(fi: File) {
            temporaryFileStorage(fi)
            val dollar = "$"
            val commandFile = File("command.sh")
            commandFile.outputStream().bufferedWriter().use {
                it.write(
                    """
                        mkdir --parents $finalDestination
                        find $tmpFolder -maxdepth 1 -iname '*' -type 'f' -execdir touch $finalDestination{} \;
                        for ext in txt ann json
                        do
                            for file in $tmpFolder/*.${dollar}ext
                            do
                                cat "${dollar}file" > $finalDestination"${dollar}( basename ${dollar}file )"
                            done
                        done
                    """.trimIndent()
                )
            }
            ProcessBuilder(
                *processBuilder(ConnectionTool.PLINK),
                connection, "-m", commandFile.absolutePath
            )
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
                .log()
            logging.logBrat("Extracted files to brat folder...")
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
            transferFileIndirectly(bulkZip)
//            bulkZip.delete()
        }

        fun getDataFromRemote() : List<File> {
            return listOf()
        }
    }
}