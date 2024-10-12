package dev.nohus.rift.utils

import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val compactFormatter = NumberFormat.getCompactNumberInstance().apply {
    minimumFractionDigits = 1
}
private val formatter = NumberFormat.getInstance()
private val dateFormatter = DateTimeFormatter
    .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    .withLocale(Locale.ENGLISH)
private val dateFormatterWithDateTime = DateTimeFormatter.ofPattern("MMM dd, HH:mm:ss")
private val dateFormatterWithTime = DateTimeFormatter.ofPattern("HH:mm:ss")

fun formatIsk(number: Double): String = "${formatNumberCompact(number)} ISK"

fun formatNumberCompact(number: Int): String = compactFormatter.format(number)
fun formatNumberCompact(number: Long): String = compactFormatter.format(number)
fun formatNumberCompact(number: Double): String = compactFormatter.format(number)
fun formatNumber(number: Int): String = formatter.format(number)
fun formatNumber(number: Long): String = formatter.format(number)

fun formatDurationNumeric(duration: Duration): String {
    return String.format("%02d:%02d:%02d", duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart())
}
fun formatDurationCompact(duration: Duration): String {
    return buildString {
        if (duration.toDays() >= 1) append("${duration.toDays()}d ")
        if (duration.toHoursPart() >= 1) append("${duration.toHoursPart()}h ")
        if (duration.toMinutesPart() >= 1) append("${duration.toMinutesPart()}m")
        if (duration.toMinutes() == 0L) append("${duration.toSeconds()}s")
    }.trimEnd()
}
fun formatDurationLong(duration: Duration): String {
    return buildList {
        if (duration.toDays() >= 1) add("${duration.toDays()} day${duration.toDays().plural}")
        if (duration.toHoursPart() >= 1) add("${duration.toHoursPart()} hour${duration.toHoursPart().plural}")
        if (duration.toMinutesPart() >= 1) add("${duration.toMinutesPart()} minute${duration.toMinutesPart().plural}")
    }.take(2).joinToString(" and ")
}

fun formatDateTime(instant: Instant): String {
    val zoneId = ZoneId.systemDefault()
    val time: ZonedDateTime = ZonedDateTime.ofInstant(instant, zoneId)
    val previousMidnight = ZonedDateTime.of(LocalDate.now().atTime(0, 0), zoneId)
    val nextMidnight = previousMidnight.plusDays(1)
    val isToday = time.isAfter(previousMidnight) && time.isBefore(nextMidnight)
    val formatter = if (isToday) dateFormatterWithTime else dateFormatterWithDateTime
    return formatter.format(ZonedDateTime.ofInstant(instant, zoneId))
}

val Int.plural: String get() {
    return if (this != 1) "s" else ""
}

val Int.invertedPlural: String get() {
    return if (this == 1) "s" else ""
}

val Long.plural: String get() {
    return if (this != 1L) "s" else ""
}
