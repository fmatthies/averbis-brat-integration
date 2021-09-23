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
        private val tmpFolder = "/home/${mainView.usernameField}/.tmp"

        private fun temporaryFileStorage(fi: File) {
            // Create temporary folder
            ProcessBuilder(
                "plink.exe",
                "-P", mainView.remotePortField.text,
                "-pw", mainView.passwordField.text,
                "-no-antispoof",
                "${mainView.usernameField.text}@${mainView.hostField.text}",
                "mkdir --parents $tmpFolder"
            ).start()
            // Transfer Bulk.zip to tmp folder
            ProcessBuilder(
                "pscp.exe",
                "-P", mainView.remotePortField.text,
                "-pw", mainView.passwordField.text,
                fi.absolutePath,
                "${mainView.usernameField.text}@${mainView.hostField.text}:$tmpFolder"
            ).start()
            logging.logBrat("Transferred files to remote...")
        }

        private fun transferFileIndirectly(fi: File) {
            temporaryFileStorage(fi)

            val sudoPass = "sudoPass.txt"
            File(sudoPass)
                .bufferedWriter()
                .use { it.write("${mainView.passwordField.text}\n") }

            waitForFile(File(sudoPass))
            ProcessBuilder(
                "type ${Paths.get("").toAbsolutePath()}\\$sudoPass", "|",
                "plink.exe",
                "-P", mainView.remotePortField.text,
                "-pw", mainView.passwordField.text,
                "-no-antispoof",
                "${mainView.usernameField.text}@${mainView.hostField.text}",
                "sudo mkdir --parents ${mainView.destinationField.text}"
            ).start()
            ProcessBuilder(
                "type ${Paths.get("").toAbsolutePath()}\\$sudoPass", "|",
                "plink.exe",
                "-P", mainView.remotePortField.text,
                "-pw", mainView.passwordField.text,
                "-no-antispoof",
                "${mainView.usernameField.text}@${mainView.hostField.text}",
                "sudo unzip $tmpFolder/${fi.name} -d ${mainView.destinationField.text}"
            ).start()

            File(sudoPass).delete()
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
            val bulkZip = "bulk.zip"
            FileOutputStream(bulkZip).use {  fos ->
                ZipOutputStream(fos).use { zos ->
                    OutputTransformationController.transformToBrat(response).forEach { pair ->
                        pair.toList().forEach { entry ->
                            val zipEntry = ZipEntry("${entry.fileName}.${entry.extension}")
                            zos.putNextEntry(zipEntry)
                            zos.write(entry.content.toByteArray())
                            zos.closeEntry()
                        }
                    }
                }
            }
            waitForFile(File(bulkZip)) //ToDo:
            transferFileIndirectly(File(bulkZip))
            File(bulkZip).delete()
        }
    }
}