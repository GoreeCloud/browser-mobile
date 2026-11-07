package com.goreecloud.browsermobile.ui

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Native staggered tab overview. Web previews exist only in volatile memory. */
internal enum class TabPreviewKind { WEB, HOME, NEW_TAB }

internal data class TabOverviewItem(
    val id: String, val title: String, val address: String, val webspace: String,
    val kind: TabPreviewKind, val selected: Boolean, val thumbnail: Bitmap?
)

internal data class TabOverviewColors(
    val canvas: Color, val surface: Color, val surfaceAlt: Color,
    val text: Color, val muted: Color, val accent: Color, val outline: Color
)

@Composable
internal fun TabOverviewScreen(
    tabs: List<TabOverviewItem>, colors: TabOverviewColors,
    onSelect: (String) -> Unit, onClose: (String) -> Unit,
    onNewTab: () -> Unit, onDismiss: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(colors.canvas)) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 12.dp, end = 20.dp, top = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Return to browser", tint = colors.text)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Your tabs", modifier = Modifier.semantics { heading() },
                    fontSize = 27.sp, fontWeight = FontWeight.Bold,
                    color = colors.text, letterSpacing = (-0.5).sp)
                Text("${tabs.size} open · Tap a preview to switch",
                    color = colors.muted, fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = onNewTab, enabled = tabs.size < 12,
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 13.dp, vertical = 10.dp),
                border = BorderStroke(1.dp, colors.outline)
            ) {
                Icon(Icons.Default.Add, contentDescription = null,
                    tint = colors.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text("New", color = colors.text, fontSize = 13.sp)
            }
        }
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(156.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 7.dp, 16.dp, 32.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 14.dp
        ) {
            itemsIndexed(tabs, key = { _, tab -> tab.id }) { index, tab ->
                TabPreviewCard(
                    tab, colors, position = index + 1, total = tabs.size,
                    previewHeight = if (index % 4 == 1 || index % 4 == 2) 204 else 252,
                    onSelect = { onSelect(tab.id) }, onClose = { onClose(tab.id) }
                )
            }
        }
    }
}

@Composable
private fun TabPreviewCard(
    tab: TabOverviewItem, colors: TabOverviewColors, position: Int, total: Int,
    previewHeight: Int,
    onSelect: () -> Unit, onClose: () -> Unit
) {
    val shape = RoundedCornerShape(23.dp)
    Column(
        modifier = Modifier.fillMaxWidth()
            .shadow(if (tab.selected) 7.dp else 3.dp, shape)
            .clip(shape).background(colors.surface)
            .border(if (tab.selected) 2.dp else 1.dp,
                if (tab.selected) colors.accent else colors.outline, shape)
            .clickable(onClickLabel = "Open tab $position of $total: ${tab.title}", onClick = onSelect)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(previewHeight.dp)
                .background(colors.surfaceAlt)
        ) {
            val preview = tab.thumbnail
            if (tab.kind == TabPreviewKind.WEB && preview != null && !preview.isRecycled) {
                Image(preview.asImageBitmap(), contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
            } else {
                TabPreviewFallback(tab.kind, colors)
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd).padding(7.dp).size(45.dp)
                    .shadow(3.dp, CircleShape)
                    .background(colors.surface.copy(alpha = 0.98f), CircleShape)
                    .border(1.dp, colors.outline, CircleShape)
            ) {
                Icon(Icons.Default.Close, "Close tab $position of $total: ${tab.title}",
                    tint = colors.text, modifier = Modifier.size(21.dp))
            }
            if (tab.selected) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
                    color = colors.accent, shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Active",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 13.dp)) {
            Text(tab.title, color = colors.text, fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 2,
                lineHeight = 18.sp, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(5.dp))
            Text(tab.address, color = colors.muted, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Text(tab.webspace, color = colors.accent, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TabPreviewFallback(kind: TabPreviewKind, colors: TabOverviewColors) {
    val web = kind == TabPreviewKind.WEB
    val home = kind == TabPreviewKind.HOME
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                if (web) listOf(colors.surfaceAlt, colors.canvas)
                else listOf(Color(0xFF273B71), Color(0xFF12243A))
            )
        ), contentAlignment = Alignment.Center
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Icon(
                when {
                    web -> Icons.Default.Language
                    home -> Icons.Default.Home
                    else -> Icons.Default.Tab
                }, contentDescription = null,
                tint = if (web) colors.accent else Color(0xFF8AE7D9),
                modifier = Modifier.size(30.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                when {
                    web -> "Preview unavailable"
                    home -> "Your space. Your way."
                    else -> "Where to next?"
                }, color = if (web) colors.text else Color.White,
                fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 20.sp
            )
            Spacer(Modifier.height(7.dp))
            Text(
                when {
                    web -> "Open the tab to view this website."
                    home -> "A quieter place to browse."
                    else -> "Your next discovery starts here."
                }, color = if (web) colors.muted else Color(0xFFCFDDF3),
                fontSize = 11.sp, lineHeight = 15.sp
            )
        }
    }
}
