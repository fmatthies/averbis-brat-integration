package de.imise.integrator.view

import javafx.scene.control.TabPane
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import tornadofx.*

class MainView : View("Averbis & Brat Integrator") {
    private val averbis_url = "http://10.230.7.129:8445/health-discovery"

    lateinit var urlField: TextField
    lateinit var projectNameField: TextField
    lateinit var pipelineNameField: TextField
    lateinit var outputField: TextArea

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
                            projectNameField = textfield()
                        }
                        field("Pipeline Name") {
                            pipelineNameField = textfield()
                        }
                    }
                    fieldset("Output") {
                        outputField = textarea()
                    }

                    button("Post data") {
                        action { outputField.text = projectNameField.text }
                    }
                }
            }
            tab("Brat") {
                label("Brat")
            }
        }
    }
}