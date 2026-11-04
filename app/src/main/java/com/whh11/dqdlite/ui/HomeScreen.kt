package com.whh11.dqdlite.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.whh11.dqdlite.R
import com.whh11.dqdlite.data.DqdApi
import com.whh11.dqdlite.data.News
import com.whh11.dqdlite.data.relTime

@Composable
fun HomeScreen(onOpen: (url: String, title: String) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var reload by remember { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .background(DqdColors.Bg)
    ) {
        BrandHeader(onReload = { reload++ })
        SegmentTabs(
            labels = listOf("资讯", "赛程", "圈子", "数据"),
            selected = tab,
            onSelect = { tab = it }
        )
        Box(Modifier.fillMaxSize()) {
            when (tab) {
                0 -> NewsTab(reload = reload, onOpen = onOpen)
                1 -> MatchTab(reload = reload, onOpen = onOpen)
                2 -> CommunityTab(reload = reload, onOpen = onOpen)
                else -> DataTab(reload = reload, onOpen = onOpen)
            }
        }
    }
}

@Composable
fun BrandHeader(onReload: () -> Unit) {
    val context = LocalContext.current
    val currentMode = ThemeManager.mode

    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_brand),
            contentDescription = "懂球帝",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "懂球帝",
            color = DqdColors.TextPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp
        )
        Spacer(Modifier.width(7.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(5.dp))
                .background(DqdColors.Green.copy(alpha = 0.14f))
                .padding(horizontal = 5.dp, vertical = 2.dp)
        ) {
            Text("LITE", color = DqdColors.Green, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(1f))

        // 主题切换按钮（跟随系统 -> 白天 -> 夜间）
        IconButton(onClick = {
            val nextMode = when (currentMode) {
                ThemeMode.SYSTEM -> ThemeMode.LIGHT
                ThemeMode.LIGHT -> ThemeMode.DARK
                ThemeMode.DARK -> ThemeMode.SYSTEM
            }
            ThemeManager.set(context, nextMode)
            Toast.makeText(context, "切换模式: ${nextMode.label}", Toast.LENGTH_SHORT).show()
        }) {
            val icon = when (currentMode) {
                ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                ThemeMode.LIGHT -> Icons.Rounded.LightMode
                ThemeMode.DARK -> Icons.Rounded.DarkMode
            }
            Icon(
                icon,
                contentDescription = "切换主题模式",
                tint = DqdColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(onClick = onReload) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = "刷新",
                tint = DqdColors.TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SegmentTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.padding(start = 16.dp, top = 6.dp, bottom = 2.dp)) {
        labels.forEachIndexed { i, label ->
            val active = i == selected
            Column(
                Modifier
                    .clickable { onSelect(i) }
                    .padding(end = 22.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Text(
                    label,
                    color = if (active) DqdColors.TextPrimary else DqdColors.TextTertiary,
                    fontSize = 17.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .width(if (active) 20.dp else 0.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(DqdColors.Green)
                )
            }
        }
    }
}

@Composable
private fun NewsTab(reload: Int, onOpen: (String, String) -> Unit) {
    val tabs = DqdApi.TABS
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var items by remember { mutableStateOf<List<News>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tabIndex, reload) {
        loading = true
        error = null
        runCatching { DqdApi.news(tabs[tabIndex].id) }
            .onSuccess { items = it }
            .onFailure { error = it.message ?: "网络异常" }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            itemsIndexed(tabs.map { it.label }) { i, label ->
                val active = i == tabIndex
                Box(
                    Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (active) DqdColors.Green.copy(alpha = 0.13f) else DqdColors.Surface)
                        .clickable { tabIndex = i }
                        .padding(horizontal = 13.dp, vertical = 7.dp)
                ) {
                    Text(
                        label,
                        color = if (active) DqdColors.Green else DqdColors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        when {
            loading && items.isEmpty() -> LoadingBlock()
            error != null && items.isEmpty() -> ErrorBlock(error!!)
            items.isEmpty() -> EmptyBlock("这个栏目暂时没有内容")
            else -> {
                // 集锦栏目的官方接口已停更（最后一批内容为 2024-10），
                // 不提示的话用户会以为 App 抓挂了
                val staleDays = remember(items) {
                    val newest = items.maxOfOrNull { it.showTime } ?: 0L
                    if (newest <= 0L) 0
                    else ((System.currentTimeMillis() / 1000 - newest) / 86400).toInt()
                }
                val isHighlights = tabs[tabIndex].id == DqdApi.TAB_HIGHLIGHTS

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 26.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (staleDays > 90) {
                        item { StaleNotice(days = staleDays, isHighlights = isHighlights) }
                    }
                    itemsIndexed(items) { i, n ->
                        NewsRow(n, videoLabel = if (isHighlights) "集锦" else "视频") {
                            onOpen(n.detailUrl, n.title)
                        }
                        if (i != items.lastIndex) {
                            Box(
                                Modifier
                                    .padding(start = 16.dp, end = 16.dp)
                                    .height(1.dp)
                                    .fillMaxWidth()
                                    .background(DqdColors.Line.copy(alpha = 0.55f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsRow(n: News, videoLabel: String = "视频", onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            if (n.isTop || n.isVideo) {
                Row(Modifier.padding(bottom = 5.dp)) {
                    if (n.isTop) {
                        MiniTag("置顶", DqdColors.Accent)
                        Spacer(Modifier.width(5.dp))
                    }
                    if (n.isVideo) MiniTag(videoLabel, DqdColors.Green)
                }
            }
            Text(
                n.title,
                color = DqdColors.TextPrimary,
                fontSize = 15.5.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val meta = listOfNotNull(
                    n.category.takeIf { it.isNotBlank() },
                    n.subCategory.takeIf { it.isNotBlank() && it != n.category }
                ).joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(meta, color = DqdColors.Green.copy(alpha = 0.85f), fontSize = 11.sp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(relTime(n.showTime), color = DqdColors.TextTertiary, fontSize = 11.sp)
                if (n.comments > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text("${n.comments} 评论", color = DqdColors.TextTertiary, fontSize = 11.sp)
                }
            }
        }
        n.thumb?.let { url ->
            Spacer(Modifier.width(12.dp))
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 104.dp, height = 70.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DqdColors.SurfaceHigh)
            )
        }
    }
}

@Composable
fun MiniTag(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 5.dp, vertical = 1.5.dp)
    ) {
        Text(text, color = color, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StaleNotice(days: Int, isHighlights: Boolean) {
    val text = if (isHighlights) {
        "集锦栏目官方接口已停止更新，下面是 2024 年 10 月前的存档；新集锦可去「赛程」里点比赛看"
    } else {
        "该栏目已 $days 天未更新"
    }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DqdColors.Accent.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Text(text, color = DqdColors.Accent, fontSize = 11.5.sp, lineHeight = 17.sp)
    }
}

@Composable
fun LoadingBlock() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("加载中…", color = DqdColors.TextTertiary, fontSize = 13.sp)
    }
}

@Composable
fun ErrorBlock(message: String) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("没抓到数据", color = DqdColors.TextSecondary, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text(message, color = DqdColors.TextTertiary, fontSize = 12.sp)
    }
}

@Composable
fun EmptyBlock(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = DqdColors.TextTertiary, fontSize = 13.sp)
    }
}
