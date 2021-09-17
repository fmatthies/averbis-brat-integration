package de.imise.integrator.controller

import de.imise.integrator.view.MainView
import tornadofx.*

class LoggingController : Controller() {
    private val mainView: MainView by inject()

    fun log(message: String) {
        val logField = mainView.logField
        if (!logField.text.isNullOrBlank()) { logField.text += "\n" }
        logField.text += message
    }
}