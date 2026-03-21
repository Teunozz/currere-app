package nl.teunk.currere.ui

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormatters {
    /** e.g. "Friday 28 February 2026" */
    val dateFull: DateTimeFormatter
        get() = DateTimeFormatter
            .ofPattern("EEEE d MMMM yyyy", Locale.getDefault())
            .withZone(ZoneId.systemDefault())

    /** e.g. "14:30" */
    val timeShort: DateTimeFormatter
        get() = DateTimeFormatter
            .ofPattern("HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())

    /** e.g. "Fri 28 Feb 2026, 14:30" */
    val dateTimeCompact: DateTimeFormatter
        get() = DateTimeFormatter
            .ofPattern("EEE d MMM yyyy, HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())

    /** e.g. "28 Feb, 14:30" */
    val dateShort: DateTimeFormatter
        get() = DateTimeFormatter
            .ofPattern("d MMM, HH:mm", Locale.getDefault())
            .withZone(ZoneId.systemDefault())
}
