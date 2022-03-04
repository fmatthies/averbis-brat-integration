package de.imise.integrator.extensions

import tornadofx.*


data class CustomDateMatch(val day: Int?, val month: Int?, val year: Int?)

class DateFunctionality(private val dateString: String, private val basename: String) {

    private val monthNamesMap: Map<String, Int> = mapOf("januar" to 1, "jan" to 1, "februar" to 2, "feb" to 2, "märz" to 3,
        "mär" to 3, "april" to 4, "apr" to 4, "mai" to 5, "juni" to 6, "jun" to 6, "juli" to 7, "jul" to 7,
        "august" to 8, "aug" to 8, "september" to 9, "sep" to 9, "oktober" to 10, "okt" to 10, "november" to 11,
        "nov" to 11, "dezember" to 12, "dez" to 12)
    private var date: CustomDateMatch?
    private val apostroph = "'`´"

    init {
        try {
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
            } else if ( /* Month name and Year */
                Regex("([a-zA-Z]+)\\s+(\\d+)").find(dateString) != null
            ) {
                val (m,y) = Regex("([a-zA-Z]+)\\s+(\\d+)").find(dateString)!!.destructured
                CustomDateMatch(day = null, month = monthNamesMap[m.lowercase().trim()], year = y.toInt())
            } else if ( /* Month Number and Year; day optional*/
                Regex("^(\\d{1,2} *[./,]? *)??(\\d{1,2} *[./,]?)?? *(['`´]?\\d{2,4})??$").find(dateString) != null
            ) {
                val regexMatch = Regex("^(\\d{1,2} *[./,]? *)??(\\d{1,2} *[./,]?)?? *(['`´]?\\d{2,4})??$").find(dateString)!!.groupValues
                var (_,d,m,y) = regexMatch
                if (y.isNotEmpty() && m.isEmpty() && d.isNotEmpty()) {
                    m = d
                    d = ""
                }
                CustomDateMatch(
                    day = d.filterNot { ".".indexOf(it) > -1 }.takeIf { it.isInt() }?.toInt(),
                    month = m.filterNot { ".".indexOf(it) > -1 }.takeIf { it.isInt() }?.toInt(),
                    year = y.filterNot { apostroph.indexOf(it) > -1 }.takeIf { it.isInt() }?.toInt())
            } else if ( /* Day and Month only and with '. / , space' delimiters; year optional */
                (2..3).contains(dateString.split(Regex(" *[./, ] *")).size)
            ) {
                var year: Int? = null
                val dateList = dateString.split(Regex(" *[./, ] *"))
                val month = if (dateList[1].trim().length <= 2 &&
                                dateList[1].trim().isInt() &&
                                dateList[1].trim().toInt() <= 12 ) {
                    dateList[1].trim().toInt()
                } else if (dateList[1].trim().length > 2) {
                    if (!dateList[1].any {apostroph.contains(it)} && !dateList[1].trim().isInt()) {
                        monthNamesMap[dateList[1].trim().lowercase()]
                    } else {
                        val (m, y) = dateList[1].trim().split(" ", limit = 2)
                            .takeIf { it.size >= 2 } ?: kotlin.run {
                                when (dateList[1].length) {
                                    3 -> listOf(dateList[1].substring(0, 1), dateList[1].substring(1, 3))
                                    4 -> listOf(dateList[1].substring(0, 2), dateList[1].substring(2, 4))
                                    5 -> listOf(dateList[1].substring(0, 1), dateList[1].substring(1, 5))
                                    6 -> listOf(dateList[1].substring(0, 2), dateList[1].substring(2, 6))
                                    else -> listOf("null", "null")
                                }
                        }
                        year = y.takeIf { y.trim().isInt() }?.trim()?.toInt()
                        m.takeIf { m.trim().isInt() }?.trim()?.toInt()
                    }
                } else {
                    monthNamesMap[dateList[1].trim().lowercase()]
                }
                year = if (year == null && dateList.size == 3 && dateList[2].filterNot { apostroph.indexOf(it) > -1 }.isInt()) {
                    dateList[2].filterNot { apostroph.indexOf(it) > -1 }.toInt()
                } else {
                    null
                }

                val day: Int? = if (dateList.first().isBlank()) {
                    null
                } else {
                    if ( dateList.first().isInt() ) {
                        dateList.first().toInt()
                    } else {
                        null
                    }
                }
                CustomDateMatch(day = day, month = month, year = year)
            } else { /* No CustomDateMatch pattern detectable */
                null
            }

            if (date == null) parseDateByRegex(dateString)
        } catch (e: Exception) {
            date = null
        }
    }

    private fun parseDateByRegex(dateString: String) {
//        LocalDate.parse(dateString)
    }

    fun getDate(logMap: MutableMap<String, MutableList<String>>): String {
        val dateAsString = if (date == null) {
            "<YEAR>"
        } else {
            var finalDate = "${date!!.year?: "<YEAR>"}"
            if (date!!.year == null && date!!.month != null) {
                finalDate = "<MONTH>"
            }
            finalDate
        }
        if (listOf("<MONTH>", "<YEAR>").contains(dateAsString)) {
            val mesPart1 = "($basename) Problems with DateString: "
            val mesPart2 = "'$dateString'"
            val mesPart3 = "--> replaced with ${if (dateString.length >= dateAsString.length) "'$dateAsString'" else "'${dateAsString.substring(0, 2)}>'"}"
            LOG.warning("$mesPart1$mesPart2$\n$mesPart3")
            if (!logMap.contains(basename)) {
                logMap[basename] = mutableListOf()
            }
            logMap.getValue(basename).add("$mesPart2 $mesPart3")
        }
        return dateAsString
    }

    companion object {
        val LOG by logger()
    }
}