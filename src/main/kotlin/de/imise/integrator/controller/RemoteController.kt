package de.imise.integrator.controller

import de.imise.integrator.view.MainView
import tornadofx.*
import java.io.File
import java.io.FileOutputStream
import java.nio.file.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class RemoteController : Controller() {
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

        //ToDo: extract process builder templates for plink and pscp
        private fun temporaryFileStorage(fi: File) {
            // Create temporary folder
            ProcessBuilder(
                "plink.exe",
                "-P", mainView.remotePortField.text,
                "-pw", mainView.passwordField.text,
                "-no-antispoof",
                "${mainView.usernameField.text}@${mainView.hostField.text}",
                "mkdir --parents $tmpFolder")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start().inputStream.bufferedReader().use { logging.logBrat(it.readText()) }
            // Transfer Bulk.zip to tmp folder
            ProcessBuilder(
                "pscp.exe",
                "-P", mainView.remotePortField.text,
                "-pw", mainView.passwordField.text,
                fi.absolutePath,
                "${mainView.usernameField.text}@${mainView.hostField.text}:$tmpFolder")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start().inputStream.bufferedReader().use { logging.logBrat(it.readText()) }
            // Unzip Bulk.zip
            val unzipBulk = ProcessBuilder(
                "plink.exe",
                "-P", mainView.remotePortField.text,
                "-pw", mainView.passwordField.text,
                "-no-antispoof",
                "${mainView.usernameField.text}@${mainView.hostField.text}",
                "unzip $tmpFolder/${fi.name} -d $tmpFolder/")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
            logging.logBrat(unzipBulk.inputStream.bufferedReader().readText())
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
            val proc = ProcessBuilder(
                "pscp.exe",
                "-P", mainView.remotePortField.text,
                "-pw", mainView.passwordField.text,
                fi.absolutePath,
                "${mainView.usernameField.text}@${mainView.hostField.text}:${mainView.destinationField.text}")
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()
            val response = proc.inputStream.bufferedReader().readText()
            logging.logBrat(response)
        }

        fun transferData(response: List<AverbisResponse>) {
            val bulkZip = File("bulk.zip")
            bulkZip.outputStream().use {  fos ->
                ZipOutputStream(fos).use { zos ->
                    OutputTransformationController.transformToBrat(response).forEach { pair ->
                        zos.putNextEntry(ZipEntry("${mainView.pipelineNameField.text}/"))
                        pair.toList().forEach { entry ->
                            val zipEntry = ZipEntry("${mainView.pipelineNameField.text}/${entry.fileName}.${entry.extension}")
                            zos.putNextEntry(zipEntry)
                            zos.write(entry.content.toByteArray())
                            zos.closeEntry()
                        }
                    }
                }
            }
            waitForFile(bulkZip) //ToDo:
            transferFileIndirectly(bulkZip)
//            bulkZip.delete()
        }
    }
}