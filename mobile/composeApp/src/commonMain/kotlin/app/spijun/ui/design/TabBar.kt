package app.spijun.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.spijun.ui.platform.isIos

data class TabItem<T>(val id: T, val label: String, val icon: String, val selectedIcon: String = icon)

/**
 * The tab bar (components.md). iOS: a floating capsule, 64 tall, inset 24 / 22, the selected item in
 * a 54 tall `tabSelected` capsule. Android: a flat 80 dp bar with a neutral 56×32 indicator, not a
 * brand colour. Labels are always visible.
 */
@Composable
fun <T> SpyTabBar(items: List<TabItem<T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    if (isIos) IosTabBar(items, selected, onSelect, modifier) else AndroidTabBar(items, selected, onSelect, modifier)
}

@Composable
private fun <T> AndroidTabBar(items: List<TabItem<T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier) {
    val c = Spy.colors
    Column(modifier.fillMaxWidth().background(c.surface).windowInsetsPadding(WindowInsets.navigationBars)) {
        Row(Modifier.fillMaxWidth().height(80.dp), verticalAlignment = Alignment.CenterVertically) {
            items.forEach { item ->
                val on = item.id == selected
                Column(
                    Modifier
                        .weight(1f)
                        .height(80.dp)
                        .clickable(role = Role.Tab) { onSelect(item.id) }
                        .semantics { this.selected = on },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        Modifier.size(56.dp, 32.dp).clip(RoundedCornerShape(16.dp)).background(if (on) c.tabSelected else androidx.compose.ui.graphics.Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        SpyIcon(if (on) item.selectedIcon else item.icon, if (on) c.text else c.textMuted, size = 24.dp)
                    }
                    Text(
                        item.label,
                        fontFamily = Spy.type.body.fontFamily,
                        fontSize = 12.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        color = if (on) c.text else c.textMuted,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> IosTabBar(items: List<TabItem<T>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier) {
    val c = Spy.colors
    val shape = RoundedCornerShape(50)
    Box(modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars).padding(start = 24.dp, end = 24.dp, bottom = 22.dp)) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).shadow(12.dp, shape, clip = false).clip(shape).background(c.glass).border(1.dp, c.glassLine, shape).padding(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                val on = item.id == selected
                Column(
                    Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(shape)
                        .background(if (on) c.tabSelected else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable(role = Role.Tab) { onSelect(item.id) }
                        .semantics { this.selected = on },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    SpyIcon(if (on) item.selectedIcon else item.icon, if (on) c.text else c.textMuted, size = 22.dp)
                    Text(
                        item.label,
                        fontFamily = Spy.type.body.fontFamily,
                        fontSize = 11.sp,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        color = if (on) c.text else c.textMuted,
                    )
                }
            }
        }
    }
}
