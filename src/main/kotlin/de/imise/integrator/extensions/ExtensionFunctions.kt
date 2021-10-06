package de.imise.integrator.extensions

import javafx.collections.ObservableList
import javafx.geometry.Pos
import tornadofx.*


fun Fieldset.withTableFrom(responseList: ObservableList<ResponseType>): Fieldset {
    return this.apply {
        tableview(responseList) {
            prefHeight = 1000.0
            isEditable = false
            columnResizePolicy = SmartResize.POLICY
            readonlyColumn("File Name", ResponseType::basename)
            readonlyColumn("File Path", ResponseType::additionalColumn)

            rowExpander(expandOnDoubleClick = true) {
                paddingLeft = expanderColumn.width

                tableview(it.items) {
                    columnResizePolicy = SmartResize.POLICY

                    readonlyColumn("Type", ResponseTypeEntry::type).contentWidth(padding = 50.0)
                    readonlyColumn("Text", ResponseTypeEntry::text)
                }
            }
        }
    }
}

fun Field.withActionButton(text: String, action: () -> Unit): Field {
    return this.apply {
        borderpane {
            padding = insets(10)
            center {
                vbox {
                    alignment = Pos.CENTER
                    spacing = 10.0
                    button(text) {
                        setPrefSize(200.0, 40.0)
                        action(action)
                    }
                }
            }
        }
    }
}