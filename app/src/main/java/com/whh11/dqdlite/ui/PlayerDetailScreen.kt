package com.whh11.dqdlite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.whh11.dqdlite.data.DqdApi
import com.whh11.dqdlite.data.Match
import com.whh11.dqdlite.data.News
import com.whh11.dqdlite.data.PlayerDetail
import kotlinx.coroutines.launch

/**
 * 懂球帝全原生球员主页（资料特点 + 相关新闻 + 所属球队赛程 + 生涯/转会/伤病）。
 */
@Composable
fun PlayerDetailScreen(
    personId: String,
    fallbackName: String,
    onBack: () -> Unit,
    onOpenArticle: (News) -> Unit,
    onOpenMatch: (String) -> Unit,
    onOpenTeam: (teamId: String, teamName: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<PlayerDetail?>(null) }
    var relatedNews by remember { mutableStateOf<List<News>>(emptyList()) }
    var teamSchedule by remember { mutableStateOf<List<Match>>(emptyList()) }
    var newsLoading by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var subTab by remember { mutableIntStateOf(0) } // 0=资料特点, 1=新闻, 2=赛程, 3=履历转会

    suspend fun load() {
        loading = true
        error = null
        runCatching { DqdApi.playerDetail(personId) }
            .onSuccess { pd ->
                detail = pd
                loading = false
                newsLoading = true
                relatedNews = runCatching {
                    DqdApi.relatedNews(listOf(pd.name, fallbackName))
                }.getOrDefault(emptyList())
                if (pd.teamId.isNotBlank()) {
                    teamSchedule = runCatching { DqdApi.teamDetail(pd.teamId).schedule }.getOrDefault(emptyList())
                }
                newsLoading = false
            }
            .onFailure {
                error = it.message ?: "球员主页加载失败"
                loading = false
                newsLoading = false
            }
    }

    LaunchedEffect(personId) { load() }

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
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = DqdColors.TextPrimary)
            }
            val pd = detail
            if (!pd?.avatar.isNullOrBlank()) {
                AsyncImage(
                    model = pd!!.avatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(DqdColors.SurfaceHigh)
                )
                Spacer(Modifier.width(8.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    pd?.name?.ifBlank { fallbackName } ?: fallbackName,
                    color = DqdColors.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!pd?.teamName.isNullOrBlank()) {
                    Text(
                        pd!!.teamName,
                        color = DqdColors.Primary,
                        fontSize = 11.5.sp,
                        modifier = Modifier.clickable(enabled = pd.teamId.isNotBlank()) {
                            onOpenTeam(pd.teamId, pd.teamName)
                        }
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(DqdColors.Card)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            listOf("资料特点", "新闻", "赛程", "履历").forEachIndexed { idx, label ->
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
                val pd = detail!!
                when (subTab) {
                    0 -> PlayerProfileTab(pd)
                    1 -> PlayerNewsTab(newsLoading, relatedNews, pd.name, onOpenArticle)
                    2 -> PlayerScheduleTab(teamSchedule, pd.teamName, onOpenMatch)
                    else -> PlayerCareerTab(pd, onOpenTeam)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayerProfileTab(pd: PlayerDetail) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (pd.facts.isNotEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DqdColors.Card)
                        .padding(14.dp)
                ) {
                    Text("基本档案", color = DqdColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    pd.facts.forEach { f ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text(f.label, color = DqdColors.TextTertiary, fontSize = 13.sp, modifier = Modifier.width(84.dp))
                            Text(f.value, color = DqdColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        if (pd.strengths.isNotEmpty() || pd.styles.isNotEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DqdColors.Card)
                        .padding(14.dp)
                ) {
                    Text("技术特点", color = DqdColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (pd.strengths.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pd.strengths.forEach { s ->
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DqdColors.PrimaryContainer)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(s, color = DqdColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                    if (pd.styles.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            pd.styles.forEach { s ->
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DqdColors.SurfaceHigh)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(s, color = DqdColors.TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (pd.honors.isNotEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DqdColors.Card)
                        .padding(14.dp)
                ) {
                    Text("所获荣誉", color = DqdColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    pd.honors.forEach { h ->
                        Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            TeamCrest(h.logo, 20)
                            Spacer(Modifier.width(8.dp))
                            Text(h.name, color = DqdColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text("${h.times}次", color = DqdColors.Primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerNewsTab(loading: Boolean, list: List<News>, playerName: String, onOpenArticle: (News) -> Unit) {
    when {
        loading -> LoadingBox()
        list.isEmpty() -> EmptyBox("近期频道中暂无「$playerName」相关新闻")
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
private fun PlayerScheduleTab(schedule: List<Match>, teamName: String, onOpenMatch: (String) -> Unit) {
    if (schedule.isEmpty()) {
        EmptyBox(if (teamName.isBlank()) "暂无所属球队赛程" else "暂无 $teamName 的本赛季赛程")
        return
    }
    val ordered = remember(schedule) {
        schedule.filter { !it.isPlayed } + schedule.filter { it.isPlayed }.reversed()
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
                    Text(m.dateGroup, color = DqdColors.TextSecondary, fontSize = 11.5.sp)
                    Text(m.competition, color = DqdColors.TextTertiary, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(m.teamA, color = DqdColors.TextPrimary, fontSize = 13.sp, textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(
                    if (m.hasScore) "${m.scoreA} - ${m.scoreB}" else m.timeLabel.ifBlank { "VS" },
                    color = DqdColors.Primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(62.dp)
                )
                Text(m.teamB, color = DqdColors.TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            }
            HorizontalDivider(thickness = 0.5.dp, color = DqdColors.Divider)
        }
    }
}

@Composable
private fun PlayerCareerTab(pd: PlayerDetail, onOpenTeam: (String, String) -> Unit) {
    LazyColumn(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (pd.careers.isNotEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DqdColors.Card)
                        .padding(14.dp)
                ) {
                    Text("俱乐部履历", color = DqdColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    pd.careers.forEach { c ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(enabled = c.teamId.isNotBlank()) { onOpenTeam(c.teamId, c.teamName) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TeamCrest(c.teamLogo, 22)
                            Spacer(Modifier.width(10.dp))
                            Text(c.teamName, color = DqdColors.TextPrimary, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                            Text("${c.startDate} ~ ${c.endDate}", color = DqdColors.TextTertiary, fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }
        if (pd.transfers.isNotEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DqdColors.Card)
                        .padding(14.dp)
                ) {
                    Text("转会记录", color = DqdColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    pd.transfers.forEach { t ->
                        Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(t.date, color = DqdColors.TextTertiary, fontSize = 11.5.sp, modifier = Modifier.width(78.dp))
                            Text("${t.fromClub} → ${t.toClub}", color = DqdColors.TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            if (t.fee.isNotBlank()) {
                                Text(t.fee, color = DqdColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
        if (pd.injuries.isNotEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DqdColors.Card)
                        .padding(14.dp)
                ) {
                    Text("伤病历史", color = DqdColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    pd.injuries.take(10).forEach { inj ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text("${inj.dateFrom}~${inj.dateUntil}", color = DqdColors.TextTertiary, fontSize = 11.5.sp, modifier = Modifier.width(126.dp))
                            Text(inj.injury, color = DqdColors.TextPrimary, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
                            if (inj.days.isNotBlank()) {
                                Text("${inj.days}天", color = DqdColors.Red, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
