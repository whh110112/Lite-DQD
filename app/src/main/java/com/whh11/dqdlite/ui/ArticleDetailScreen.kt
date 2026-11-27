package com.whh11.dqdlite.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.whh11.dqdlite.data.ArticleDetail
import com.whh11.dqdlite.data.CommentItem
import com.whh11.dqdlite.data.ContentBlock
import com.whh11.dqdlite.data.DqdApi
import com.whh11.dqdlite.data.LocalStore
import com.whh11.dqdlite.data.SavedArticle
import kotlinx.coroutines.launch

/**
 * 纯原生文章详情页 + 原生热评/全量评论流 + 原生全屏缩放存图查看器。
 * 彻底告别 WebView .tt-fold 截断和全局 CSS 暴力染色的割裂体验。
 */
@Composable
fun ArticleDetailScreen(
    articleId: Long,
    fallbackTitle: String,
    onBack: () -> Unit,
    onOpenWeb: (url: String, title: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gifLoader = rememberGifImageLoader()

    var article by remember { mutableStateOf<ArticleDetail?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var hotComments by remember { mutableStateOf<List<CommentItem>>(emptyList()) }
    var comments by remember { mutableStateOf<List<CommentItem>>(emptyList()) }
    var commentTotal by remember { mutableStateOf(0) }
    var commentNextUrl by remember { mutableStateOf<String?>(null) }
    var commentsLoading by remember { mutableStateOf(true) }
    var commentsError by remember { mutableStateOf<String?>(null) }
    var loadingMoreComments by remember { mutableStateOf(false) }

    var viewerImageUrl by remember { mutableStateOf<String?>(null) }

    val favorites by LocalStore.favorites.collectAsState()
    val isFav = remember(favorites, articleId) { favorites.any { it.id == articleId } }

    suspend fun loadCommentsOnly() {
        commentsLoading = true
        commentsError = null
        runCatching { DqdApi.comments(articleId) }
            .onSuccess { page ->
                hotComments = page.hotComments
                comments = page.comments
                commentTotal = page.total
                commentNextUrl = page.nextUrl
            }
            .onFailure {
                commentsError = it.message ?: "评论加载失败"
            }
        commentsLoading = false
    }

    suspend fun loadAll() {
        loading = true
        error = null
        val detailJob = scope.launch {
            runCatching { DqdApi.articleDetail(articleId) }
                .onSuccess { detail ->
                    article = detail
                    LocalStore.recordHistory(
                        SavedArticle(
                            id = detail.id,
                            title = detail.title.ifBlank { fallbackTitle },
                            thumb = detail.thumb,
                            author = detail.writer
                        )
                    )
                }
                .onFailure { error = it.message ?: "正文加载失败" }
            loading = false
        }
        val commentsJob = scope.launch { loadCommentsOnly() }
        detailJob.join()
        commentsJob.join()
    }

    LaunchedEffect(articleId) { loadAll() }

    Column(
        Modifier
            .fillMaxSize()
            .background(DqdColors.Card)
    ) {
        // 顶部简洁原生导航栏
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
                article?.writer?.ifBlank { "正文" } ?: "正文",
                color = DqdColors.TextPrimary,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                val a = article
                val saved = LocalStore.toggleFavorite(
                    SavedArticle(
                        id = articleId,
                        title = a?.title?.ifBlank { fallbackTitle } ?: fallbackTitle,
                        thumb = a?.thumb,
                        author = a?.writer.orEmpty()
                    )
                )
                Toast.makeText(context, if (saved) "已加入本地收藏" else "已取消收藏", Toast.LENGTH_SHORT).show()
            }) {
                Icon(
                    if (isFav) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "收藏",
                    tint = if (isFav) DqdColors.Primary else DqdColors.TextSecondary
                )
            }
            IconButton(onClick = {
                val shareTitle = article?.title?.ifBlank { fallbackTitle } ?: fallbackTitle
                val shareUrl = "https://m.dongqiudi.com/article/$articleId.html"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "$shareTitle\n$shareUrl")
                }
                context.startActivity(Intent.createChooser(intent, "分享文章"))
            }) {
                Icon(Icons.Outlined.Share, contentDescription = "分享", tint = DqdColors.TextSecondary)
            }
            IconButton(onClick = {
                val webUrl = "https://m.dongqiudi.com/article/$articleId.html"
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))) }
            }) {
                Icon(Icons.Outlined.OpenInBrowser, contentDescription = "原网页", tint = DqdColors.TextSecondary)
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = DqdColors.Divider)

        when {
            loading -> LoadingBox()
            error != null && article == null -> ErrorRetryBox(
                message = error!!,
                onRetry = { scope.launch { loadAll() } }
            )
            else -> {
                val detail = article!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // 标题与作者时间信息
                    item(key = "header") {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Text(
                                text = detail.title.ifBlank { fallbackTitle },
                                color = DqdColors.TextPrimary,
                                fontSize = 21.sp,
                                lineHeight = 29.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (detail.writer.isNotBlank()) {
                                    Text(
                                        detail.writer,
                                        color = DqdColors.Primary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.width(10.dp))
                                }
                                Text(detail.time, color = DqdColors.TextTertiary, fontSize = 12.5.sp)
                            }
                        }
                    }

                    // 视频播放入口（如有）
                    if (!detail.videoUrl.isNullOrBlank()) {
                        item(key = "video_banner") {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DqdColors.PrimaryContainer)
                                    .clickable { onOpenWeb(detail.webUrl, detail.title) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.PlayCircle,
                                        contentDescription = null,
                                        tint = DqdColors.Primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            "本文包含视频内容，点击播放",
                                            color = DqdColors.Primary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "支持全屏横屏播放",
                                            color = DqdColors.TextSecondary,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 正文原生块列表
                    items(detail.blocks.size) { idx ->
                        when (val block = detail.blocks[idx]) {
                            is ContentBlock.Heading -> Text(
                                text = block.text,
                                color = DqdColors.TextPrimary,
                                fontSize = 17.5.sp,
                                lineHeight = 25.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                            is ContentBlock.Paragraph -> Text(
                                text = block.text,
                                color = DqdColors.TextPrimary,
                                fontSize = 16.sp,
                                lineHeight = 27.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                            )
                            is ContentBlock.Quote -> Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DqdColors.SurfaceHigh)
                                    .padding(12.dp)
                            ) {
                                Box(
                                    Modifier
                                        .width(3.dp)
                                        .height(22.dp)
                                        .background(DqdColors.Primary, RoundedCornerShape(2.dp))
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = block.text,
                                    color = DqdColors.TextSecondary,
                                    fontSize = 14.5.sp,
                                    lineHeight = 22.sp
                                )
                            }
                            is ContentBlock.Image -> Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                AsyncImage(
                                    model = block.url,
                                    imageLoader = gifLoader,
                                    contentDescription = block.caption,
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DqdColors.SurfaceHigh)
                                        .clickable { viewerImageUrl = block.url }
                                )
                                if (block.caption.isNotBlank()) {
                                    Text(
                                        block.caption,
                                        color = DqdColors.TextTertiary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 正文与评论分割带
                    item(key = "comment_divider") {
                        Spacer(Modifier.height(14.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(DqdColors.Bg)
                        )
                    }

                    // 精彩热评区
                    if (hotComments.isNotEmpty()) {
                        item(key = "hot_title") {
                            CommentSectionHeader("精彩热评", hotComments.size, accent = DqdColors.Red)
                        }
                        items(hotComments, key = { "hot_${it.id}" }) { c ->
                            CommentCardRow(c)
                            HorizontalDivider(
                                Modifier.padding(start = 54.dp, end = 16.dp),
                                thickness = 0.5.dp,
                                color = DqdColors.Divider
                            )
                        }
                    }

                    // 全部评论区
                    item(key = "all_title") {
                        CommentSectionHeader("全部评论", commentTotal, accent = DqdColors.Primary)
                    }

                    if (commentsLoading && comments.isEmpty() && hotComments.isEmpty()) {
                        item(key = "comments_loading") {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = DqdColors.Primary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    } else if (commentsError != null && comments.isEmpty() && hotComments.isEmpty()) {
                        item(key = "comments_error") {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(commentsError ?: "评论加载失败", color = DqdColors.TextTertiary, fontSize = 13.sp)
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = { scope.launch { loadCommentsOnly() } },
                                    colors = ButtonDefaults.buttonColors(containerColor = DqdColors.SurfaceHigh),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text("点击重试评论", color = DqdColors.TextPrimary, fontSize = 12.5.sp)
                                }
                            }
                        }
                    } else if (comments.isEmpty() && hotComments.isEmpty()) {
                        item(key = "no_comments") {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("暂无评论（部分外链专题/圈子帖暂未开放）", color = DqdColors.TextTertiary, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(comments, key = { "all_${it.id}" }) { c ->
                            CommentCardRow(c)
                            HorizontalDivider(
                                Modifier.padding(start = 54.dp, end = 16.dp),
                                thickness = 0.5.dp,
                                color = DqdColors.Divider
                            )
                        }

                        if (commentNextUrl != null) {
                            item(key = "load_more_comments") {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (loadingMoreComments) {
                                        CircularProgressIndicator(
                                            color = DqdColors.Primary,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else {
                                        Button(
                                            onClick = {
                                                val next = commentNextUrl ?: return@Button
                                                loadingMoreComments = true
                                                scope.launch {
                                                    runCatching { DqdApi.comments(articleId, nextUrl = next) }
                                                        .onSuccess { page ->
                                                            val seen = comments.map { it.id }.toHashSet()
                                                            val added = page.comments.filter { it.id !in seen }
                                                            comments = comments + added
                                                            commentNextUrl = if (added.isEmpty()) null else page.nextUrl
                                                        }
                                                    loadingMoreComments = false
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = DqdColors.SurfaceHigh),
                                            shape = RoundedCornerShape(18.dp)
                                        ) {
                                            Text("加载更多评论", color = DqdColors.TextPrimary, fontSize = 13.sp)
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

    viewerImageUrl?.let { imgUrl ->
        ImageViewerDialog(url = imgUrl, onDismiss = { viewerImageUrl = null })
    }
}

@Composable
private fun CommentSectionHeader(title: String, count: Int, accent: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(15.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent)
        )
        Spacer(Modifier.width(8.dp))
        Text(title, color = DqdColors.TextPrimary, fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            Text("($count)", color = DqdColors.TextTertiary, fontSize = 12.5.sp)
        }
    }
}

@Composable
private fun CommentCardRow(c: CommentItem) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (!c.userAvatar.isNullOrBlank()) {
            AsyncImage(
                model = c.userAvatar,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(DqdColors.SurfaceHigh)
            )
        } else {
            Box(
                Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(DqdColors.SurfaceHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(c.userName.take(1), color = DqdColors.TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    c.userName,
                    color = DqdColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (c.upCount > 0) {
                    Icon(
                        Icons.Outlined.ThumbUp,
                        contentDescription = null,
                        tint = if (c.isHot) DqdColors.Red else DqdColors.TextTertiary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        c.upCount.toString(),
                        color = if (c.isHot) DqdColors.Red else DqdColors.TextTertiary,
                        fontSize = 11.5.sp,
                        fontWeight = if (c.isHot) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
            Spacer(Modifier.height(5.dp))
            if (c.quoteContent.isNotBlank()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(DqdColors.SurfaceHigh)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    if (c.quoteUser.isNotBlank()) {
                        Text(c.quoteUser, color = DqdColors.Primary, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                    }
                    Text(
                        c.quoteContent,
                        color = DqdColors.TextSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                c.content,
                color = DqdColors.TextPrimary,
                fontSize = 14.5.sp,
                lineHeight = 21.sp
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(c.time, color = DqdColors.TextTertiary, fontSize = 11.sp)
                if (c.replyCount > 0) {
                    Spacer(Modifier.width(10.dp))
                    Text("${c.replyCount} 条回复", color = DqdColors.Primary, fontSize = 11.sp)
                }
            }
        }
    }
}
