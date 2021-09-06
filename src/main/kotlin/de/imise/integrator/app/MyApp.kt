package de.imise.integrator.app

import de.imise.integrator.view.MainView
import tornadofx.App
import java.nio.file.Path
import java.nio.file.Paths

class MyApp: App(MainView::class, Styles::class) {
}