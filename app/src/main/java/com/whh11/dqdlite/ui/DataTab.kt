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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.whh11.dqdlite.data.DataMenuGroup
import com.whh11.dqdlite.data.DqdApi
import com.whh11.dqdlite.data.MenuLeague
import com.whh11.dqdlite.data.PlayerRankRow
import com.whh11.dqdlite.data.RankSubTab
import com.whh11.dqdlite.data.StandingRow
import kotlinx.coroutines.launch

/**
 * 懂球帝全官方数据中台（动态加载 data_menu，彻底淘汰 qiumibao 混血与五层叠罗汉胶囊）。
 * 支持全球联赛积分榜（含欧冠/降级区色带）与全维度球员榜（射手榜、助攻榜、黄牌、传球等）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataTab(
    scrollToTopTrigger: Int,
    onOpenTeam: (teamId: String, teamName: String) -> Unit,
    onOpenPlayer: (personId: String, playerName: String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var menuGroups by remember { mutableStateOf<List<DataMenuGroup>>(emptyList()) }
    var menuLoading by remember { mutableStateOf(true) }
    var menuError by remember { mutableStateOf<String?>(null) }

    var selectedGroupIdx by remember { mutableIntStateOf(0) }
    var selectedLeague by remember { mutableStateOf<MenuLeague?>(null) }
    var showLeaguePicker by remember { mutableStateOf(false) }

    // 0 = 积分榜, 1 = 球员榜
    var mainDimension by remember { mutableIntStateOf(0) }
    var standings by remember { mutableStateOf<List<StandingRow>>(emptyList()) }
    var rankSubTabs by remember { mutableStateOf<List<RankSubTab>>(emptyList()) }
    var selectedRankSubIdx by remember { mutableIntStateOf(0) }
    var playerRows by remember { mutableStateOf<List<PlayerRankRow>>(emptyList()) }

    var contentLoading by remember { mutableStateOf(false) }
    var contentError by remember { mutableStateOf<String?>(null) }

    suspend fun loadMenu() {
        menuLoading = true
        menuError = null
        runCatching { DqdApi.dataMenu() }
            .onSuccess { groups ->
                menuGroups = groups
                if (selectedLeague == null) {
                    selectedLeague = groups.firstOrNull()?.leagues?.firstOrNull()
                }
            }
            .onFailure { menuError = it.message ?: "无法加载赛事目录" }
        menuLoading = false
    }

    LaunchedEffect(Unit) { loadMenu() }
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0 && menuGroups.isEmpty()) loadMenu()
    }

    // 切换联赛或维度时加载数据
    LaunchedEffect(selectedLeague, mainDimension) {
        val lg = selectedLeague ?: return@LaunchedEffect
        contentLoading = true
        contentError = null
        if (mainDimension == 0) {
            runCatching { DqdApi.standings(lg.standingUrl) }
                .onSuccess { standings = it }
                .onFailure { contentError = it.message ?: "加载积分榜失败" }
        } else {
            runCatching { DqdApi.rankingSubTabs(lg.personRankUrl) }
                .onSuccess { tabs ->
                    rankSubTabs = tabs
                    selectedRankSubIdx = 0
                    val first = tabs.firstOrNull()
                    playerRows = if (first != null) {
                        runCatching { DqdApi.personRanking(first.url) }.getOrDefault(emptyList())
                    } else emptyList()
                }
                .onFailure { contentError = it.message ?: "加载球员榜失败" }
        }
        contentLoading = false
    }

    // 切换球员榜子维度（射手/助攻/黄牌...）
    LaunchedEffect(selectedRankSubIdx, rankSubTabs) {
        if (mainDimension != 1) return@LaunchedEffect
        val tab = rankSubTabs.getOrNull(selectedRankSubIdx) ?: return@LaunchedEffect
        contentLoading = true
        runCatching { DqdApi.personRanking(tab.url) }
            .onSuccess { playerRows = it }
            .onFailure { contentError = it.message ?: "加载榜单失败" }
        contentLoading = false
    }

    val hotLeagues = remember(menuGroups) {
        menuGroups.firstOrNull()?.leagues?.take(12).orEmpty()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(DqdColors.Bg)
    ) {
        // 顶栏 + 单行联赛快捷条 + 右侧「全部赛事」图标
        Column(
            Modifier
                .fillMaxWidth()
                .background(DqdColors.Card)
                .statusBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    selectedLeague?.let { "${it.label} ${it.seasonTitle}".trim() } ?: "数据",
                    color = DqdColors.TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = { showLeaguePicker = true }) {
                    Icon(Icons.Outlined.GridView, contentDescription = "全部赛事", tint = DqdColors.TextPrimary)
                }
            }

            if (hotLeagues.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    itemsIndexed(hotLeagues) { _, lg ->
                        val active = lg.id == selectedLeague?.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                selectedLeague = lg
                                mainDimension = 0
                            }
                        ) {
                            Text(
                                lg.label,
                                color = if (active) DqdColors.Primary else DqdColors.TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier
                                    .width(if (active) 16.dp else 0.dp)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(DqdColors.Primary)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = DqdColors.Divider)
        }

        // 维度切换条（积分榜 / 球员榜） + 球员榜子维度（射手/助攻/传球...）
        if (selectedLeague != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(DqdColors.Bg)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val hasPerson = selectedLeague!!.personRankUrl.isNotBlank()
                listOf("积分榜" to true, "球员榜（射手/助攻）" to hasPerson).forEachIndexed { idx, (label, visible) ->
                    if (visible) {
                        val active = mainDimension == idx
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (active) DqdColors.Primary else DqdColors.Card)
                                .clickable { mainDimension = idx }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                label,
                                color = if (active) Color.White else DqdColors.TextSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            if (mainDimension == 1 && rankSubTabs.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(rankSubTabs) { idx, sub ->
                        val active = idx == selectedRankSubIdx
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) DqdColors.PrimaryContainer else DqdColors.Card)
                                .clickable { selectedRankSubIdx = idx }
                                .padding(horizontal = 11.dp, vertical = 5.dp)
                        ) {
                            Text(
                                sub.name,
                                color = if (active) DqdColors.Primary else DqdColors.TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
        }

        // 主体数据列表
        Box(Modifier.fillMaxSize()) {
            when {
                menuLoading -> LoadingBox()
                menuError != null && menuGroups.isEmpty() -> ErrorRetryBox(menuError!!, onRetry = { scope.launch { loadMenu() } })
                contentLoading -> LoadingBox()
                contentError != null -> ErrorRetryBox(contentError!!, onRetry = {
                    val lg = selectedLeague
                    selectedLeague = null
                    selectedLeague = lg
                })
                mainDimension == 0 -> StandingsTable(standings, onOpenTeam)
                else -> PlayerRankingTable(
                    rows = playerRows,
                    statHeader = rankSubTabs.getOrNull(selectedRankSubIdx)?.name ?: "数据",
                    onClickPlayer = { p ->
                        if (p.playerId.isNotBlank()) onOpenPlayer(p.playerId, p.playerName)
                        else if (p.teamId.isNotBlank()) onOpenTeam(p.teamId, p.teamName)
                    }
                )
            }
        }
    }

    // 全部赛事网格选择弹窗（替代五层横向滚动栏）
    if (showLeaguePicker && menuGroups.isNotEmpty()) {
        ModalBottomSheet(
            onDismissRequest = { showLeaguePicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DqdColors.Card
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    "全部赛事",
                    color = DqdColors.TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(menuGroups) { idx, g ->
                        val active = idx == selectedGroupIdx
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (active) DqdColors.Primary else DqdColors.SurfaceHigh)
                                .clickable { selectedGroupIdx = idx }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                g.title,
                                color = if (active) Color.White else DqdColors.TextSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                val leagues = menuGroups.getOrNull(selectedGroupIdx)?.leagues.orEmpty()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.height(360.dp)
                ) {
                    items(leagues, key = { "${selectedGroupIdx}_${it.id}_${it.label}" }) { lg ->
                        val active = lg.id == selectedLeague?.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) DqdColors.PrimaryContainer else DqdColors.SurfaceHigh)
                                .clickable {
                                    selectedLeague = lg
                                    mainDimension = 0
                                    showLeaguePicker = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp)
                        ) {
                            if (!lg.logo.isNullOrBlank()) {
                                AsyncImage(
                                    model = lg.logo,
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(Modifier.height(5.dp))
                            }
                            Text(
                                lg.label,
                                color = if (active) DqdColors.Primary else DqdColors.TextPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StandingsTable(
    rows: List<StandingRow>,
    onClickTeam: (teamId: String, teamName: String) -> Unit
) {
    if (rows.isEmpty()) {
        EmptyBox("该赛事暂无积分榜数据")
        return
    }
    val grouped = remember(rows) { rows.groupBy { it.groupTitle } }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        grouped.forEach { (group, list) ->
            if (group.isNotBlank()) {
                item(key = "grp_$group") {
                    Text(
                        group,
                        color = DqdColors.Primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            item(key = "hdr_$group") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(DqdColors.Card)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#", color = DqdColors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                    Text("球队", color = DqdColors.TextTertiary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("赛", color = DqdColors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                    Text("胜/平/负", color = DqdColors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(62.dp), textAlign = TextAlign.Center)
                    Text("进/失", color = DqdColors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
                    Text("积分", color = DqdColors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(34.dp), textAlign = TextAlign.End)
                }
                HorizontalDivider(thickness = 0.5.dp, color = DqdColors.Divider)
            }
            itemsIndexed(list, key = { idx, r -> "${group}_${r.teamId}_${r.rank}_$idx" }) { _, row ->
                val zoneColor = remember(row.zoneColor) {
                    row.zoneColor?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(DqdColors.Card)
                        .clickable { onClickTeam(row.teamId, row.teamName) }
                        .padding(end = 14.dp, top = 9.dp, bottom = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(22.dp)
                            .background(zoneColor ?: Color.Transparent, RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                    )
                    Spacer(Modifier.width(11.dp))
                    Column(
                        Modifier.width(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            row.rank.toString(),
                            color = zoneColor ?: DqdColors.TextSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = if (row.rank <= 4) FontWeight.Bold else FontWeight.Normal
                        )
                        row.rankDelta?.let { d ->
                            if (d != 0) {
                                Text(
                                    if (d > 0) "▲$d" else "▼${-d}",
                                    color = if (d > 0) DqdColors.Primary else DqdColors.Red,
                                    fontSize = 7.5.sp,
                                    lineHeight = 9.sp
                                )
                            }
                        }
                    }
                    Row(
                        Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TeamCrest(row.teamLogo, 20)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            row.teamName,
                            color = DqdColors.TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(row.played, color = DqdColors.TextSecondary, fontSize = 11.5.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                    Text("${row.win}/${row.draw}/${row.loss}", color = DqdColors.TextSecondary, fontSize = 11.5.sp, modifier = Modifier.width(62.dp), textAlign = TextAlign.Center)
                    Text("${row.goalsFor}/${row.goalsAgainst}", color = DqdColors.TextSecondary, fontSize = 11.5.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
                    Text(row.points, color = DqdColors.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(34.dp), textAlign = TextAlign.End)
                }
                HorizontalDivider(
                    Modifier
                        .background(DqdColors.Card)
                        .padding(horizontal = 14.dp),
                    thickness = 0.5.dp,
                    color = DqdColors.Divider
                )
            }
        }
    }
}

@Composable
private fun PlayerRankingTable(
    rows: List<PlayerRankRow>,
    statHeader: String,
    onClickPlayer: (PlayerRankRow) -> Unit
) {
    if (rows.isEmpty()) {
        EmptyBox("暂无该维度球员榜数据")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item(key = "p_hdr") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(DqdColors.Card)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", color = DqdColors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                Text("球员", color = DqdColors.TextTertiary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("球队", color = DqdColors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(92.dp))
                Text(statHeader, color = DqdColors.TextTertiary, fontSize = 11.sp, modifier = Modifier.width(46.dp), textAlign = TextAlign.End)
            }
            HorizontalDivider(thickness = 0.5.dp, color = DqdColors.Divider)
        }
        itemsIndexed(rows, key = { idx, p -> "${p.playerId}_${p.rank}_$idx" }) { _, p ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(DqdColors.Card)
                    .clickable { onClickPlayer(p) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    p.rank.toString(),
                    color = if (p.rank <= 3) DqdColors.Primary else DqdColors.TextSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = if (p.rank <= 3) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.width(26.dp),
                    textAlign = TextAlign.Center
                )
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    if (!p.playerLogo.isNullOrBlank()) {
                        AsyncImage(
                            model = p.playerLogo,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(DqdColors.SurfaceHigh)
                        )
                        Spacer(Modifier.width(7.dp))
                    }
                    Text(
                        p.playerName,
                        color = DqdColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    p.teamName,
                    color = DqdColors.TextSecondary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(92.dp)
                )
                Text(
                    p.count,
                    color = DqdColors.Primary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(46.dp),
                    textAlign = TextAlign.End
                )
            }
            HorizontalDivider(
                Modifier
                    .background(DqdColors.Card)
                    .padding(horizontal = 14.dp),
                thickness = 0.5.dp,
                color = DqdColors.Divider
            )
        }
    }
}
