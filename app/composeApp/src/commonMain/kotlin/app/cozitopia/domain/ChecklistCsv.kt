package app.cozitopia.domain

data class CsvRow(
    val what: String,
    val where: String,
    val frequencyText: String,
    val durationMinutes: Int,
    val assigneeNames: List<String>,
)

object ChecklistCsv {
    const val TEMPLATE = "What,Where,Frequency,Duration,Assignee\n" +
        "Steckdosen reinigen,Alle Räume,1x Jahr,3h,\n" +
        "Terrasse reinigen,Garten,1x Jahr (Herbst),2h,Everyone\n"

    private val EVERYONE = setOf("everyone", "alle", "tous", "tout le monde")
    private val UNASSIGNED = setOf("unassigned", "nicht zugewiesen", "non assigné", "")
    private val DURATION = Regex("""^(\d+(?:[.,]\d+)?)\s*(h|std|min|m)?$""", RegexOption.IGNORE_CASE)

    fun parse(text: String): List<CsvRow> {
        val lines = text.removePrefix("﻿").lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val delimiter = detectDelimiter(lines.first())
        val records = lines.map { splitLine(it, delimiter) }
        val body = if (isHeader(records.first())) records.drop(1) else records
        return body.mapNotNull(::toRow)
    }

    fun resolveAssignee(names: List<String>, homies: List<Homie>): Assignee {
        val normalized = names.map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        if (normalized.isEmpty() || normalized.all { it in UNASSIGNED }) return Assignee.Unassigned
        if (normalized.any { it in EVERYONE }) return Assignee.Everyone
        val ids = homies.filter { it.name.trim().lowercase() in normalized }.map { it.userId }.toSet()
        return if (ids.isEmpty()) Assignee.Unassigned else Assignee(AssigneeKind.PEOPLE, ids)
    }

    fun parseDurationMinutes(text: String): Int {
        val match = DURATION.matchEntire(text.trim()) ?: return 0
        val value = match.groupValues[1].replace(',', '.').toDouble()
        val isMinutes = match.groupValues[2].lowercase() in setOf("min", "m")
        return (if (isMinutes) value else value * 60).toInt()
    }

    private fun toRow(fields: List<String>): CsvRow? {
        if (fields.size < 4 || fields[0].isBlank()) return null
        return CsvRow(
            what = fields[0],
            where = fields[1],
            frequencyText = fields[2],
            durationMinutes = parseDurationMinutes(fields[3]),
            assigneeNames = fields.getOrElse(4) { "" }.split(';', '&').map { it.trim() },
        )
    }

    private fun isHeader(fields: List<String>): Boolean =
        fields.getOrNull(2)?.lowercase() in setOf("frequency", "how often", "häufigkeit", "fréquence")

    private fun detectDelimiter(line: String): Char =
        listOf(',', ';', '\t').maxBy { delimiter -> line.count { it == delimiter } }

    private fun splitLine(line: String, delimiter: Char): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        for (char in line) {
            when {
                quote != null && char == quote -> quote = null
                quote == null && (char == '"' || char == '\'') && current.isBlank() -> quote = char
                quote == null && char == delimiter -> {
                    fields += current.toString().trim()
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        fields += current.toString().trim()
        return fields
    }
}
