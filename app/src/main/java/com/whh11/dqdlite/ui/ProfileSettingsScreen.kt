package com.whh11.dqdlite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whh11.dqdlite.data.LocalStore
import com.whh11.dqdlite.data.SavedArticle
import com.whh11.dqdlite.data.ThemeMode

@Composable
fun ProfileSettingsScreen(
    onBack: () -> Unit,
    onOpenArticle: (id: Long, title: String) -> Unit
) {
    val themeMode by LocalStore.themeMode.collectAsState()
    val amoled by LocalStore.amoled.collectAsState()
    val fontScale by LocalStore.fontScale.collectAsState()
    val favorites by LocalStore.favorites.collectAsState()
    val history by LocalStore.history.collectAsState()

    var listTab by remember { mutableIntStateOf(0) } // 0 = 外观设置, 1 = 本地收藏, 2 = 阅读历史

    Column(
        Modifier
            .fillMaxSize()
            .background(DqdColors.Bg)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(DqdColors.Card)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = DqdColors.TextPrimary)
            }
            Text("我的与外观设置", color = DqdColors.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(DqdColors.Card)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            listOf("外观与关于", "收藏 (${favorites.size})", "历史 (${history.size})").forEachIndexed { idx, label ->
                val active = listTab == idx
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { listTab = idx }
                        .padding(top = 8.dp, bottom = 6.dp)
                ) {
                    Text(
                        label,
                        color = if (active) DqdColors.Primary else DqdColors.TextSecondary,
                        fontSize = 14.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        Modifier
                            .width(if (active) 20.dp else 0.dp)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(DqdColors.Primary)
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = DqdColors.Divider)

        when (listTab) {
            0 -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DqdColors.Card)
                            .padding(14.dp)
                    ) {
                        Text("主题模式", color = DqdColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeMode.entries.forEach { m ->
                                val active = m == themeMode
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) DqdColors.Primary else DqdColors.SurfaceHigh)
                                        .clickable { LocalStore.setThemeMode(m) }
                                        .padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        m.label,
                                        color = if (active) Color.White else DqdColors.TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("AMOLED 纯黑背景", color = DqdColors.TextPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                                Text("深色模式下使用 #000000 纯黑底省电防烧屏", color = DqdColors.TextTertiary, fontSize = 12.sp)
                            }
                            Switch(
                                checked = amoled,
                                onCheckedChange = { LocalStore.setAmoled(it) },
                                colors = SwitchDefaults.colors(checkedTrackColor = DqdColors.Primary)
                            )
                        }
                    }
                }

                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DqdColors.Card)
                            .padding(14.dp)
                    ) {
                        Text("显示字号", color = DqdColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("仅缩放文字大小，不改变图标与边距", color = DqdColors.TextTertiary, fontSize = 12.sp)
                        Spacer(Modifier.height(10.dp))
                        val scales = listOf("小" to 0.88f, "标准" to 1.0f, "大" to 1.12f, "特大" to 1.24f)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            scales.forEach { (label, value) ->
                                val active = kotlin.math.abs(fontScale - value) < 0.03f
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) DqdColors.Primary else DqdColors.SurfaceHigh)
                                        .clickable { LocalStore.setFontScale(value) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        color = if (active) Color.White else DqdColors.TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DqdColors.Card)
                            .padding(14.dp)
                    ) {
                        Text("关于 懂球帝 Lite", color = DqdColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "v3.0.0 · 全原生重构版\n零广告 · 全官方免鉴权 JSON 接口 · 原生文章与热评 · 原生赛况/阵型/统计",
                            color = DqdColors.TextSecondary,
                            fontSize = 12.5.sp,
                            lineHeight = 19.sp
                        )
                    }
                }
            }
            1 -> SavedArticleList(favorites, "暂无本地收藏文章", onOpenArticle)
            else -> Column(Modifier.fillMaxSize()) {
                if (history.isNotEmpty()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            "清空历史",
                            color = DqdColors.Red,
                            fontSize = 12.5.sp,
                            modifier = Modifier.clickable { LocalStore.clearHistory() }
                        )
                    }
                }
                SavedArticleList(history, "暂无阅读历史", onOpenArticle)
            }
        }
    }
}

@Composable
private fun SavedArticleList(
    list: List<SavedArticle>,
    emptyText: String,
    onOpenArticle: (Long, String) -> Unit
) {
    if (list.isEmpty()) {
        EmptyBox(emptyText)
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(list, key = { "${it.id}_${it.savedAt}" }) { a ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(DqdColors.Card)
                    .clickable { onOpenArticle(a.id, a.title) }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    a.title,
                    color = DqdColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (a.author.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(a.author, color = DqdColors.TextTertiary, fontSize = 11.5.sp)
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = DqdColors.Divider)
        }
    }
}
