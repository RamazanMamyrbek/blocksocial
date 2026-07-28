package com.blocksocial.spike.a03

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Surface = Color(0xFF11171B)
private val SurfaceContainer = Color(0xFF1B2127)
private val OnSurface = Color(0xFFE8ECEF)
private val OnSurfaceVariant = Color(0xFFA1A9AF)
private val Outline = Color(0xFF50565B)

@Composable
fun CatalogScreen(
    items: List<ResolvedCatalogItem>,
    iconFor: (String) -> Painter?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface)
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = stringResource(R.string.catalog_title),
            color = OnSurface,
            fontSize = 26.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 56.dp, bottom = 4.dp)
        )
        Text(
            text = stringResource(
                R.string.catalog_subtitle,
                items.count { it.state is CatalogItemState.Installed },
                items.size
            ),
            color = OnSurfaceVariant,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        LazyColumn {
            items(items, key = { it.entry.id }) { item ->
                CatalogRow(item, iconFor)
            }
        }
    }
}

@Composable
private fun CatalogRow(item: ResolvedCatalogItem, iconFor: (String) -> Painter?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceContainer)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (val state = item.state) {
            is CatalogItemState.Installed -> {
                val painter = iconFor(state.packageName)
                if (painter != null) {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp)
                    )
                } else {
                    PlaceholderIcon()
                }
            }

            CatalogItemState.NotInstalled -> PlaceholderIcon()
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when (val state = item.state) {
                    is CatalogItemState.Installed -> state.label
                    CatalogItemState.NotInstalled -> item.entry.displayName
                },
                color = OnSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = when (item.state) {
                    is CatalogItemState.Installed -> stringResource(R.string.catalog_installed)
                    CatalogItemState.NotInstalled -> stringResource(R.string.catalog_not_installed)
                },
                color = OnSurfaceVariant,
                fontSize = 13.sp
            )
        }

        Text(
            text = when (item.state) {
                is CatalogItemState.Installed -> stringResource(R.string.catalog_mark_installed)
                CatalogItemState.NotInstalled -> stringResource(R.string.catalog_mark_absent)
            },
            color = OnSurfaceVariant,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun PlaceholderIcon() {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Outline, RoundedCornerShape(10.dp))
    )
}
