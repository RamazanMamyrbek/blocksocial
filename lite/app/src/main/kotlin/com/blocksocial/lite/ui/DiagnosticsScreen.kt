package com.blocksocial.lite.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.blocksocial.lite.R
import com.blocksocial.lite.detection.DecisionRecord
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DiagnosticsTags {
    const val SERVICE = "diagnostics-service"
    const val MEASURED = "diagnostics-measured"
    const val DECISIONS = "diagnostics-decisions"
}

data class DiagnosticsState(
    val serviceState: ServiceState,
    val connectedAtMillis: Long?,
    val lastEventAtMillis: Long?,
    val overlayFailure: String?,
    val usageAccessGranted: Boolean,
    val measuredMinutes: List<Pair<String, Int>>,
    val decisions: List<DecisionRecord>,
)

@Composable
fun DiagnosticsScreen(state: DiagnosticsState) {
    val clock = rememberClock()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = stringResource(R.string.diagnostics_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = stringResource(R.string.diagnostics_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SoftCard(modifier = Modifier.testTag(DiagnosticsTags.SERVICE)) {
            Line(stringResource(R.string.diagnostics_service), state.serviceState.name)
            Line(stringResource(R.string.diagnostics_connected_at), clock(state.connectedAtMillis))
            Line(stringResource(R.string.diagnostics_last_event), clock(state.lastEventAtMillis))
            Line(
                stringResource(R.string.diagnostics_usage_access),
                state.usageAccessGranted.toString(),
            )
            if (state.overlayFailure != null) {
                Line(stringResource(R.string.diagnostics_overlay_failure), state.overlayFailure)
            }
        }

        SectionHeader(stringResource(R.string.diagnostics_measured))
        SoftCard(modifier = Modifier.testTag(DiagnosticsTags.MEASURED)) {
            if (state.measuredMinutes.isEmpty()) {
                Text(
                    text = stringResource(R.string.diagnostics_nothing_measured),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.measuredMinutes.forEach { (app, minutes) -> Line(app, "$minutes") }
        }

        SectionHeader(stringResource(R.string.diagnostics_decisions))
        SoftCard(modifier = Modifier.testTag(DiagnosticsTags.DECISIONS)) {
            if (state.decisions.isEmpty()) {
                Text(
                    text = stringResource(R.string.diagnostics_no_decisions),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.decisions.forEach { record ->
                Text(
                    text = lineFor(record, clock),
                    style = Numeric,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun lineFor(record: DecisionRecord, clock: (Long?) -> String): String = buildString {
    append(clock(record.atMillis))
    append(' ')
    append(record.transition.name.removePrefix("IGNORED_").lowercase())
    if (record.app != null) {
        append(' ')
        append(record.app)
    }
    if (record.limitMinutes != null) {
        append(' ')
        append(record.usedMinutes ?: '-')
        append('/')
        append(record.limitMinutes)
    }
    if (record.warned) append(" WARNED")
}

@Composable
private fun Line(label: String, value: String) {
    Text(
        text = "$label: $value",
        style = Numeric,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun rememberClock(): (Long?) -> String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
    return { millis -> millis?.let { formatter.format(Instant.ofEpochMilli(it)) } ?: "—" }
}
