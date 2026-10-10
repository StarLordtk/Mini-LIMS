package com.example.minilims.data

data class CsvTestRow(
    val code: String,
    val name: String,
    val rate: Double?,
    val labCharge: Double?,
    val sampleType: String,
    val reportDay: String,
    val unit: String,
    val refRange: String
)

/**
 * Reads a test/rate list from CSV. Recognised columns (any order, case-insensitive):
 * Test Name | Patient Charge / Rate | Lab Charge | Sample Type | Report Day | Code | Unit | Reference Range.
 * Without a header row the order is: name, rate, sample type, report day.
 */
object CsvImport {
    fun parse(raw: String): List<CsvTestRow> {
        val table = readCsv(raw.removePrefix("\uFEFF"))
        if (table.isEmpty()) return emptyList()

        fun norm(s: String) = s.lowercase().filter { it.isLetter() }
        val header = table[0].map { norm(it) }
        fun find(vararg keys: String) = header.indexOfFirst { it in keys }

        var iName = find("name", "testname", "test")
        var iRate = find("rate", "patientcharge", "charge", "price", "amount", "mrp")
        var iLab = find("labcharge", "labrate", "labcost", "cost")
        val hasHeader = iName >= 0 && (iRate >= 0 || iLab >= 0)
        var iSample = find("sampletype", "sample", "specimen")
        var iReport = find("reportday", "report", "tat", "turnaround")
        var iCode = find("code", "testcode")
        var iUnit = find("unit", "units")
        var iRef = find("refrange", "referencerange", "reference", "normalrange")
        if (!hasHeader) {
            iName = 0; iRate = 1; iSample = 2; iReport = 3; iCode = -1; iUnit = -1; iRef = -1; iLab = -1
        }

        fun cell(r: List<String>, i: Int) = if (i >= 0 && i < r.size) r[i].trim() else ""
        return table.drop(if (hasHeader) 1 else 0).map { r ->
            CsvTestRow(
                code = cell(r, iCode),
                name = cell(r, iName),
                rate = cell(r, iRate).replace(Regex("[^0-9.]"), "").toDoubleOrNull(),
                labCharge = cell(r, iLab).replace(Regex("[^0-9.]"), "").toDoubleOrNull(),
                sampleType = cell(r, iSample),
                reportDay = cell(r, iReport),
                unit = cell(r, iUnit),
                refRange = cell(r, iRef)
            )
        }
    }

    private fun readCsv(s: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < s.length && s[i + 1] == '"') { cur.append('"'); i++ } else inQuotes = false
                } else cur.append(c)
            } else if (c == '"') {
                inQuotes = true
            } else if (c == ',') {
                row.add(cur.toString()); cur.setLength(0)
            } else if (c == '\n') {
                row.add(cur.toString()); cur.setLength(0)
                rows.add(row); row = mutableListOf()
            } else if (c != '\r') {
                cur.append(c)
            }
            i++
        }
        if (cur.isNotEmpty() || row.isNotEmpty()) { row.add(cur.toString()); rows.add(row) }
        return rows.filter { r -> r.any { it.isNotBlank() } }
    }
}
