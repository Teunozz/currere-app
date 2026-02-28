package nl.teunk.currere.ui

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormatters {
    /** e.g. "Friday 28 February 2026" */
    val dateFull: DateTimeFormatter = DateTimeFormatter
        .ofPattern("EEEE d MMMM yyyy", Locale.US)
        .withZone(ZoneId.systemDefault())

    /** e.g. "14:30" */
    val timeShort: DateTimeFormatter = DateTimeFormatter
        .ofPattern("HH:mm", Locale.US)
        .withZone(ZoneId.systemDefault())

    /** e.g. "Fri 28 Feb 2026, 14:30" */
    val dateTimeCompact: DateTimeFormatter = DateTimeFormatter
        .ofPattern("EEE d MMM yyyy, HH:mm", Locale.US)
        .withZone(ZoneId.systemDefault())
}
