package de.imise.integrator.controller

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import de.imise.integrator.view.MainView
import tornadofx.*
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class RemoteController : Controller() {
    private val mainView: MainView by inject()
    private val logging: LoggingController by inject()

    inner class FileTransfer {
        private fun transferFile(fi: File) {
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
            transferFile(File(bulkZip))
        }
    }
}