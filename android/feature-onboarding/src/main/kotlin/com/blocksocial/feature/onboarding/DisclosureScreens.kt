package com.blocksocial.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.blocksocial.core.ui.theme.Numeric
import com.blocksocial.core.ui.theme.Spacing

object DisclosureTags {
    const val TITLE = "disclosure-title"
    const val CONTINUE = "disclosure-continue"
    const val NOT_NOW = "disclosure-not-now"
    const val FOOTNOTE = "disclosure-footnote"
    const val CONSENT_TITLE = "consent-title"
    const val AGREE = "consent-agree"
    const val CANCEL = "consent-cancel"
}

@Composable
fun ProminentDisclosureScreen(onContinue: () -> Unit, onNotNow: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text(
            text = stringResource(R.string.disclosure_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(DisclosureTags.TITLE),
        )

        listOf(
            R.string.disclosure_intro,
            R.string.disclosure_lead,
            R.string.disclosure_reads,
            R.string.disclosure_never_reads,
            R.string.disclosure_does,
            R.string.disclosure_where,
        ).forEach { paragraph ->
            Text(
                text = stringResource(paragraph),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .testTag(DisclosureTags.CONTINUE),
        ) {
            Text(stringResource(R.string.disclosure_continue))
        }

        OutlinedButton(
            onClick = onNotNow,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .testTag(DisclosureTags.NOT_NOW),
        ) {
            Text(stringResource(R.string.disclosure_not_now))
        }

        Text(
            text = stringResource(R.string.disclosure_footnote),
            style = Numeric,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(DisclosureTags.FOOTNOTE),
        )
    }
}

@Composable
fun AffirmativeConsentScreen(onAgree: () -> Unit, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        Text(
            text = stringResource(R.string.consent_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.testTag(DisclosureTags.CONSENT_TITLE),
        )

        listOf(R.string.consent_body, R.string.consent_detail).forEach { paragraph ->
            Text(
                text = stringResource(paragraph),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Button(
            onClick = onAgree,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .testTag(DisclosureTags.AGREE),
        ) {
            Text(stringResource(R.string.consent_agree))
        }

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 52.dp)
                .testTag(DisclosureTags.CANCEL),
        ) {
            Text(stringResource(R.string.consent_cancel))
        }

        Text(
            text = stringResource(R.string.disclosure_footnote),
            style = Numeric,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
