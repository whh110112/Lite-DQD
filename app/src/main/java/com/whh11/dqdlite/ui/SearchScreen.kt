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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.whh11.dqdlite.data.DqdApi
import com.whh11.dqdlite.data.FollowedTeam
import com.whh11.dqdlite.data.LocalStore
import com.whh11.dqdlite.data.News
import com.whh11.dqdlite.data.SearchResult
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(
    initialQuery: String = "",
    onBack: () -> Unit,
    onOpenArticle: (News) -> Unit
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf(initialQuery) }
    var result by remember { mutableStateOf<SearchResult?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val followed by LocalStore.followedTeams.collectAsState()

    fun doSearch(q: String) {
        val trimmed = q.trim()
        if (trimmed.isBlank()) return
        loading = true
        error = null
        scope.launch {
            runCatching { DqdApi.search(trimmed) }
                .onSuccess { result = it }
                .onFailure { error = it.message ?: "搜索失败" }
            loading = false
        }
    }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) doSearch(initialQuery)
    }

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
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = DqdColors.TextPrimary)
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(DqdColors.SurfaceHigh)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text("搜索球队、球员或资讯…", color = DqdColors.TextTertiary, fontSize = 14.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = DqdColors.TextPrimary, fontSize = 14.sp),
                    cursorBrush = SolidColor(DqdColors.Primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { doSearch(query) }),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { query = ""; result = null }) {
                    Icon(Icons.Default.Close, contentDescription = "清除", tint = DqdColors.TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
            Text(
                "搜索",
                color = DqdColors.Primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { doSearch(query) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = DqdColors.Divider)

        when {
            loading -> LoadingBox()
            error != null -> ErrorRetryBox(error!!, onRetry = { doSearch(query) })
            result == null -> {
                val hotWords = listOf("皇马", "巴萨", "阿森纳", "曼城", "利物浦", "拜仁", "国足", "梅西", "C罗")
                Column(Modifier.padding(16.dp)) {
                    Text("热门搜索", color = DqdColors.TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        hotWords.take(5).forEach { w ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DqdColors.Card)
                                    .clickable { query = w; doSearch(w) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(w, color = DqdColors.TextPrimary, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        hotWords.drop(5).forEach { w ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DqdColors.Card)
                                    .clickable { query = w; doSearch(w) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(w, color = DqdColors.TextPrimary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            else -> {
                val r = result!!
                if (r.teams.isEmpty() && r.players.isEmpty() && r.news.isEmpty()) {
                    EmptyBox("没有找到与「$query」相关的内容")
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        if (r.teams.isNotEmpty()) {
                            item(key = "s_teams") {
                                Text("球队", color = DqdColors.TextSecondary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(14.dp))
                            }
                            items(r.teams, key = { "st_${it.teamId}_${it.name}" }) { t ->
                                val isFollowed = followed.any { it.teamId == t.teamId || it.name == t.name }
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(DqdColors.Card)
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TeamCrest(t.logo, 32)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(t.name, color = DqdColors.TextPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
                                        if (t.enName.isNotBlank()) {
                                            Text(t.enName, color = DqdColors.TextTertiary, fontSize = 11.5.sp)
                                        }
                                    }
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isFollowed) DqdColors.SurfaceHigh else DqdColors.Primary)
                                            .clickable {
                                                LocalStore.toggleFollowTeam(FollowedTeam(t.teamId, t.name, t.logo))
                                            }
                                            .padding(horizontal = 12.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            if (isFollowed) "已关注" else "+ 关注",
                                            color = if (isFollowed) DqdColors.TextSecondary else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = DqdColors.Divider)
                            }
                        }

                        if (r.players.isNotEmpty()) {
                            item(key = "s_players") {
                                Text("球员", color = DqdColors.TextSecondary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(14.dp))
                            }
                            items(r.players, key = { "sp_${it.playerId}_${it.name}" }) { p ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(DqdColors.Card)
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!p.logo.isNullOrBlank()) {
                                        AsyncImage(
                                            model = p.logo,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(DqdColors.SurfaceHigh)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                    }
                                    Column {
                                        Text(p.name, color = DqdColors.TextPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                                        if (p.teamName.isNotBlank()) {
                                            Text(p.teamName, color = DqdColors.TextTertiary, fontSize = 12.sp)
                                        }
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = DqdColors.Divider)
                            }
                        }

                        if (r.news.isNotEmpty()) {
                            item(key = "s_news") {
                                Text("相关资讯", color = DqdColors.TextSecondary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(14.dp))
                            }
                            items(r.news, key = { "sn_${it.id}" }) { n ->
                                NewsRowItem(news = n, onClick = { onOpenArticle(n) })
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
    }
}
