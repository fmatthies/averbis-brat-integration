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
//    private val REMOTE_HOST = "10.230.7.129"
//    private val USERNAME = ""
//    private val PASSWORD = ""
//    private val REMOTE_PORT = 22
//    private val SESSION_TIMEOUT = 10000
//    private val CHANNEL_TIMEOUT = 5000
//    private val DESTINATION = ""

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
//    inner class SFTPFileTransfer {
//        val jsch: JSch = JSch()
//
//        private fun openConnection(): ChannelSftp {
//            val jschSession = jsch.getSession(
//                mainView.usernameField.text,
//                "${mainView.usernameField.text}@${mainView.hostField.text}",
//                mainView.remotePortField.text.toInt()
//            )
//            jschSession.setPassword(mainView.passwordField.text)
//            jschSession.setConfig("StrictHostKeyChecking", "no")
//            jschSession.connect() // jschSession.connect(SESSION_TIMEOUT)
//
//            val channel = jschSession.openChannel("sftp")
//            channel.connect() // channel.connect(CHANNEL_TIMEOUT)
//
//            return channel as ChannelSftp
//        }
//
//        private fun transferFileToRemote(channelSftp: ChannelSftp, src: String, dest: String) {
//            channelSftp.put(src, dest)
//        }
//
//        @JvmName("transferFilesToRemoteString")
//        fun transferFilesToRemote(files: List<String>) {
//            openConnection().run {
//                files.forEach {
//                    transferFileToRemote(this, it, mainView.destinationField.text)
//                }
//                this.disconnect()
//            }
//        }
//
//        fun transferFilesToRemote(files: List<File>) {
//            openConnection().run {
//                files.forEach {
//                    transferFileToRemote(this, it.absolutePath, mainView.destinationField.text)
//                }
//                this.disconnect()
//            }
//        }
//    }
}