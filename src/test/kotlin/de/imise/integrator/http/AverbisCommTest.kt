package de.imise.integrator.http

import org.junit.jupiter.api.Test
import java.io.File

class AverbisCommTest {
    private val testFile = AverbisComm::class.java.getResource("/Schulz-Arztbriefe/Albers.txt").file
    private val testResponse = File(
        AverbisComm::class.java.getResource("/Schulz-Arztbriefe/deid/Albers.json").file).readText()

    @Test
    internal fun testPostDocument() {
        val averbis_hd = AverbisComm("https://7db06374-f5b2-4579-af64-1391102a4852.mock.pstmn.io")
        assert(averbis_hd.postDocument(testFile) == testResponse)
    }
}