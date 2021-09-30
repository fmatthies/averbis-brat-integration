package de.imise.integrator.controller

import de.imise.integrator.view.MainView
import tornadofx.*
import java.io.File
import java.nio.file.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
            //ToDo:
            // mkdir `project-name dir` in `destination dir` (i.e. brat life-data)
            // for every file in `.tmp/project-name` (from unzip):
            //  - create (i.e. `touch`) an equally named file in `destination/project-name`
            //  - `cat` content from former files to latter
            // this way the newly created folder andfiles in `destination` have the group `bin` (used `setfacl`) and keep it
            // brat will be able to read/write there
            ProcessBuilder(
                *processBuilder(ConnectionTool.PLINK),
                connection,
                "mkdir --parents $finalDestination;",
                "find $tmpFolder/${mainView.pipelineNameField.text}/",
                "-maxdepth", "1", "-iname", "'*'", "-type", "f",
                "-exec", "touch", "$finalDestination/{}", ";",
                "-printf", "'cat %p > $finalDestination/%P\n'", "|",
                "sh"
            ) // ToDo: this does not work; maybe I need to write this into a script file and let it run with plink
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
                .log()
//            val sudoPass = "sudoPass.txt"
//            File(sudoPass)
//                .bufferedWriter()
//                .use { it.write("${mainView.passwordField.text}\n") }
//
//            waitForFile(File(sudoPass))
//            ProcessBuilder(
//                "type ${Paths.get("").toAbsolutePath()}\\$sudoPass", "|",
//                "plink.exe",
//                "-P", mainView.remotePortField.text,
//                "-pw", mainView.passwordField.text,
//                "-no-antispoof",
//                "${mainView.usernameField.text}@${mainView.hostField.text}",
//                "sudo mkdir --parents ${mainView.destinationField.text}"
//            ).start()
//            ProcessBuilder(
//                "type ${Paths.get("").toAbsolutePath()}\\$sudoPass", "|",
//                "plink.exe",
//                "-P", mainView.remotePortField.text,
//                "-pw", mainView.passwordField.text,
//                "-no-antispoof",
//                "${mainView.usernameField.text}@${mainView.hostField.text}",
//                "sudo unzip $tmpFolder/${fi.name} -d ${mainView.destinationField.text}"
//            ).start()
//
//            File(sudoPass).delete()
            logging.logBrat("Extracted files to brat folder...")
        }

        private fun transferFileDirectly(fi: File) {
            ProcessBuilder(
                *processBuilder(ConnectionTool.PSCP),
                fi.absolutePath,
                "$connection:${mainView.bratDataFolderField.text}")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
                .log()
        }

        fun transferData(response: List<AverbisResponse>) {
            val bulkZip = File("bulk.zip")
            bulkZip.outputStream().use {  fos ->
                ZipOutputStream(fos).use { zos ->
                    zos.putNextEntry(ZipEntry("${mainView.pipelineNameField.text}/"))
                    OutputTransformationController.transformToBrat(response).forEach { pair ->
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

        fun getDataFromRemote() : List<String> {
            return listOf()
        }
    }
}