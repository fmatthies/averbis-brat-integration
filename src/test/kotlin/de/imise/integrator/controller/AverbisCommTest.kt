package de.imise.integrator.controller

import org.junit.jupiter.api.Test
import java.io.File

class AverbisCommTest {
    private val testFile = AverbisController::class.java.getResource("/data/Schulz-Arztbriefe/Albers.txt").file
    private val testResponse = File(
        AverbisController::class.java.getResource("/data/Schulz-Arztbriefe/deid/Albers.json").file).readText()

    @Test
    internal fun testPostDocument() {
        val averbis_hd = AverbisController(
            "https://7db06374-f5b2-4579-af64-1391102a4852.mock.pstmn.io" +
                    "/rest/v1/textanalysis/projects/test-project/pipelines/deid/analyseText?language=de"
        )
        assert(averbis_hd.postDocuments(listOf(File(testFile))).toString() == testResponse)
    }
}