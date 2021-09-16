package de.imise.integrator.controller

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import de.imise.integrator.view.MainView
import tornadofx.*
import java.io.File

class RemoteController : Controller() {
    private val mainView: MainView by inject()
//    private val REMOTE_HOST = "10.230.7.129"
//    private val USERNAME = ""
//    private val PASSWORD = ""
//    private val REMOTE_PORT = 22
//    private val SESSION_TIMEOUT = 10000
//    private val CHANNEL_TIMEOUT = 5000
//    private val DESTINATION = ""

    inner class SFTPFileTransfer {
        val jsch: JSch = JSch()

        private fun openConnection(): ChannelSftp {
            val jschSession = jsch.getSession(
                mainView.usernameField.text,
                mainView.hostField.text,
                mainView.remotePortField.text.toInt()
            )
            jschSession.setPassword(mainView.passwordField.text)
            jschSession.connect() // jschSession.connect(SESSION_TIMEOUT)

            val channel = jschSession.openChannel("sftp")
            channel.connect() // channel.connect(CHANNEL_TIMEOUT)

            return channel as ChannelSftp
        }

        private fun transferFileToRemote(channelSftp: ChannelSftp, src: String, dest: String) {
            channelSftp.put(src, dest)
        }

        @JvmName("transferFilesToRemoteString")
        fun transferFilesToRemote(files: List<String>) {
            openConnection().run {
                files.forEach {
                    transferFileToRemote(this, it, mainView.destinationField.text)
                }
                this.disconnect()
            }
        }

        fun transferFilesToRemote(files: List<File>) {
            openConnection().run {
                files.forEach {
                    transferFileToRemote(this, it.absolutePath, mainView.destinationField.text)
                }
                this.disconnect()
            }
        }
    }
}