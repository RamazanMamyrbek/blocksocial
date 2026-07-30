package com.blocksocial.core.ui.text

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

val LocalUse24HourClock = compositionLocalOf<Boolean?> { null }

@Composable
fun rememberIs24Hour(): Boolean {
    val context = LocalContext.current
    val pinned = LocalUse24HourClock.current
    return pinned ?: remember(context) { DateFormat.is24HourFormat(context) }
}

@Composable
fun rememberClockFormatter(): (LocalTime) -> String {
    val locale = LocalLocale.current.platformLocale
    val is24Hour = rememberIs24Hour()
    val formatter = remember(locale, is24Hour) { clockFormatter(locale, is24Hour) }
    return { time -> formatter.format(time) }
}

@Composable
fun rememberEventFormatter(zone: ZoneId): (Instant) -> String {
    val locale = LocalLocale.current.platformLocale
    val is24Hour = rememberIs24Hour()
    val formatter = remember(locale, is24Hour, zone) {
        eventFormatter(locale, is24Hour).withZone(zone)
    }
    return { instant -> formatter.format(instant) }
}

@Composable
fun rememberWeekOrder(): List<DayOfWeek> {
    val locale = LocalLocale.current.platformLocale
    return remember(locale) { weekOrder(locale) }
}

internal fun clockFormatter(locale: Locale, is24Hour: Boolean): DateTimeFormatter =
    DateTimeFormatter.ofPattern(
        DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hm"),
        locale,
    )

internal fun eventFormatter(locale: Locale, is24Hour: Boolean): DateTimeFormatter =
    DateTimeFormatter.ofPattern(
        DateFormat.getBestDateTimePattern(locale, if (is24Hour) "dMMMHm" else "dMMMhm"),
        locale,
    )

internal fun weekOrder(locale: Locale): List<DayOfWeek> {
    val first = WeekFields.of(locale).firstDayOfWeek
    return (0 until DayOfWeek.entries.size).map { first.plus(it.toLong()) }
}
