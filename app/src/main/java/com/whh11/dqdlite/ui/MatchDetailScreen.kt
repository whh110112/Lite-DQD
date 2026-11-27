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
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.whh11.dqdlite.data.DqdApi
import com.whh11.dqdlite.data.LineupPlayer
import com.whh11.dqdlite.data.MatchDetail
import com.whh11.dqdlite.data.MatchEvent
import com.whh11.dqdlite.data.MatchStat
import kotlinx.coroutines.launch

/**
 * 原生比赛详情中心（赛况事件轴 + 技术统计对比条 + 双方首发与替补阵容）。
 */
@Composable
fun MatchDetailScreen(
    matchId: String,
    onBack: () -> Unit,
    onOpenWeb: (String, String) -> Unit,
    onOpenTeam: (teamId: String, teamName: String) -> Unit = { _, _ -> },
    onOpenPlayer: (personId: String, playerName: String) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<MatchDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var subTab by remember { mutableIntStateOf(0) } // 0=赛况与统计, 1=首发阵容, 2=赛前前瞻

    suspend fun load() {
        loading = true
        error = null
        runCatching { DqdApi.matchDetail(matchId) }
            .onSuccess { d ->
                detail = d
                if (d.stats.isEmpty() && d.events.isEmpty() && d.analysis != null) {
                    subTab = 2
                }
            }
            .onFailure { error = it.message ?: "加载比赛详情失败" }
        loading = false
    }

    LaunchedEffect(matchId) { load() }

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
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = DqdColors.TextPrimary)
            }
            Text(
                detail?.competition ?: "比赛详情",
                color = DqdColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = { scope.launch { load() } }) {
                Icon(Icons.Outlined.Refresh, contentDescription = "刷新", tint = DqdColors.TextSecondary)
            }
            IconButton(onClick = {
                onOpenWeb("https://m.dongqiudi.com/match/$matchId", detail?.let { "${it.teamA} vs ${it.teamB}" } ?: "比赛")
            }) {
                Icon(Icons.Outlined.OpenInBrowser, contentDescription = "网页版", tint = DqdColors.TextSecondary)
            }
        }

        when {
            loading && detail == null -> LoadingBox()
            error != null && detail == null -> ErrorRetryBox(error!!, onRetry = { scope.launch { load() } })
            else -> {
                val m = detail!!
                // 顶部记分牌卡片
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(DqdColors.Card)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            Modifier
                                .weight(1f)
                                .clickable(enabled = m.teamAId.isNotBlank()) { onOpenTeam(m.teamAId, m.teamA) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TeamCrest(m.logoA, 44)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                m.teamA,
                                color = DqdColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Column(
                            Modifier.width(108.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                if (m.scoreA.isNotBlank() && m.scoreB.isNotBlank()) "${m.scoreA} - ${m.scoreB}" else "VS",
                                color = DqdColors.TextPrimary,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DqdColors.PrimaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    when {
                                        m.status.equals("Played", true) -> "已完场"
                                        m.period.isNotBlank() -> m.period
                                        else -> m.startTime.takeLast(5).ifBlank { "未开赛" }
                                    },
                                    color = DqdColors.Primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Column(
                            Modifier
                                .weight(1f)
                                .clickable(enabled = m.teamBId.isNotBlank()) { onOpenTeam(m.teamBId, m.teamB) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            TeamCrest(m.logoB, 44)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                m.teamB,
                                color = DqdColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    val meta = listOf(m.venue, m.weather, if (m.referee.isNotBlank()) "主裁: ${m.referee}" else "")
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                    if (meta.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(meta, color = DqdColors.TextTertiary, fontSize = 11.5.sp, textAlign = TextAlign.Center)
                    }
                }

                // 子 Tab：赛况与统计 / 首发阵容 / 赛前前瞻
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(DqdColors.Card)
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    listOf("赛况与统计", "首发阵容", "赛前前瞻").forEachIndexed { idx, label ->
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
                                fontSize = 14.5.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(Modifier.height(5.dp))
                            Box(
                                Modifier
                                    .width(if (active) 24.dp else 0.dp)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(DqdColors.Primary)
                            )
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = DqdColors.Divider)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (subTab == 0) {
                        // 技术统计
                        if (m.stats.isNotEmpty()) {
                            item(key = "stats_card") {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DqdColors.Card)
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        "技术统计",
                                        color = DqdColors.TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    m.stats.forEach { stat ->
                                        StatCompareRow(stat)
                                        Spacer(Modifier.height(10.dp))
                                    }
                                }
                            }
                        }

                        // 关键事件时间轴
                        if (m.events.isNotEmpty()) {
                            item(key = "events_card") {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DqdColors.Card)
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        "比赛事件",
                                        color = DqdColors.TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    m.events.forEach { ev ->
                                        EventTimelineRow(ev)
                                        HorizontalDivider(
                                            Modifier.padding(vertical = 6.dp),
                                            thickness = 0.5.dp,
                                            color = DqdColors.Divider
                                        )
                                    }
                                }
                            }
                        }

                        if (m.stats.isEmpty() && m.events.isEmpty()) {
                            item(key = "empty_sit") {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 36.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("暂无本场赛况与技术统计（部分冷门或未开赛场次无统计）", color = DqdColors.TextTertiary, fontSize = 13.sp, textAlign = TextAlign.Center)
                                    if (m.analysis != null) {
                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            "查看赛前前瞻与交锋记录 →",
                                            color = DqdColors.Primary,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.clickable { subTab = 2 }
                                        )
                                    }
                                }
                            }
                        }
                    } else if (subTab == 1) {
                        // 首发阵容
                        if (m.lineupA.isEmpty() && m.lineupB.isEmpty()) {
                            item(key = "empty_lin") {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("暂未公布本场首发阵容", color = DqdColors.TextTertiary, fontSize = 13.5.sp)
                                }
                            }
                        } else {
                            item(key = "starting_xi") {
                                LineupCompareCard(
                                    title = "首发阵容",
                                    teamA = m.teamA,
                                    teamB = m.teamB,
                                    formationA = m.formationA,
                                    formationB = m.formationB,
                                    listA = m.lineupA,
                                    listB = m.lineupB
                                )
                            }
                            if (m.subsA.isNotEmpty() || m.subsB.isNotEmpty()) {
                                item(key = "subs") {
                                    LineupCompareCard(
                                        title = "替补席",
                                        teamA = m.teamA,
                                        teamB = m.teamB,
                                        formationA = "",
                                        formationB = "",
                                        listA = m.subsA,
                                        listB = m.subsB
                                    )
                                }
                            }
                        }
                    } else {
                        // 赛前前瞻（对赛往绩 + 联赛积分 + 双方近况 + 伤停名单）
                        val ana = m.analysis
                        if (ana == null) {
                            item(key = "empty_ana") {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("暂无本场赛前前瞻数据", color = DqdColors.TextTertiary, fontSize = 13.5.sp)
                                }
                            }
                        } else {
                            if (ana.tableA != null || ana.tableB != null) {
                                item(key = "ana_table") {
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(DqdColors.Card)
                                            .padding(14.dp)
                                    ) {
                                        Text("联赛积分对比", color = DqdColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(10.dp))
                                        ana.tableA?.let { t ->
                                            Text(
                                                "${m.teamA}：第${t.rank}名 · ${t.played}赛 ${t.win}胜${t.draw}平${t.loss}负 · ${t.points}分（胜率 ${t.winRate}）",
                                                color = DqdColors.TextSecondary,
                                                fontSize = 13.sp
                                            )
                                            Spacer(Modifier.height(6.dp))
                                        }
                                        ana.tableB?.let { t ->
                                            Text(
                                                "${m.teamB}：第${t.rank}名 · ${t.played}赛 ${t.win}胜${t.draw}平${t.loss}负 · ${t.points}分（胜率 ${t.winRate}）",
                                                color = DqdColors.TextSecondary,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                            if (ana.battleHistory.isNotEmpty()) {
                                item(key = "ana_bh") {
                                    AnalysisRecordCard(
                                        title = "对赛往绩",
                                        subtitle = ana.battleSummary,
                                        records = ana.battleHistory
                                    )
                                }
                            }
                            if (ana.recentA.isNotEmpty()) {
                                item(key = "ana_ra") {
                                    AnalysisRecordCard(
                                        title = "${m.teamA} 近期战绩",
                                        subtitle = "",
                                        records = ana.recentA
                                    )
                                }
                            }
                            if (ana.recentB.isNotEmpty()) {
                                item(key = "ana_rb") {
                                    AnalysisRecordCard(
                                        title = "${m.teamB} 近期战绩",
                                        subtitle = "",
                                        records = ana.recentB
                                    )
                                }
                            }
                            if (ana.sidelineA.isNotEmpty() || ana.sidelineB.isNotEmpty()) {
                                item(key = "ana_sl") {
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(DqdColors.Card)
                                            .padding(14.dp)
                                    ) {
                                        Text("伤停与缺席", color = DqdColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(8.dp))
                                        ana.sidelineA.forEach { p ->
                                            Text("${m.teamA} · ${p.name}（${p.reason}）", color = DqdColors.TextSecondary, fontSize = 12.5.sp)
                                            Spacer(Modifier.height(4.dp))
                                        }
                                        ana.sidelineB.forEach { p ->
                                            Text("${m.teamB} · ${p.name}（${p.reason}）", color = DqdColors.TextSecondary, fontSize = 12.5.sp)
                                            Spacer(Modifier.height(4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCompareRow(stat: MatchStat) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stat.valueA, color = DqdColors.TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            Text(
                stat.label,
                color = DqdColors.TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text(stat.valueB, color = DqdColors.TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(5.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        ) {
            Box(
                Modifier
                    .weight(stat.ratioA)
                    .fillMaxSize()
                    .background(DqdColors.Primary)
            )
            Spacer(Modifier.width(2.dp))
            Box(
                Modifier
                    .weight(1f - stat.ratioA)
                    .fillMaxSize()
                    .background(DqdColors.Blue)
            )
        }
    }
}

@Composable
private fun EventTimelineRow(ev: MatchEvent) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        // 主队侧
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (ev.isTeamA) {
                Text(ev.playerName, color = DqdColors.TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!ev.iconUrl.isNullOrBlank()) {
                    Spacer(Modifier.width(6.dp))
                    AsyncImage(model = ev.iconUrl, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
        Box(
            Modifier
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DqdColors.SurfaceHigh)
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Text(ev.minute, color = DqdColors.TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        // 客队侧
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!ev.isTeamA) {
                if (!ev.iconUrl.isNullOrBlank()) {
                    AsyncImage(model = ev.iconUrl, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(ev.playerName, color = DqdColors.TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun LineupCompareCard(
    title: String,
    teamA: String,
    teamB: String,
    formationA: String,
    formationB: String,
    listA: List<LineupPlayer>,
    listB: List<LineupPlayer>
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DqdColors.Card)
            .padding(14.dp)
    ) {
        Text(title, color = DqdColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(
                if (formationA.isNotBlank()) "$teamA ($formationA)" else teamA,
                color = DqdColors.Primary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (formationB.isNotBlank()) "$teamB ($formationB)" else teamB,
                color = DqdColors.Blue,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        val maxRows = maxOf(listA.size, listB.size)
        for (i in 0 until maxRows) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LineupPlayerCell(listA.getOrNull(i), alignEnd = false, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                LineupPlayerCell(listB.getOrNull(i), alignEnd = true, modifier = Modifier.weight(1f))
            }
            if (i != maxRows - 1) HorizontalDivider(thickness = 0.5.dp, color = DqdColors.Divider)
        }
    }
}

@Composable
private fun LineupPlayerCell(p: LineupPlayer?, alignEnd: Boolean, modifier: Modifier) {
    if (p == null) {
        Spacer(modifier)
        return
    }
    Row(
        modifier = modifier,
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!alignEnd) {
            if (!p.avatar.isNullOrBlank()) {
                AsyncImage(
                    model = p.avatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(DqdColors.SurfaceHigh)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                "${p.number.ifBlank { "-" }} ${p.name}${if (p.isCaptain) " (C)" else ""}",
                color = DqdColors.TextPrimary,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(
                "${if (p.isCaptain) "(C) " else ""}${p.name} ${p.number.ifBlank { "-" }}",
                color = DqdColors.TextPrimary,
                fontSize = 12.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
            if (!p.avatar.isNullOrBlank()) {
                Spacer(Modifier.width(6.dp))
                AsyncImage(
                    model = p.avatar,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(DqdColors.SurfaceHigh)
                )
            }
        }
    }
}

@Composable
private fun AnalysisRecordCard(
    title: String,
    subtitle: String,
    records: List<com.whh11.dqdlite.data.AnalysisRecordItem>
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DqdColors.Card)
            .padding(14.dp)
    ) {
        Text(title, color = DqdColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (subtitle.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = DqdColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.height(10.dp))
        records.forEachIndexed { idx, r ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.width(68.dp)) {
                    Text(r.date, color = DqdColors.TextTertiary, fontSize = 11.sp)
                    Text(r.competition, color = DqdColors.TextTertiary, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    r.teamA,
                    color = DqdColors.TextPrimary,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (r.scoreA.isNotBlank() && r.scoreB.isNotBlank()) "${r.scoreA}-${r.scoreB}" else "vs",
                    color = DqdColors.Primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(48.dp)
                )
                Text(
                    r.teamB,
                    color = DqdColors.TextPrimary,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            if (idx < records.lastIndex) {
                HorizontalDivider(thickness = 0.5.dp, color = DqdColors.Divider)
            }
        }
    }
}
