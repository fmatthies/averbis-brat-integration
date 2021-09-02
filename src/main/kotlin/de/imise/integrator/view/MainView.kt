package de.imise.integrator.view

import de.imise.integrator.controller.AverbisController
import javafx.scene.control.TabPane
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import tornadofx.*

class MainView : View("Averbis & Brat Integrator") {
    private val averbisController: AverbisController by inject()
    private val averbis_url = "http://10.230.7.129:8445/health-discovery"

    var urlField: TextField by singleAssign()
    var projectNameField: TextField by singleAssign()
    var pipelineNameField: TextField by singleAssign()
    var outputField: TextArea by singleAssign()

    val tabBinding = {tabPane: TabPane, parent: HBox ->
        val x = parent.widthProperty().doubleBinding(tabPane.tabs) {
            it as Double / tabPane.tabs.size - 25
        }
        x
    }

    override val root = hbox {
        prefHeight = 700.0
        prefWidth = 550.0
        tabpane {
            tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
            tabMinWidthProperty().bind(tabBinding(this, this@hbox))
            tabMaxWidthProperty().bind(tabBinding(this, this@hbox))

            tab("Averbis") {
                form {
                    fieldset("Setup") {
                        field("Health Discovery URL") {
                            urlField = textfield(averbis_url)
                        }
                        field("Project Name") {
                            projectNameField = textfield("test-project")
                        }
                        field("Pipeline Name") {
                            pipelineNameField = textfield("deid")
                        }
                    }
                    fieldset("Output") {
                        outputField = textarea()
                    }

                    button("Post data") {
                        action { outputField.text = averbisController.buildFinalUrl() }
                    }
                }
            }
            tab("Brat") {
                label("Brat")
            }
        }
    }
}