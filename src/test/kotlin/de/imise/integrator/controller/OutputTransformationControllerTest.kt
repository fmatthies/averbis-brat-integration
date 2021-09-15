package de.imise.integrator.controller

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

import java.io.File

internal class OutputTransformationControllerTest {

//    companion object {
//        private val testFile = AverbisController::class.java.getResource("/Schulz-Arztbriefe/deid/Albers.json").file
//        private val testJsonStream = File(testFile).inputStream()
//        private var testJsonArray: JsonArray<JsonObject>? = null
//
//        private val transformationController = OutputTransformationController.Builder().build()
//
//        @BeforeAll
//        @JvmStatic
//        internal fun testReadJson() {
//            testJsonArray = transformationController.readJson(testJsonStream)
//        }
//    }
//
//    @Test
//    fun testQueryArrayByValues() {
//        testJsonArray?.let {
//            transformationController.queryArrayBy(it, "type", listOf("de.averbis.types.health.Date", "de.averbis.types.health.Name"))
//        }?.forEach { println(it.string("coveredText")) }
//    }
}