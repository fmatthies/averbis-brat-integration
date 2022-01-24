package de.imise.integrator.extensions

import javafx.collections.ObservableList
import javafx.geometry.Pos
import tornadofx.*
import java.nio.charset.Charset
import java.util.logging.Logger
import kotlin.reflect.full.companionObject


fun Fieldset.withTableFrom(
    responseList: ObservableList<ResponseType>,
    fieldsToBottom: Boolean,
    fields: () -> Sequence<Field>): Fieldset
{
    return this.apply {
        if (!fieldsToBottom) {
            fields().forEach { this.add(it) }
        }
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
        if (fieldsToBottom) {
            fields().forEach { this.add(it) }
        }
    }
}

fun Field.withActionButton(text: String, status: TaskStatus, action: () -> Unit): Field {
    return this.apply {
        borderpane {
            padding = insets(10)
            center {
                vbox {
                    alignment = Pos.CENTER
                    spacing = 10.0
                    button(text) {
                        disableWhen { status.running }
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

fun String.substringBeforeRegex(delimiter: String): String {
    val match = Regex(delimiter).find(this)
    return match?.range?.start?.let { this.substring(0, it) } ?: this
}

fun String.splitAfterBytes(bytes: Int, charset: Charset) = sequence {
    var currentBytes = 0
    val lineList = mutableListOf<String>()
    this@splitAfterBytes.splitToSequence("\n").forEach {
        currentBytes += it.toByteArray(charset).size
        lineList.add(it)
        if (currentBytes >= bytes) {
            yield(lineList.joinToString("\n"))
            currentBytes = 0
            lineList.clear()
        }
    }
    yield(lineList.joinToString("\n"))
}

fun <T : Any> unwrapCompanionClass(ofClass: Class<T>): Class<*> {
    return ofClass.enclosingClass?.takeIf {
        ofClass.enclosingClass.kotlin.companionObject?.java == ofClass
    } ?: ofClass
}

fun <R : Any> R.logger(): Lazy<Logger> {
    return lazy { Logger.getLogger(unwrapCompanionClass(this.javaClass).name) }
}