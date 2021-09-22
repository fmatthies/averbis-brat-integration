package de.imise.integrator.controller

import de.imise.integrator.view.MainView
import tornadofx.*

class LoggingController : Controller() {
    private val mainView: MainView by inject()

    fun logAverbis(message: String, clear: Boolean = false) {
        val logField = mainView.logFieldAverbis
        if (clear) {
            logField.text = ""
        }
        if (!logField.text.isNullOrBlank()) {
            logField.text += "\n"
        }
        logField.text += message
    }

    fun logBrat(message: String, clear: Boolean = false) {
        val logField = mainView.logFieldBrat
        if (clear) {
            logField.text = ""
        }
        if (!logField.text.isNullOrBlank()) {
            logField.text += "\n"
        }
        logField.text += message
    }
}