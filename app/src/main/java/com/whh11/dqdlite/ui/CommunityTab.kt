package com.whh11.dqdlite.ui

import android.content.Context
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.whh11.dqdlite.data.DqdApi
import com.whh11.dqdlite.data.DqdCircle
import com.whh11.dqdlite.data.News
import com.whh11.dqdlite.data.relTime

/**
 * 懂球帝社区圈子模块。
 * 1. 包含俱乐部主队圈（皇马、巴萨、曼城、阿森纳、利物浦、曼联、拜仁、国足）与特色圈子（懂球妹、装备、深度）；
 * 2. 支持设置“我的主队”（永久置顶置前并带有金色主队标）；
 * 3. 支持关注/取消关注圈子，状态本地持久化；
 * 4. 专区讨论信息流支持点击查看文章与评论。
 */
@Composable
fun CommunityTab(reload: Int, onOpen: (String, String) -> Unit) {
    val context = LocalContext.current
    val sp = remember { context.getSharedPreferences("dqd_community", Context.MODE_PRIVATE) }

    var favTeam by remember { mutableStateOf(sp.getString("fav_team", "realm") ?: "realm") }
    var followedSet by remember {
        mutableStateOf(sp.getStringSet("followed", setOf("all", "realm", "barca", "mancity", "china")) ?: setOf("all"))
    }

    val circles = DqdApi.CIRCLES
    val sortedCircles = remember(favTeam, followedSet) {
        circles.sortedWith { a, b ->
            when {
                a.id == favTeam -> -2
                b.id == favTeam -> 2
                followedSet.contains(a.id) && !followedSet.contains(b.id) -> -1
                !followedSet.contains(a.id) && followedSet.contains(b.id) -> 1
                else -> 0
            }
        }
    }

    var circleIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentCircle = sortedCircles.getOrElse(circleIndex.coerceIn(0, sortedCircles.lastIndex)) { sortedCircles[0] }

    var items by remember { mutableStateOf<List<News>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentCircle.id, reload) {
        loading = true
        error = null
        runCatching {
            DqdApi.circleNews(currentCircle)
        }.onSuccess { items = it }
            .onFailure { error = it.message ?: "加载圈子内容失败" }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        // 圈子横向导航
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            itemsIndexed(sortedCircles) { i, c ->
                val active = c.id == currentCircle.id
                val isMyFav = c.id == favTeam

                Box(
                    Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (active) DqdColors.Green else DqdColors.Surface)
                        .clickable { circleIndex = i }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isMyFav) {
                            Icon(
                                Icons.Rounded.Star,
                                contentDescription = "主队",
                                tint = if (active) Color(0xFF00210A) else Color(0xFFFFC53D),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                        }
                        Text(
                            c.name,
                            color = if (active) Color(0xFF00210A) else DqdColors.TextSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // 圈子操作条：设为主队 + 关注
        if (currentCircle.id != "all") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 5.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DqdColors.Surface)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    currentCircle.name,
                    color = DqdColors.TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(6.dp))
                if (currentCircle.id == favTeam) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DqdColors.Accent.copy(alpha = 0.2f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("★ 我的主队", color = DqdColors.Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.weight(1f))

                // 设为主队
                val isFav = currentCircle.id == favTeam
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isFav) DqdColors.SurfaceHigh else DqdColors.Green.copy(alpha = 0.14f))
                        .clickable {
                            val newFav = if (isFav) "" else currentCircle.id
                            favTeam = newFav
                            sp.edit().putString("fav_team", newFav).apply()
                        }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (isFav) "取消主队" else "设为主队",
                        color = if (isFav) DqdColors.TextSecondary else DqdColors.Green,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.width(8.dp))

                // 关注圈子
                val isFollowed = followedSet.contains(currentCircle.id)
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isFollowed) DqdColors.SurfaceHigh else DqdColors.Green)
                        .clickable {
                            val newSet = followedSet.toMutableSet()
                            if (isFollowed) newSet.remove(currentCircle.id) else newSet.add(currentCircle.id)
                            followedSet = newSet
                            sp.edit().putStringSet("followed", newSet).apply()
                        }
                        .padding(horizontal = 9.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (isFollowed) "已关注" else "+ 关注",
                        color = if (isFollowed) DqdColors.TextSecondary else Color(0xFF00210A),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        when {
            loading && items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DqdColors.Green, modifier = Modifier.size(28.dp))
                }
            }
            error != null && items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error!!, color = DqdColors.TextTertiary, fontSize = 13.sp)
                }
            }
            items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("该圈子暂时没有讨论", color = DqdColors.TextTertiary, fontSize = 13.sp)
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(items) { i, n ->
                        CircleNewsRow(n) {
                            onOpen(n.detailUrl, n.title)
                        }
                        if (i != items.lastIndex) {
                            Box(
                                Modifier
                                    .padding(start = 16.dp, end = 16.dp)
                                    .height(1.dp)
                                    .fillMaxWidth()
                                    .background(DqdColors.Line.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CircleNewsRow(n: News, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                n.title,
                color = DqdColors.TextPrimary,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 21.sp
            )
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (n.subCategory.isNotBlank()) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(DqdColors.Green.copy(alpha = 0.12f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(n.subCategory, color = DqdColors.Green, fontSize = 9.5.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    relTime(n.showTime),
                    color = DqdColors.TextTertiary,
                    fontSize = 11.sp
                )
                Spacer(Modifier.weight(1f))
                if (n.comments > 0) {
                    Text(
                        "${n.comments} 讨论",
                        color = DqdColors.TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
        if (n.thumb != null) {
            Spacer(Modifier.width(12.dp))
            AsyncImage(
                model = n.thumb,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 80.dp, height = 54.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DqdColors.SurfaceHigh)
            )
        }
    }
}
