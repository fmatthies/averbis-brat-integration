package de.imise.integrator.extensions

import javafx.collections.ObservableList
import tornadofx.*


fun Fieldset.withTableFrom(responseList: ObservableList<ResponseType>): Fieldset {
    return this.apply {
        tableview(responseList) {
            prefHeight = 1000.0
            isEditable = false
            columnResizePolicy = SmartResize.POLICY
            readonlyColumn("File Name", ResponseType::basename)
            readonlyColumn("File Path", ResponseType::pathname)

            rowExpander(expandOnDoubleClick = true) {
                paddingLeft = expanderColumn.width

                tableview(it.items) {
                    columnResizePolicy = SmartResize.POLICY

                    readonlyColumn(
                        "Type",
                        ResponseTypeEntry::type
                    ).contentWidth(padding = 50.0)
                    readonlyColumn("Text", ResponseTypeEntry::text)
                }
            }
        }
    }
}