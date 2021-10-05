package de.imise.integrator.extensions

import javafx.collections.ObservableList

interface ResponseType {
    val basename: String
    val pathname: String
    val items: ObservableList<ResponseTypeEntry>
}

interface ResponseTypeEntry {
    val type: String
    val text: String
}
