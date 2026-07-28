package com.blocksocial.spike.a05

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class UsageScreenState(
    val access: UsageAccessState,
    val usage: List<ForegroundUsage>,
    val window: LocalDayWindow?
)

private val Surface = Color(0xFF11171B)
private val SurfaceContainer = Color(0xFF1B2127)
private val OnSurface = Color(0xFFE8ECEF)
private val OnSurfaceVariant = Color(0xFFA1A9AF)
private val Primary = Color(0xFF6FBEBE)
private val OnPrimary = Color(0xFF11171B)

@Composable
fun UsageScreen(state: UsageScreenState, onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = stringResource(R.string.usage_title),
            color = OnSurface,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 56.dp, bottom = 12.dp)
        )

        when (state.access) {
            UsageAccessState.DENIED -> {
                Text(
                    text = stringResource(R.string.usage_denied),
                    color = OnSurfaceVariant,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        contentColor = OnPrimary
                    )
                ) {
                    Text(stringResource(R.string.usage_open_settings), fontSize = 16.sp)
                }
            }

            UsageAccessState.GRANTED -> {
                Text(
                    text = stringResource(R.string.usage_granted, state.usage.size),
                    color = OnSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyColumn {
                    items(state.usage, key = { it.packageName }) { entry ->
                        UsageRow(entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageRow(entry: ForegroundUsage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceContainer)
            .padding(12.dp)
    ) {
        Text(
            text = entry.packageName,
            color = OnSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = stringResource(
                R.string.usage_row_detail,
                entry.totalMillis / 1000,
                entry.sessionCount,
                entry.confidence.name
            ),
            color = OnSurfaceVariant,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
