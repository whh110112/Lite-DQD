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
 * 懂球帝官方联赛数据版。
 * 覆盖：欧冠、英超、西甲、德甲、意甲、中超、法甲、NBA、CBA
 * 功能：
 * 1. 积分榜（与官方数据版一致：排名、队徽、胜平负、进/失、积分、资格区高亮）
 * 2. 球员榜（射手榜、助攻榜，含球员头像、所属球队）
 */
@Composable
fun DataTab(reload: Int) {
    val leagues = DqdApi.LEAGUES
    var leagueIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentLeague = leagues[leagueIndex]

    // 子维度：0: 积分榜, 1: 射手榜, 2: 助攻榜
    var subTab by rememberSaveable { mutableIntStateOf(0) }

    var standings by remember { mutableStateOf<List<DqdStandingItem>>(emptyList()) }
    var scorers by remember { mutableStateOf<List<DqdPlayerItem>>(emptyList()) }
    var assists by remember { mutableStateOf<List<DqdPlayerItem>>(emptyList()) }

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val isBasketball = currentLeague.id == "nba" || currentLeague.id == "cba"

    LaunchedEffect(leagueIndex, subTab, reload) {
        loading = true
        error = null
        runCatching {
            when {
                subTab == 0 || isBasketball -> standings = DqdApi.fetchStandings(currentLeague)
                subTab == 1 -> scorers = DqdApi.fetchScorers(currentLeague)
                subTab == 2 -> assists = DqdApi.fetchAssists(currentLeague)
            }
        }.onFailure {
            error = it.message ?: "加载榜单失败"
        }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        // 联赛选择栏（欧冠、英超、西甲、德甲、意甲、中超、法甲、NBA、CBA）
        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            itemsIndexed(leagues) { i, l ->
                val active = i == leagueIndex
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) DqdColors.Green else DqdColors.Surface)
                        .clickable {
                            leagueIndex = i
                            if (isBasketball) subTab = 0
                        }
                        .padding(horizontal = 13.dp, vertical = 6.dp)
                ) {
                    Text(
                        l.name,
                        color = if (active) Color(0xFF00210A) else DqdColors.TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        // 子榜单切换（足球支持：积分榜、射手榜、助攻榜）
        if (!isBasketball) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val subTabs = listOf("积分榜", "射手榜", "助攻榜")
                subTabs.forEachIndexed { i, label ->
                    val active = i == subTab
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (active) DqdColors.Green.copy(alpha = 0.16f) else DqdColors.SurfaceHigh)
                            .clickable { subTab = i }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            label,
                            color = if (active) DqdColors.Green else DqdColors.TextTertiary,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        when {
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DqdColors.Green, modifier = Modifier.size(28.dp))
                }
            }
            error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error!!, color = DqdColors.TextTertiary, fontSize = 13.sp)
                }
            }
            subTab == 0 || isBasketball -> {
                StandingTable(standings, isBasketball)
            }
            subTab == 1 -> {
                PlayerTable(scorers, "进球")
            }
            else -> {
                PlayerTable(assists, "助攻")
            }
        }
    }
}

@Composable
private fun StandingTable(list: List<DqdStandingItem>, isBasketball: Boolean) {
    if (list.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无积分榜数据", color = DqdColors.TextTertiary, fontSize = 13.sp)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(DqdColors.Surface)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", color = DqdColors.TextTertiary, fontSize = 11.5.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                Text("球队", color = DqdColors.TextTertiary, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                Text(if (isBasketball) "场次" else "赛", color = DqdColors.TextTertiary, fontSize = 11.5.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                Text(if (isBasketball) "胜" else "胜", color = DqdColors.TextTertiary, fontSize = 11.5.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                Text(if (isBasketball) "近况" else "平", color = DqdColors.TextTertiary, fontSize = 11.5.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                Text(if (isBasketball) "负" else "负", color = DqdColors.TextTertiary, fontSize = 11.5.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                Text(if (isBasketball) "胜率" else "进/失", color = DqdColors.TextTertiary, fontSize = 11.5.sp, modifier = Modifier.width(42.dp), textAlign = TextAlign.Center)
                Text(if (isBasketball) "胜差" else "积分", color = DqdColors.Green, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
            }
        }

        itemsIndexed(list) { i, itm ->
            val rankNum = itm.rank.toIntOrNull() ?: (i + 1)
            val isTopZone = rankNum in 1..4

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (isTopZone && !isBasketball) DqdColors.Green.copy(alpha = 0.04f) else Color.Transparent)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    itm.rank,
                    color = if (isTopZone) DqdColors.Green else DqdColors.TextTertiary,
                    fontSize = 12.sp,
                    fontWeight = if (isTopZone) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.width(24.dp),
                    textAlign = TextAlign.Center
                )

                if (itm.teamLogo != null) {
                    AsyncImage(
                        model = itm.teamLogo,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 6.dp)
                    )
                }

                Text(
                    itm.teamName,
                    color = DqdColors.TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(itm.played, color = DqdColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                Text(itm.win, color = DqdColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                Text(itm.draw, color = DqdColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                Text(itm.loss, color = DqdColors.TextSecondary, fontSize = 12.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                Text(itm.goals, color = DqdColors.TextTertiary, fontSize = 11.5.sp, modifier = Modifier.width(42.dp), textAlign = TextAlign.Center)
                Text(
                    itm.points,
                    color = DqdColors.Green,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.Center
                )
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(0.6.dp)
                    .padding(horizontal = 14.dp)
                    .background(DqdColors.Line.copy(alpha = 0.4f))
            )
        }
    }
}

@Composable
private fun PlayerTable(list: List<DqdPlayerItem>, countLabel: String) {
    if (list.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("暂无球员统计数据", color = DqdColors.TextTertiary, fontSize = 13.sp)
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(DqdColors.Surface)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", color = DqdColors.TextTertiary, fontSize = 11.5.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                Text("球员", color = DqdColors.TextTertiary, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
                Text("球队", color = DqdColors.TextTertiary, fontSize = 11.5.sp, modifier = Modifier.weight(0.7f))
                Text(countLabel, color = DqdColors.Green, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
            }
        }

        itemsIndexed(list) { i, itm ->
            val rankNum = itm.rank.toIntOrNull() ?: (i + 1)
            val isTopThree = rankNum in 1..3

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    itm.rank,
                    color = if (isTopThree) DqdColors.Accent else DqdColors.TextTertiary,
                    fontSize = 12.5.sp,
                    fontWeight = if (isTopThree) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.width(26.dp),
                    textAlign = TextAlign.Center
                )

                if (itm.playerLogo != null) {
                    AsyncImage(
                        model = itm.playerLogo,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(DqdColors.SurfaceHigh)
                    )
                    Spacer(Modifier.width(8.dp))
                }

                Text(
                    itm.playerName,
                    color = DqdColors.TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    itm.teamName,
                    color = DqdColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(0.7f)
                )

                Row(
                    modifier = Modifier.width(44.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        itm.count,
                        color = DqdColors.Green,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (itm.subCount.isNotBlank()) {
                        Text(
                            itm.subCount,
                            color = DqdColors.TextTertiary,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(0.6.dp)
                    .padding(horizontal = 14.dp)
                    .background(DqdColors.Line.copy(alpha = 0.4f))
            )
        }
    }
}
