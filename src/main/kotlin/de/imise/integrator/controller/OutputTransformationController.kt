package de.imise.integrator.controller

import java.io.File
import java.time.Year


data class OutputFileStream(val fileName: String, val extension: String, val content: String)

class OutputTransformationController() {

    companion object {
        fun transformToBrat(response: List<AverbisResponse>): List<Pair<OutputFileStream, OutputFileStream>> {
            return response.map {
                Pair(
                    OutputFileStream(
                        fileName = it.inputFileName, extension = "ann",
                        content = it.jsonToBrat().replace("\\r\\n?", "\n")
                    ),
                    OutputFileStream(
                        fileName = it.inputFileName, extension = "txt",
                        content = it.documentText.replace("\\r\\n?", "\n")
                    )
                )
            }
        }

        fun getFilteredJson(response: List<AverbisResponse>): List<OutputFileStream> {
            return response.map {
                OutputFileStream(
                    fileName = it.inputFileName, extension = "json",
                    content = it.filteredJson().replace("\\r\\n?", "\n")
                )
            }
        }

        fun parseDate(dateString: String) {

        }
    }
}