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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.whh11.dqdlite.data.DqdApi
import com.whh11.dqdlite.data.DqdLeague
import com.whh11.dqdlite.data.DqdPlayerItem
import com.whh11.dqdlite.data.DqdStandingItem

/**
 * 懂球帝全项目数据版。
 * 覆盖：欧洲足球、亚洲足球、美洲足球、非洲足球、女子足球、全民足球、篮球、电竞、体坛与网球
 */
/** 队名/球员名含空格、中点等字符时，拼进 URL 前必须编码 */
private fun urlEncode(s: String): String =
    runCatching { java.net.URLEncoder.encode(s, "UTF-8") }.getOrDefault(s)

@Composable
fun DataTab(reload: Int, onOpen: (String, String) -> Unit) {
    val categories = listOf("全部", "欧洲足球", "亚洲足球", "美洲足球", "非洲足球", "女子足球", "全民足球", "篮球", "电竞", "体坛与网球")
    var selectedCategoryIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedCategory = categories[selectedCategoryIndex]

    val allLeagues = DqdApi.LEAGUES
    val filteredLeagues = remember(selectedCategoryIndex) {
        if (selectedCategory == "全部") allLeagues
        else allLeagues.filter { it.category == selectedCategory }
    }

    var selectedLeagueIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentLeague = filteredLeagues.getOrNull(selectedLeagueIndex) ?: filteredLeagues.firstOrNull() ?: allLeagues.first()

    // 0: 积分榜/排名, 1: 射手榜, 2: 助攻榜
    var subTab by rememberSaveable { mutableIntStateOf(0) }

    var standings by remember { mutableStateOf<List<DqdStandingItem>>(emptyList()) }
    var scorers by remember { mutableStateOf<List<DqdPlayerItem>>(emptyList()) }
    var assists by remember { mutableStateOf<List<DqdPlayerItem>>(emptyList()) }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val hasScorers = currentLeague.scorersUrl.isNotBlank()
    val hasAssists = currentLeague.assistsUrl.isNotBlank()

    LaunchedEffect(currentLeague, subTab, reload) {
        loading = true
        error = null
        try {
            when (subTab) {
                0 -> standings = DqdApi.standings(currentLeague)
                1 -> if (hasScorers) scorers = DqdApi.players(currentLeague.scorersUrl)
                2 -> if (hasAssists) assists = DqdApi.players(currentLeague.assistsUrl)
            }
        } catch (e: Exception) {
            error = e.message ?: "加载数据失败"
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        // 一级分类选择（欧洲、亚洲、美洲、非洲、女足、全民足球、篮球、电竞、体坛）
        LazyRow(
            Modifier
                .fillMaxWidth()
                .background(DqdColors.Card)
                .padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(categories) { index, cat ->
                val active = index == selectedCategoryIndex
                Box(
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (active) DqdColors.Primary else DqdColors.Border)
                        .clickable {
                            selectedCategoryIndex = index
                            selectedLeagueIndex = 0
                            subTab = 0
                        }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        cat,
                        color = if (active) Color.White else DqdColors.TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // 二级联赛选择
        LazyRow(
            Modifier
                .fillMaxWidth()
                .background(DqdColors.Bg)
                .padding(vertical = 6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(filteredLeagues) { index, league ->
                val active = league.id == currentLeague.id
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) DqdColors.Primary.copy(alpha = 0.15f) else DqdColors.Card)
                        .clickable {
                            selectedLeagueIndex = index
                            subTab = 0
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        league.name,
                        color = if (active) DqdColors.Primary else DqdColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // 三级维度（积分/排名、射手榜、助攻榜）
        if (hasScorers || hasAssists) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("积分榜", "射手榜", "助攻榜").forEachIndexed { index, title ->
                    if (index == 0 || (index == 1 && hasScorers) || (index == 2 && hasAssists)) {
                        val active = subTab == index
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) DqdColors.Primary else DqdColors.Card)
                                .clickable { subTab = index }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                title,
                                color = if (active) Color.White else DqdColors.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            when {
                loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DqdColors.Primary, modifier = Modifier.size(32.dp))
                    }
                }
                error != null && standings.isEmpty() && scorers.isEmpty() && assists.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error ?: "网络加载失败", color = DqdColors.TextSecondary, fontSize = 14.sp)
                    }
                }
                subTab == 0 -> {
                    StandingsView(
                        standings = standings,
                        isBasketball = currentLeague.isBasketball,
                        onClick = { row ->
                            val url = if (row.teamId.isNotBlank()) {
                                "https://www.dongqiudi.com/team/${row.teamId}"
                            } else {
                                "https://www.dongqiudi.com/search?keyword=" + urlEncode(row.teamName)
                            }
                            onOpen(url, row.teamName)
                        }
                    )
                }
                subTab == 1 -> {
                    PlayersView(players = scorers, statName = "进球") { p ->
                        val url = if (p.playerId.isNotBlank()) {
                            "https://www.dongqiudi.com/player/${p.playerId}"
                        } else {
                            "https://www.dongqiudi.com/search?keyword=" + urlEncode(p.playerName)
                        }
                        onOpen(url, p.playerName)
                    }
                }
                else -> {
                    PlayersView(players = assists, statName = "助攻") { p ->
                        val url = if (p.playerId.isNotBlank()) {
                            "https://www.dongqiudi.com/player/${p.playerId}"
                        } else {
                            "https://www.dongqiudi.com/search?keyword=" + urlEncode(p.playerName)
                        }
                        onOpen(url, p.playerName)
                    }
                }
            }
        }
    }
}

