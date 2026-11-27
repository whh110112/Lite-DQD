package com.whh11.dqdlite.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.whh11.dqdlite.R
import com.whh11.dqdlite.data.DqdApi
import com.whh11.dqdlite.data.Match
import com.whh11.dqdlite.data.News
import com.whh11.dqdlite.data.SportCategory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsHomeTab(
    scrollToTopTrigger: Int,
    onOpenArticle: (News) -> Unit,
    onOpenMatch: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val tabs = DqdApi.TABS
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var items by remember { mutableStateOf<List<News>>(emptyList()) }
    var nextUrl by remember { mutableStateOf<String?>(null) }
    var focusMatches by remember { mutableStateOf<List<Match>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val headerVisible by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 120
        }
    }

    suspend fun loadInitial(isRefresh: Boolean = false) {
        if (isRefresh) refreshing = true else loading = true
        error = null
        runCatching { DqdApi.news(tabs[tabIndex].id) }
            .onSuccess { page ->
                items = page.items
                nextUrl = page.nextUrl
            }
            .onFailure { error = it.message ?: "网络连接异常" }
        if (tabIndex == 0 && (focusMatches.isEmpty() || isRefresh)) {
            runCatching { DqdApi.matches(SportCategory.ALL) }
                .onSuccess { list ->
                    focusMatches = list.sortedByDescending { if (it.isLive) 2 else if (!it.isPlayed) 1 else 0 }.take(8)
                }
        }
        loading = false
        refreshing = false
    }

    suspend fun loadMore() {
        val next = nextUrl ?: return
        if (loadingMore) return
        loadingMore = true
        runCatching { DqdApi.news(tabs[tabIndex].id, nextUrl = next) }
            .onSuccess { page ->
                val seen = items.map { it.id }.toHashSet()
                val appended = page.items.filter { it.id == 0L || it.id !in seen }
                items = items + appended
                nextUrl = if (appended.isEmpty()) null else page.nextUrl
            }
        loadingMore = false
    }

    LaunchedEffect(tabIndex) {
        items = emptyList()
        nextUrl = null
        loadInitial(isRefresh = false)
    }

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            listState.animateScrollToItem(0)
            loadInitial(isRefresh = true)
        }
    }

    // 触底自动加载下一页
    val shouldLoadMore by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 5 && lastVisible >= total - 4 && nextUrl != null && !loadingMore
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) loadMore()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(DqdColors.Bg)
    ) {
        // 顶部品牌栏：随列表下滑平滑收起，释放垂直空间
        AnimatedVisibility(
            visible = headerVisible,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(DqdColors.Card)
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_brand),
                    contentDescription = "懂球帝",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(7.dp))
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    "懂球帝",
                    color = DqdColors.TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onOpenSearch) {
                    Icon(Icons.Outlined.Search, contentDescription = "搜索", tint = DqdColors.TextPrimary)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Person, contentDescription = "我的与设置", tint = DqdColors.TextPrimary)
                }
            }
        }

        // 频道下划线 Tab 栏（白底 + 绿色短横杠，回归懂球帝原版清爽感）
        Column(
            Modifier
                .fillMaxWidth()
                .background(DqdColors.Card)
                .then(if (!headerVisible) Modifier.statusBarsPadding() else Modifier)
        ) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                itemsIndexed(tabs) { idx, t ->
                    val active = idx == tabIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { tabIndex = idx }
                            .padding(top = 8.dp, bottom = 6.dp)
                    ) {
                        Text(
                            t.label,
                            color = if (active) DqdColors.Primary else DqdColors.TextSecondary,
                            fontSize = if (active) 15.5.sp else 14.5.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(Modifier.height(5.dp))
                        Box(
                            Modifier
                                .width(if (active) 18.dp else 0.dp)
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
            onRefresh = { scope.launch { loadInitial(isRefresh = true) } },
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                loading && items.isEmpty() -> LoadingBox()
                error != null && items.isEmpty() -> ErrorRetryBox(error!!, onRetry = { scope.launch { loadInitial() } })
                items.isEmpty() -> EmptyBox("该栏目暂无资讯")
                else -> {
                    val bannerItems = remember(items, tabIndex) {
                        if (tabIndex == 0) items.filter { !it.thumb.isNullOrBlank() }.take(4) else emptyList()
                    }
                    val bannerIds = remember(bannerItems) { bannerItems.map { it.id }.toHashSet() }
                    val feedItems = remember(items, bannerIds) {
                        if (bannerIds.isEmpty()) items else items.filter { it.id !in bannerIds }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        // 1. 头条焦点大图轮播
                        if (bannerItems.isNotEmpty()) {
                            item(key = "hero_banner") {
                                HeroBannerCarousel(bannerItems, onOpenArticle)
                            }
                        }

                        // 2. 今日焦点战实时横卡（参考 hupuX & 懂球帝头条）
                        if (tabIndex == 0 && focusMatches.isNotEmpty()) {
                            item(key = "score_strip") {
                                FocusMatchStrip(focusMatches, onOpenMatch)
                            }
                        }

                        // 3. 高密度懂球帝原生资讯流
                        itemsIndexed(feedItems, key = { i, n -> "${n.id}_$i" }) { i, n ->
                            NewsRowItem(news = n, onClick = { onOpenArticle(n) })
                            if (i != feedItems.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier
                                        .background(DqdColors.Card)
                                        .padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = DqdColors.Divider
                                )
                            }
                        }

                        // 4. 分页加载底部指示器
                        item(key = "footer") {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    loadingMore -> CircularProgressIndicator(
                                        color = DqdColors.Primary,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    nextUrl == null -> Text("已经到底了", color = DqdColors.TextTertiary, fontSize = 12.sp)
                                    else -> Text("上滑加载更多", color = DqdColors.TextTertiary, fontSize = 12.sp)
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
private fun HeroBannerCarousel(
    banners: List<News>,
    onOpenArticle: (News) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { banners.size })
    Box(
        Modifier
            .fillMaxWidth()
            .background(DqdColors.Card)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(12.dp))
        ) { page ->
            val item = banners[page]
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable { onOpenArticle(item) }
            ) {
                AsyncImage(
                    model = item.thumb,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f)),
                                startY = 120f
                            )
                        )
                )
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                ) {
                    Text(
                        item.title,
                        color = Color.White,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 21.sp
                    )
                }
            }
        }

        // 右下角指示点
        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(banners.size) { idx ->
                Box(
                    Modifier
                        .size(width = if (idx == pagerState.currentPage) 14.dp else 5.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(if (idx == pagerState.currentPage) DqdColors.BrandGreen else Color.White.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
private fun FocusMatchStrip(
    matches: List<Match>,
    onOpenMatch: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(DqdColors.Bg)
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(matches, key = { it.id }) { m ->
            Column(
                Modifier
                    .width(156.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DqdColors.Card)
                    .clickable { onOpenMatch(m.id) }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        m.competition,
                        color = DqdColors.TextTertiary,
                        fontSize = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (m.isLive) m.period.ifBlank { "直播中" } else if (m.isPlayed) "完场" else m.timeLabel,
                        color = if (m.isLive) DqdColors.Red else DqdColors.TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = if (m.isLive) FontWeight.Bold else FontWeight.Normal
                    )
                }
                Spacer(Modifier.height(6.dp))
                MiniTeamScoreLine(m.logoA, m.teamA, if (m.hasScore) m.scoreA else "-", m.teamAWon)
                Spacer(Modifier.height(4.dp))
                MiniTeamScoreLine(m.logoB, m.teamB, if (m.hasScore) m.scoreB else "-", m.teamBWon)
            }
        }
    }
}

@Composable
private fun MiniTeamScoreLine(logo: String?, name: String, score: String, bold: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (!logo.isNullOrBlank()) {
            AsyncImage(
                model = logo,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            name,
            color = DqdColors.TextPrimary,
            fontSize = 12.5.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            score,
            color = if (bold) DqdColors.TextPrimary else DqdColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
