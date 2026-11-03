package com.whh11.dqdlite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import com.whh11.dqdlite.data.Match
import com.whh11.dqdlite.data.SportCategory

@Composable
fun MatchTab(reload: Int, onOpen: (String, String) -> Unit) {
    val categories = SportCategory.values()
    var selectedCatIndex by rememberSaveable { mutableIntStateOf(0) }
    val currentCat = categories[selectedCatIndex]

    var matches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedCatIndex, reload) {
        loading = true
        error = null
        runCatching { DqdApi.matches(currentCat) }
            .onSuccess { matches = it }
            .onFailure { error = it.message ?: "网络异常" }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        // 顶部项目赛程选择器
        LazyRow(
            Modifier
                .fillMaxWidth()
                .background(DqdColors.Card)
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(categories) { index, cat ->
                val active = index == selectedCatIndex
                Box(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) DqdColors.Primary else DqdColors.Border)
                        .clickable { selectedCatIndex = index }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        cat.label,
                        color = if (active) Color.White else DqdColors.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Box(Modifier.fillMaxSize()) {
            when {
                loading && matches.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DqdColors.Primary, modifier = Modifier.size(32.dp))
                    }
                }
                error != null && matches.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error ?: "加载失败", color = DqdColors.TextSecondary, fontSize = 14.sp)
                    }
                }
                matches.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("${currentCat.label} 暂时没有比赛", color = DqdColors.TextSecondary, fontSize = 14.sp)
                    }
                }
                else -> {
                    val grouped = matches.groupBy { it.competition }
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        grouped.forEach { (compName, list) ->
                            item(key = "h_$compName") {
                                CompetitionHeader(compName, list.firstOrNull()?.competitionColor)
                            }
                            itemsIndexed(list, key = { _, m -> m.id }) { _, match ->
                                MatchRow(match = match, onClick = {
                                    onOpen(match.detailUrl, "${match.teamA} vs ${match.teamB}")
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompetitionHeader(name: String, colorHex: String?) {
    val barColor = colorHex?.let {
        runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
    } ?: DqdColors.Primary

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 3.dp, height = 12.dp)
                .background(barColor, RoundedCornerShape(1.dp))
        )
        Spacer(Modifier.width(6.dp))
        Text(
            name,
            color = DqdColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MatchRow(match: Match, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DqdColors.Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
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
                fontSize = 13.sp,
                fontWeight = if (match.hasScore && match.scoreA > match.scoreB) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
            Spacer(Modifier.width(6.dp))
            TeamLogo(match.logoA)
        }

        // 中间比分 / 时间
        Column(
            Modifier.width(80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                match.centreLabel,
                color = when {
                    match.isLive -> DqdColors.Red
                    match.hasScore -> DqdColors.TextPrimary
                    else -> DqdColors.TextSecondary
                },
                fontSize = if (match.hasScore) 15.sp else 12.sp,
                fontWeight = if (match.hasScore) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                match.stateLabel,
                color = if (match.isLive) DqdColors.Red else DqdColors.TextSecondary,
                fontSize = 10.sp
            )
        }

        // 客队
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TeamLogo(match.logoB)
            Spacer(Modifier.width(6.dp))
            Text(
                match.teamB,
                color = DqdColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (match.hasScore && match.scoreB > match.scoreA) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TeamLogo(url: String?) {
    if (url.isNullOrBlank()) {
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(DqdColors.Border)
        )
    } else {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(20.dp)
        )
    }
}
