package de.imise.integrator.extensions

import tornadofx.*
import java.time.LocalDate

data class CustomDateMatch(val day: Int?, val month: Int?, val year: Int?)

class DateFunctionality(dateString: String) {

    private val monthNamesMap: Map<String, Int> = mapOf("januar" to 1, "jan" to 1, "februar" to 2, "feb" to 2, "märz" to 3,
        "mär" to 3, "april" to 4, "apr" to 4, "mai" to 5, "juni" to 6, "jun" to 6, "juli" to 7, "jul" to 7,
        "august" to 8, "aug" to 8, "september" to 9, "sep" to 9, "oktober" to 10, "okt" to 10, "november" to 11,
        "nov" to 11, "dezember" to 12, "dez" to 12)
    private var date: CustomDateMatch?

    init {
        date = if ( /* Year only */
            dateString.trim().length <= 4 &&
            arrayOf("19", "20").contains(dateString.trim().subSequence(0, 2))
        ) {
            CustomDateMatch(day = null, month = null, year = dateString.trim().toInt())
        } else if ( /* Month name only */
            monthNamesMap.containsKey(dateString.lowercase().trim().trimEnd('.', ' '))
        ) {
            val month = monthNamesMap[dateString.lowercase().trim().trimEnd('.', ' ')]
            CustomDateMatch(day = null, month = month, year = null)
        } else if ( /* Day and Month only and with '.' delimiter; year optional */
            (2..3).contains(dateString.split(Regex(" *\\. *")).size)
        ) {
            var year: Int? = null
            val dateList = dateString.split(Regex(" *\\. *"))
            val month = if (dateList[1].trim().length <= 2 && dateList[1].trim().isInt()) {
                dateList[1].trim().toInt()
            } else if (dateList[1].trim().length > 2) {
                val (m, y) = dateList[1].trim().split(" ", limit = 2)
                year = y.takeIf { y.trim().isInt() }?.trim()?.toInt()
                m.takeIf { m.trim().isInt() }?.trim()?.toInt()
            } else {
                monthNamesMap[dateList[1].trim().lowercase()]
            }
            year = if (year == null && dateList.size == 3 && dateList[2].isInt()) {
                dateList[2].toInt()
            } else { null }

            val day: Int? = if(dateList.first().isBlank()) { null } else { dateList.first().toInt() }

            CustomDateMatch(day = day, month = month, year = year)
        } else { /* No CustomDateMatch pattern detectable */
            null
        }

        if (date == null)  parseDateByRegex(dateString)
    }

    private fun parseDateByRegex(dateString: String) {
//        LocalDate.parse(dateString)
    }

    fun getDate(): String {
        val dateAsString = if (date == null) {
            "<YEAR>"
        } else {
            "${date!!.year?: "<YEAR>"}"
        }
        return dateAsString
    }
}