@Composable
private fun StandingsView(
    standings: List<DqdStandingItem>,
    isBasketball: Boolean,
    onClick: (DqdStandingItem) -> Unit
) {
    if (standings.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无积分排行数据", color = DqdColors.TextSecondary, fontSize = 14.sp)
        }
        return
    }

    val grouped = standings.groupBy { it.groupTitle }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    ) {
        grouped.forEach { (groupTitle, list) ->
            if (groupTitle.isNotBlank()) {
                item(key = "g_$groupTitle") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            groupTitle,
                            color = DqdColors.Primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 表头
            item(key = "header_$groupTitle") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(DqdColors.Card)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#", color = DqdColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                    Text("球队", color = DqdColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    if (isBasketball) {
                        Text("胜/负", color = DqdColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(55.dp), textAlign = TextAlign.Center)
                        Text("胜率", color = DqdColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(50.dp), textAlign = TextAlign.Center)
                        Text("胜差", color = DqdColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
                    } else {
                        Text("赛", color = DqdColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                        Text("胜/平/负", color = DqdColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(62.dp), textAlign = TextAlign.Center)
                        Text("净", color = DqdColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                        Text("分", color = DqdColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                    }
                }
            }

            itemsIndexed(list, key = { index, item -> "${groupTitle}_${item.rank}_${item.teamName}_$index" }) { _, row ->
                StandingRow(row = row, isBasketball = isBasketball) { onClick(row) }
            }
        }
    }
}

@Composable
private fun StandingRow(row: DqdStandingItem, isBasketball: Boolean, onClick: () -> Unit) {
    val rankInt = row.rank.toIntOrNull() ?: 99
    val rankColor = when (rankInt) {
        1 -> Color(0xFFE53935)
        2 -> Color(0xFFFF9800)
        3 -> Color(0xFFFBC02D)
        in 4..6 -> DqdColors.Primary
        else -> DqdColors.TextSecondary
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 0.5.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(DqdColors.Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 排名 + 升降箭头（官方接口提供 last_rank）
        Column(
            Modifier.width(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                row.rank,
                color = rankColor,
                fontSize = 12.sp,
                fontWeight = if (rankInt <= 3) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
            row.rankDelta?.let { d ->
                if (d != 0) {
                    Text(
                        if (d > 0) "▲$d" else "▼${-d}",
                        color = if (d > 0) DqdColors.Green else DqdColors.Red,
                        fontSize = 7.sp,
                        lineHeight = 8.sp
                    )
                }
            }
        }

        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!row.teamLogo.isNullOrBlank()) {
                AsyncImage(
                    model = row.teamLogo,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(7.dp))
            }
            Text(
                row.teamName,
                color = DqdColors.TextPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isBasketball) {
            Text("${row.win}/${row.loss}", color = DqdColors.TextPrimary, fontSize = 11.sp, modifier = Modifier.width(55.dp), textAlign = TextAlign.Center)
            Text(row.winRate.ifBlank { row.points }, color = DqdColors.TextPrimary, fontSize = 11.sp, modifier = Modifier.width(50.dp), textAlign = TextAlign.Center)
            Text(row.gamesBehind.ifBlank { "-" }, color = DqdColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
        } else {
            Text(row.played, color = DqdColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
            Text("${row.win}/${row.draw}/${row.loss}", color = DqdColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(62.dp), textAlign = TextAlign.Center)
            Text(row.goals, color = DqdColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
            Text(row.points, color = DqdColors.Primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PlayersView(
    players: List<DqdPlayerItem>,
    statName: String,
    onClick: (DqdPlayerItem) -> Unit
) {
    if (players.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无球员排行榜数据", color = DqdColors.TextSecondary, fontSize = 14.sp)
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(DqdColors.Card)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", color = DqdColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                Text("球员", color = DqdColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("球队", color = DqdColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(100.dp))
                Text(statName, color = DqdColors.TextSecondary, fontSize = 11.sp, modifier = Modifier.width(48.dp), textAlign = TextAlign.End)
            }
        }

        itemsIndexed(players, key = { index, p -> "${p.rank}_${p.playerName}_$index" }) { _, player ->
            PlayerRow(player = player) { onClick(player) }
        }
    }
}

@Composable
private fun PlayerRow(player: DqdPlayerItem, onClick: () -> Unit) {
    val rankInt = player.rank.toIntOrNull() ?: 99
    val rankColor = when (rankInt) {
        1 -> Color(0xFFE53935)
        2 -> Color(0xFFFF9800)
        3 -> Color(0xFFFBC02D)
        else -> DqdColors.TextSecondary
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 0.5.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(DqdColors.Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            player.rank,
            color = rankColor,
            fontSize = 12.sp,
            fontWeight = if (rankInt <= 3) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.width(24.dp),
            textAlign = TextAlign.Center
        )

        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!player.playerLogo.isNullOrBlank()) {
                AsyncImage(
                    model = player.playerLogo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                player.playerName,
                color = DqdColors.TextPrimary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            player.teamName,
            color = DqdColors.TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.width(100.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            player.count,
            color = DqdColors.Primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(48.dp),
            textAlign = TextAlign.End
        )
    }
}
