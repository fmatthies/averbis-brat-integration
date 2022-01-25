package de.imise.integrator.view

import de.imise.integrator.controller.BratController.Companion.crossOutAnnotations
import de.imise.integrator.controller.BratController.Companion.replaceAnnotations
import de.imise.integrator.controller.BratResponse
import de.imise.integrator.controller.FileHandlingController
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.geometry.Pos
import javafx.scene.control.CheckBox
import javafx.scene.control.TitledPane
import javafx.scene.layout.VBox
import tornadofx.*

class MergeFragment : Fragment() {
    private val mainView: MainView by inject()
    private val fileHandlingController: FileHandlingController by inject()

    private var removeAnnotations: CheckBox by singleAssign()
    private var createTxt: CheckBox by singleAssign()

    private val crossOutList = crossOutAnnotations.copyOf().toMutableList().asObservable()
    private val replaceList = replaceAnnotations.copyOf().toMutableList().asObservable()

    val additionListener = {reference: ObservableList<String>, referenceUIContainer: VBox ->
        ListChangeListener<String> {
            while (it.next()) {
                if (it.wasAdded()) {
                    val name = it.addedSubList.first()
                    if (reference.contains(name)) {
                        reference.remove(name)
                        val cb = referenceUIContainer.children.first { cb -> (cb as CheckBox).text == name } as CheckBox
                        cb.isSelected = false
                    }
                }
            }
        }
    }

    override val root = borderpane {
        prefHeight = 400.0
        prefWidth = 500.0

        center = form {
            fieldset("Anonymization") {
                field("Remove Crossed Out Annotations from Json") {
                    removeAnnotations = checkbox {
                        isSelected = true
                    }
                }
                field("Create Txt Files as well") {
                    createTxt = checkbox {
                        isSelected = true
                    }
                }
                separator { paddingAll = 5.0 }
                squeezebox {
                    fold("Cross Out") {
                        mainView.averbisAnalysisModel.annotationValues.value["deid"]?.let { this.addCheckBoxesByList(it.toList(), crossOutList) }
                        replaceList.addListener(additionListener(crossOutList, this.content as VBox))
                    }
                    fold("Modify/Replace") {
                        mainView.averbisAnalysisModel.annotationValues.value["deid"]?.let { this.addCheckBoxesByList(it.toList(), replaceList) }
                        crossOutList.addListener(additionListener(replaceList, this.content as VBox))
                    }
                }
            }
        }
        bottom = vbox {
            alignment = Pos.CENTER
            spacing = 10.0
            paddingAll = 10.0
            button("Merge!") {
                setPrefSize(200.0, 40.0)
                action {
                    val dir = chooseDirectory("Choose Folder") ?: return@action
                    fileHandlingController.writeMergedData(
                        dir,
                        mainView.bratResponseList.toList() as List<BratResponse>,
                        crossOutList,
                        replaceList,
                        removeAnnotations.isSelected,
                        createTxt.isSelected
                    )
                    mainView.writeDateLog()
                    (this@MergeFragment).close()
                }
            }
        }
    }
}

fun TitledPane.addCheckBoxesByList(completeList: List<String>, checkedElements: MutableList<String>) {
    completeList.forEach {
        checkbox(it).apply {
            this.isSelected = checkedElements.contains(it)
            action {
                if (isSelected) {
                    checkedElements.add(this.text)
                } else {
                    checkedElements.remove(this.text)
                }
            }
        }
    }
}