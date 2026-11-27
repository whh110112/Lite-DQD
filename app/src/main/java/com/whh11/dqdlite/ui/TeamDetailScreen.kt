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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.whh11.dqdlite.data.DqdApi
import com.whh11.dqdlite.data.FollowedTeam
import com.whh11.dqdlite.data.LocalStore
import com.whh11.dqdlite.data.Match
import com.whh11.dqdlite.data.News
import com.whh11.dqdlite.data.TeamDetail
import kotlinx.coroutines.launch

/**
 * 懂球帝全原生球队主页（赛程战绩 + 关联新闻 + 一线队名单 + 资料荣誉）。
 */
@Composable
fun TeamDetailScreen(
    teamId: String,
    fallbackName: String,
    onBack: () -> Unit,
    onOpenMatch: (String) -> Unit,
    onOpenArticle: (News) -> Unit,
    onOpenPlayer: (personId: String, playerName: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<TeamDetail?>(null) }
    var relatedNews by remember { mutableStateOf<List<News>>(emptyList()) }
    var newsLoading by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var subTab by remember { mutableIntStateOf(0) } // 0=赛程, 1=新闻, 2=大名单, 3=资料荣誉

    val followed by LocalStore.followedTeams.collectAsState()
    val isFollowed = remember(followed, teamId, fallbackName) {
        followed.any { (teamId.isNotBlank() && it.teamId == teamId) || it.name == fallbackName }
    }

    suspend fun load() {
        loading = true
        error = null
        runCatching { DqdApi.teamDetail(teamId) }
            .onSuccess { td ->
                detail = td
                loading = false
                newsLoading = true
                val kws = listOf(td.name, fallbackName, td.name.removeSuffix("俱乐部"))
                relatedNews = runCatching { DqdApi.relatedNews(kws) }.getOrDefault(emptyList())
                newsLoading = false
            }
            .onFailure {
                error = it.message ?: "球队主页加载失败"
                loading = false
                newsLoading = false
            }
    }

    LaunchedEffect(teamId) { load() }

    Column(
        Modifier
            .fillMaxSize()
            .background(DqdColors.Bg)
    ) {
        // 顶部栏
        Row(
            Modifier
                .fillMaxWidth()
                .background(DqdColors.Card)
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = DqdColors.TextPrimary)
            }
            val td = detail
            TeamCrest(td?.logo, 28)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    td?.name?.ifBlank { fallbackName } ?: fallbackName,
                    color = DqdColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!td?.enName.isNullOrBlank()) {
                    Text(td!!.enName, color = DqdColors.TextTertiary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            IconButton(onClick = {
                val tName = td?.name?.ifBlank { fallbackName } ?: fallbackName
                LocalStore.toggleFollowTeam(FollowedTeam(teamId, tName, td?.logo))
            }) {
                Icon(
                    if (isFollowed) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "关注",
                    tint = if (isFollowed) DqdColors.Amber else DqdColors.TextSecondary
                )
            }
        }

        // 子 Tab
        Row(
            Modifier
                .fillMaxWidth()
                .background(DqdColors.Card)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            listOf("赛程", "新闻", "大名单", "资料").forEachIndexed { idx, label ->
                val active = subTab == idx
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { subTab = idx }
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

        when {
            loading -> LoadingBox()
            error != null && detail == null -> ErrorRetryBox(error!!, onRetry = { scope.launch { load() } })
            else -> {
                val td = detail!!
                when (subTab) {
                    0 -> TeamScheduleTab(td.schedule, onOpenMatch)
                    1 -> TeamNewsTab(newsLoading, relatedNews, td.name, onOpenArticle)
                    2 -> TeamMembersTab(td.members, onOpenPlayer)
                    else -> TeamFactsTab(td)
                }
            }
        }
    }
}

@Composable
private fun TeamScheduleTab(schedule: List<Match>, onOpenMatch: (String) -> Unit) {
    if (schedule.isEmpty()) {
        EmptyBox("暂无本赛季赛程数据")
        return
    }
    val ordered = remember(schedule) {
        val upcoming = schedule.filter { !it.isPlayed }
        val played = schedule.filter { it.isPlayed }.reversed()
        upcoming + played
    }
    LazyColumn(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(ordered, key = { it.id }) { m ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(DqdColors.Card)
                    .clickable { onOpenMatch(m.id) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.width(74.dp)) {
                    Text(m.dateGroup, color = DqdColors.TextSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                    Text(m.competition, color = DqdColors.TextTertiary, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    m.teamA,
                    color = DqdColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (m.teamAWon) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .width(64.dp)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (m.hasScore) "${m.scoreA} - ${m.scoreB}" else m.timeLabel.ifBlank { "VS" },
                        color = if (m.isLive) DqdColors.Primary else if (m.hasScore) DqdColors.TextPrimary else DqdColors.TextSecondary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    m.teamB,
                    color = DqdColors.TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = if (m.teamBWon) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = DqdColors.Divider)
        }
    }
}

@Composable
private fun TeamNewsTab(
    loading: Boolean,
    list: List<News>,
    teamName: String,
    onOpenArticle: (News) -> Unit
) {
    when {
        loading -> LoadingBox()
        list.isEmpty() -> EmptyBox("近期频道中暂无「$teamName」相关新闻")
        else -> LazyColumn(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            items(list, key = { "${it.id}_${it.title}" }) { item ->
                NewsRowItem(item, onClick = { onOpenArticle(item) })
                HorizontalDivider(thickness = 0.5.dp, color = DqdColors.Divider)
            }
        }
    }
}

@Composable
private fun TeamMembersTab(
    members: List<com.whh11.dqdlite.data.TeamMemberItem>,
    onOpenPlayer: (String, String) -> Unit
) {
    if (members.isEmpty()) {
        EmptyBox("暂无一线队大名单")
        return
    }
    val grouped = remember(members) { members.groupBy { it.positionGroup } }
    LazyColumn(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        grouped.forEach { (pos, list) ->
            if (pos.isNotBlank()) {
                item(key = "pos_$pos") {
                    Text(
                        pos,
                        color = DqdColors.Primary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            items(list, key = { "${pos}_${it.personId}" }) { p ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(DqdColors.Card)
                        .clickable { onOpenPlayer(p.personId, p.name) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!p.avatar.isNullOrBlank()) {
                        AsyncImage(
                            model = p.avatar,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(DqdColors.SurfaceHigh)
                        )
                    } else {
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(DqdColors.SurfaceHigh),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(p.name.take(1), color = DqdColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, color = DqdColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        val sub = listOfNotNull(
                            p.number.takeIf { it.isNotBlank() }?.let { "${it}号" },
                            p.appearances.takeIf { it.isNotBlank() && it != "0" }?.let { "出场 $it" },
                            p.goals.takeIf { it.isNotBlank() && it != "0" }?.let { "进球 $it" }
                        ).joinToString(" · ")
                        if (sub.isNotBlank()) {
                            Text(sub, color = DqdColors.TextTertiary, fontSize = 11.5.sp)
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = DqdColors.Divider)
            }
        }
    }
}

@Composable
private fun TeamFactsTab(td: TeamDetail) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (td.facts.isNotEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DqdColors.Card)
                        .padding(14.dp)
                ) {
                    Text("俱乐部档案", color = DqdColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    td.facts.forEach { f ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text(f.label, color = DqdColors.TextTertiary, fontSize = 13.sp, modifier = Modifier.width(84.dp))
                            Text(f.value, color = DqdColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        if (td.honors.isNotEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DqdColors.Card)
                        .padding(14.dp)
                ) {
                    Text("主要荣誉", color = DqdColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    td.honors.forEach { h ->
                        Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            TeamCrest(h.logo, 22)
                            Spacer(Modifier.width(10.dp))
                            Text(h.name, color = DqdColors.TextPrimary, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
                            Text("${h.times}次", color = DqdColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
