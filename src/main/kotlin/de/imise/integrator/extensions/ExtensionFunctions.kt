package de.imise.integrator.extensions

import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.Button
import tornadofx.*


fun Fieldset.withTableFrom(responseList: ObservableList<ResponseType>, fields: () -> Sequence<Field>): Fieldset {
    return this.apply {
        fields().forEach { this.add(it) }
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
        var btn: Button by singleAssign()
        borderpane {
            padding = insets(10)
            center {
                vbox {
                    alignment = Pos.CENTER
                    spacing = 10.0
                    btn = button(text) {
                        setPrefSize(200.0, 40.0)
                        action(action)
                    }
                }
            }
        }
    }
}

fun String.padAround(length: Int, padChar: Char): String {
    val middle: Int = kotlin.math.max(length - this.length, 0) / 2
    return this.padStart(length - middle, padChar).padEnd(length, padChar)
}