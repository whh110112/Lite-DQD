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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whh11.dqdlite.data.DqdApi
import com.whh11.dqdlite.data.LocalStore
import com.whh11.dqdlite.data.Match
import com.whh11.dqdlite.data.News
import com.whh11.dqdlite.data.SportCategory

/**
 * 主队与关注中心（替代原假圈子）：
 * 聚合选中球队的精准关联比赛（按 teamId 或完整队名匹配）与官方搜索实时资讯流。
 */
@Composable
fun FollowTab(
    scrollToTopTrigger: Int,
    onOpenArticle: (News) -> Unit,
    onOpenMatch: (String) -> Unit,
    onOpenSearch: () -> Unit
) {
    val followed by LocalStore.followedTeams.collectAsState()
    val primaryId by LocalStore.primaryTeamId.collectAsState()

    val sortedTeams = remember(followed, primaryId) {
        followed.sortedByDescending { it.teamId == primaryId }
    }
    var selectedIdx by remember { mutableIntStateOf(0) }
    val currentTeam = sortedTeams.getOrNull(selectedIdx.coerceIn(0, (sortedTeams.size - 1).coerceAtLeast(0)))

    var teamMatches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var teamNews by remember { mutableStateOf<List<News>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(currentTeam?.teamId, currentTeam?.name, scrollToTopTrigger) {
        val t = currentTeam ?: return@LaunchedEffect
        loading = true
        runCatching { DqdApi.matches(SportCategory.ALL) }.onSuccess { all ->
            teamMatches = all.filter { m ->
                (t.teamId.isNotBlank() && (m.teamAId == t.teamId || m.teamBId == t.teamId)) ||
                    m.teamA == t.name || m.teamB == t.name
            }
        }
        runCatching { DqdApi.search(t.name) }.onSuccess { sr ->
            teamNews = sr.news
        }
        loading = false
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(DqdColors.Bg)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(DqdColors.Card)
                .statusBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("关注与主队", color = DqdColors.TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Outlined.Add, contentDescription = "搜索添加球队", tint = DqdColors.TextPrimary)
                }
            }

            if (sortedTeams.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(sortedTeams) { idx, team ->
                        val active = team.teamId == currentTeam?.teamId && team.name == currentTeam.name
                        val isPrimary = team.teamId == primaryId
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (active) DqdColors.Primary else DqdColors.SurfaceHigh)
                                .clickable { selectedIdx = idx }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            if (isPrimary) {
                                Icon(
                                    Icons.Rounded.Star,
                                    contentDescription = "主队",
                                    tint = if (active) Color(0xFFFFE066) else DqdColors.Amber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                team.name,
                                color = if (active) Color.White else DqdColors.TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = DqdColors.Divider)
        }

        if (currentTeam == null) {
            EmptyBox("点击右上角 + 搜索并关注你的主队")
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 主队卡片操作条
            item(key = "team_header") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DqdColors.Card)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TeamCrest(currentTeam.logo, 38)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(currentTeam.name, color = DqdColors.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        if (currentTeam.leagueHint.isNotBlank()) {
                            Text(currentTeam.leagueHint, color = DqdColors.TextTertiary, fontSize = 12.sp)
                        }
                    }
                    val isPrimary = currentTeam.teamId == primaryId
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isPrimary) DqdColors.PrimaryContainer else DqdColors.SurfaceHigh)
                            .clickable { LocalStore.setPrimaryTeam(currentTeam.teamId) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            if (isPrimary) "★ 我的主队" else "设为主队",
                            color = if (isPrimary) DqdColors.Primary else DqdColors.TextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 近期比赛
            if (teamMatches.isNotEmpty()) {
                item(key = "m_title") {
                    Text(
                        "近期赛程",
                        color = DqdColors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                itemsIndexed(teamMatches, key = { i, m -> "tm_${m.id}_$i" }) { _, m ->
                    DqdMatchRow(match = m, onClick = { onOpenMatch(m.id) })
                    HorizontalDivider(
                        Modifier
                            .background(DqdColors.Card)
                            .padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color = DqdColors.Divider
                    )
                }
            }

            // 相关资讯
            item(key = "n_title") {
                Text(
                    "${currentTeam.name} 相关资讯",
                    color = DqdColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)
                )
            }

            when {
                loading && teamNews.isEmpty() -> item(key = "ld") { Box(Modifier.height(160.dp)) { LoadingBox() } }
                teamNews.isEmpty() -> item(key = "em") {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("暂无 ${currentTeam.name} 最新关联资讯", color = DqdColors.TextTertiary, fontSize = 13.sp)
                    }
                }
                else -> itemsIndexed(teamNews, key = { i, n -> "tn_${n.id}_$i" }) { i, n ->
                    NewsRowItem(news = n, onClick = { onOpenArticle(n) })
                    if (i != teamNews.lastIndex) {
                        HorizontalDivider(
                            Modifier
                                .background(DqdColors.Card)
                                .padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = DqdColors.Divider
                        )
                    }
                }
            }
        }
    }
}
