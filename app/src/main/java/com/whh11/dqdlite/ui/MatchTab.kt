package com.whh11.dqdlite.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.whh11.dqdlite.data.DqdApi
import com.whh11.dqdlite.data.Match
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MatchTab(reload: Int, onOpen: (String, String) -> Unit) {
    var matches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reload) {
        loading = true
        error = null
        runCatching { DqdApi.matches() }
            .onSuccess { matches = it }
            .onFailure { error = it.message ?: "网络异常" }
        loading = false
    }

    when {
        loading && matches.isEmpty() -> LoadingBlock()
        error != null && matches.isEmpty() -> ErrorBlock(error!!)
        matches.isEmpty() -> EmptyBlock("今天暂时没有比赛")
        else -> {
            val grouped = matches.groupBy { it.competition }
            LazyColumn(
                contentPadding = PaddingValues(top = 6.dp, bottom = 28.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item { DateStrip(matches) }
                grouped.forEach { (competition, list) ->
                    item(key = "h-$competition") {
                        CompetitionHeader(competition, list.first().competitionColor, list.size)
                    }
                    items(list.size, key = { idx -> list[idx].id }) { idx ->
                        val m = list[idx]
                        MatchCard(m) { onOpen(m.detailUrl, "${m.teamA} vs ${m.teamB}") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateStrip(matches: List<Match>) {
    val today = remember {
        SimpleDateFormat("M月d日", Locale.CHINA).format(Date())
    }
    val live = matches.count { it.isLive }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("今日赛程", color = DqdColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Text(today, color = DqdColors.TextTertiary, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        if (live > 0) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(DqdColors.Green)
            )
            Spacer(Modifier.width(5.dp))
            Text("$live 场进行中", color = DqdColors.Green, fontSize = 11.sp)
        } else {
            Text("共 ${matches.size} 场", color = DqdColors.TextTertiary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CompetitionHeader(name: String, color: String?, count: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(13.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(parseColor(color) ?: DqdColors.Green)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            name,
            color = DqdColors.TextSecondary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.width(6.dp))
        Text("$count", color = DqdColors.TextTertiary, fontSize = 11.sp)
    }
}

@Composable
private fun MatchCard(m: Match, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(DqdColors.Surface)
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (m.isLive) {
                    Box(
                        Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(DqdColors.Green)
                    )
                    Spacer(Modifier.width(5.dp))
                }
                Text(
                    m.stateLabel,
                    color = when {
                        m.isLive -> DqdColors.Green
                        m.isPlayed -> DqdColors.TextTertiary
                        else -> DqdColors.TextSecondary
                    },
                    fontSize = 10.5.sp,
                    fontWeight = if (m.isLive) FontWeight.Medium else FontWeight.Normal
                )
                Spacer(Modifier.weight(1f))
                if (!m.hasScore) {
                    Text(m.timeUtc, color = DqdColors.TextTertiary, fontSize = 10.5.sp)
                }
            }

            Spacer(Modifier.height(9.dp))

            TeamRow(m.teamA, m.logoA, if (m.hasScore) m.scoreA else "", m.hasScore, m.isPlayed, m.scoreA, m.scoreB)
            Spacer(Modifier.height(7.dp))
            TeamRow(m.teamB, m.logoB, if (m.hasScore) m.scoreB else "", m.hasScore, m.isPlayed, m.scoreA, m.scoreB)
        }
    }
}

@Composable
private fun TeamRow(
    name: String,
    logo: String?,
    score: String,
    hasScore: Boolean,
    played: Boolean,
    scoreA: String,
    scoreB: String
) {
    val winner = played && hasScore && run {
        val a = scoreA.toIntOrNull() ?: return@run false
        val b = scoreB.toIntOrNull() ?: return@run false
        (score == scoreA && a > b) || (score == scoreB && b > a)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (logo != null) {
            AsyncImage(
                model = logo,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(DqdColors.SurfaceHigh)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            name,
            color = if (winner) DqdColors.TextPrimary else DqdColors.TextPrimary.copy(alpha = 0.84f),
            fontSize = 14.sp,
            fontWeight = if (winner) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (hasScore) {
            Text(
                score,
                color = if (winner) DqdColors.Green else DqdColors.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun parseColor(hex: String?): Color? {
    if (hex.isNullOrBlank() || !hex.startsWith("#")) return null
    return try {
        val v = hex.removePrefix("#")
        when (v.length) {
            6 -> Color(0xFF000000.toInt() or v.toLong(16).toInt())
            8 -> Color(v.toLong(16).toInt())
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}
