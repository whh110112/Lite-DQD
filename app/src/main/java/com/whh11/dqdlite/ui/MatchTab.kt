package com.whh11.dqdlite.ui

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.whh11.dqdlite.data.Match
import com.whh11.dqdlite.data.SportCategory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MatchTab(
    scrollToTopTrigger: Int,
    onOpenMatch: (String) -> Unit
) {
    val categories = SportCategory.entries
    var selectedCatIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentCat = categories[selectedCatIndex]

    var matches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    suspend fun load(isRefresh: Boolean = false) {
        if (isRefresh) refreshing = true else loading = true
        error = null
        runCatching { DqdApi.matches(currentCat) }
            .onSuccess { matches = it }
            .onFailure { error = it.message ?: "赛程加载失败" }
        loading = false
        refreshing = false
    }

    LaunchedEffect(selectedCatIndex) { load() }

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            listState.animateScrollToItem(0)
            load(isRefresh = true)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(DqdColors.Bg)
    ) {
        // 顶栏 + 项目切换
        Column(
            Modifier
                .fillMaxWidth()
                .background(DqdColors.Card)
                .statusBarsPadding()
        ) {
            Text(
                "比赛",
                color = DqdColors.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 6.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                itemsIndexed(categories) { idx, cat ->
                    val active = idx == selectedCatIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { selectedCatIndex = idx }
                    ) {
                        Text(
                            cat.label,
                            color = if (active) DqdColors.Primary else DqdColors.TextSecondary,
                            fontSize = 14.5.sp,
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
            HorizontalDivider(thickness = 0.5.dp, color = DqdColors.Divider)
        }

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { scope.launch { load(isRefresh = true) } },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                loading && matches.isEmpty() -> LoadingBox()
                error != null && matches.isEmpty() -> ErrorRetryBox(error!!, onRetry = { scope.launch { load() } })
                matches.isEmpty() -> EmptyBox("${currentCat.label} 暂时没有比赛")
                else -> {
                    val grouped = remember(matches) { matches.groupBy { it.dateGroup } }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        grouped.forEach { (dateStr, list) ->
                            stickyHeader(key = "date_$dateStr") {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(DqdColors.Bg)
                                        .padding(horizontal = 16.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        dateStr,
                                        color = DqdColors.TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("${list.size} 场", color = DqdColors.TextTertiary, fontSize = 11.sp)
                                }
                            }
                            itemsIndexed(list, key = { i, m -> "${m.id}_$i" }) { i, match ->
                                DqdMatchRow(match = match, onClick = { onOpenMatch(match.id) })
                                if (i != list.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier
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
        }
    }
}

@Composable
fun DqdMatchRow(match: Match, onClick: () -> Unit) {
    val compColor = remember(match.competitionColor) {
        match.competitionColor?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(DqdColors.Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(width = 3.dp, height = 11.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(compColor ?: DqdColors.Primary)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                match.competition,
                color = DqdColors.TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                match.timeLabel,
                color = DqdColors.TextTertiary,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 主队
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    match.teamA,
                    color = DqdColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (match.teamAWon) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
                Spacer(Modifier.width(8.dp))
                TeamCrest(match.logoA, 22)
            }

            // 中间比分与状态（已修复数字比较）
            Column(
                Modifier.width(86.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    match.centreLabel,
                    color = when {
                        match.isLive -> DqdColors.Red
                        match.hasScore -> DqdColors.TextPrimary
                        else -> DqdColors.TextSecondary
                    },
                    fontSize = if (match.hasScore) 17.sp else 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    match.stateLabel,
                    color = if (match.isLive) DqdColors.Red else DqdColors.TextTertiary,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }

            // 客队
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamCrest(match.logoB, 22)
                Spacer(Modifier.width(8.dp))
                Text(
                    match.teamB,
                    color = DqdColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (match.teamBWon) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun TeamCrest(url: String?, sizeDp: Int) {
    if (url.isNullOrBlank()) {
        Box(
            Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .background(DqdColors.SurfaceHigh)
        )
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(sizeDp.dp)
        )
    }
}
