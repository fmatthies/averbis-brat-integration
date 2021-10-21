package de.imise.integrator.view

import de.imise.integrator.controller.RemoteController
import tornadofx.*
import java.io.File
import java.nio.file.Paths

class TemporaryFileDeletionFragment: Fragment() {
    private val remoteController: RemoteController by inject()
    private val workingDir = Paths.get("").toAbsolutePath().toString()
    private val thisFragment = this

    override val root = borderpane {
//        prefHeight = 400.0
        prefWidth = 500.0

        center = form {
            fieldset {
                field {
                    textarea(
                        "Received a zip file '${remoteController.BRAT_BULKZIP_NAME}' in '$workingDir'.\n" +
                                "It contains json data and brat annotation data from the remote host and therefore possible sensitive information! " +
                                "You probably won't need the zip file any further since you are able to select the data you will use in a next step.\n" +
                                "Should the file be removed?"
                    )
                    {
                        isWrapText = true
                        isEditable = false
                    }
                }
                buttonbar {
                    button("Yes") {
                        action {
                            val zipFile = File(workingDir, remoteController.BRAT_BULKZIP_NAME)
                            if (zipFile.exists()) {
                                zipFile.delete()
                            }
                            thisFragment.close()
                        }
                    }
                    button("No") {
                        action {
                            thisFragment.close()
                        }
                    }
                }
            }
        }
    }
}