package de.imise.integrator.extensions

import javafx.collections.ObservableList

interface ResponseType {
    val info1: String
    val info2: String
    val items: ObservableList<ResponseTypeEntry>
}

interface ResponseTypeEntry {
    val type: String
    val text: String
}
