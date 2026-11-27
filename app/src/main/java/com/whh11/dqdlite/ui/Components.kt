package com.whh11.dqdlite.ui

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.SportsSoccer
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.whh11.dqdlite.data.News
import com.whh11.dqdlite.data.relTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class BottomTabItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val DqdBottomTabs = listOf(
    BottomTabItem("首页", Icons.Filled.Home, Icons.Outlined.Home),
    BottomTabItem("比赛", Icons.Filled.SportsSoccer, Icons.Outlined.SportsSoccer),
    BottomTabItem("关注", Icons.Filled.Star, Icons.Outlined.StarBorder),
    BottomTabItem("数据", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents)
)

@Composable
fun DqdBottomBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onReselect: (Int) -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(DqdColors.Card)
    ) {
        HorizontalDivider(thickness = 0.5.dp, color = DqdColors.Divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DqdBottomTabs.forEachIndexed { idx, item ->
                val active = idx == selectedIndex
                val tint = if (active) DqdColors.Primary else DqdColors.TextSecondary
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (active) onReselect(idx) else onSelect(idx)
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        if (active) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        item.label,
                        color = tint,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun NewsRowItem(
    news: News,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(DqdColors.Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = news.title,
                color = DqdColors.TextPrimary,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (news.isTop) {
                    MiniBadge("置顶", DqdColors.Red)
                    Spacer(Modifier.width(6.dp))
                }
                if (news.isVideo) {
                    MiniBadge("视频", DqdColors.Primary)
                    Spacer(Modifier.width(6.dp))
                }
                val tag = news.subCategory.ifBlank { news.category }
                if (tag.isNotBlank() && !news.isTop) {
                    Text(
                        tag,
                        color = DqdColors.TextTertiary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                }
                if (news.showTime > 0L) {
                    Text(relTime(news.showTime), color = DqdColors.TextTertiary, fontSize = 11.5.sp)
                }
                Spacer(Modifier.weight(1f))
                if (news.comments > 0) {
                    Text(
                        "${news.comments}评论",
                        color = DqdColors.TextTertiary,
                        fontSize = 11.5.sp
                    )
                }
            }
        }
        news.thumb?.let { url ->
            Spacer(Modifier.width(12.dp))
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 108.dp, height = 74.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DqdColors.SurfaceHigh)
            )
        }
    }
}

@Composable
fun MiniBadge(text: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(text, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = DqdColors.Primary, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
    }
}

@Composable
fun ErrorRetryBox(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("加载失败", color = DqdColors.TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(message, color = DqdColors.TextSecondary, fontSize = 12.5.sp)
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = DqdColors.Primary),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text("重新加载", fontSize = 13.sp, color = Color.White)
        }
    }
}

@Composable
fun EmptyBox(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = DqdColors.TextTertiary, fontSize = 13.5.sp)
    }
}

@Composable
fun rememberGifImageLoader(): ImageLoader {
    val context = LocalContext.current
    return remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
                else add(GifDecoder.Factory())
            }
            .build()
    }
}

@Composable
fun ImageViewerDialog(
    url: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gifLoader = rememberGifImageLoader()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var saving by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = (scale * zoom).coerceIn(1f, 4f)
                        scale = nextScale
                        offset = if (nextScale > 1f) offset + pan else Offset.Zero
                    }
                }
        ) {
            AsyncImage(
                model = url,
                imageLoader = gifLoader,
                contentDescription = "查看原图",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .clickable {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            onDismiss()
                        }
                    }
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
                }

                IconButton(
                    enabled = !saving,
                    onClick = {
                        saving = true
                        scope.launch {
                            val ok = saveImageToGallery(context, url)
                            saving = false
                            Toast.makeText(
                                context,
                                if (ok) "图片已保存到相册" else "保存失败，请检查网络",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    Icon(Icons.Default.Download, contentDescription = "保存图片", tint = Color.White)
                }
            }
        }
    }
}

private suspend fun saveImageToGallery(context: Context, url: String): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val client = OkHttpClient()
        val req = Request.Builder().url(url).build()
        val bytes = client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) return@runCatching false
            res.body?.bytes() ?: return@runCatching false
        }
        val ext = when {
            url.contains(".gif", true) -> "gif"
            url.contains(".png", true) -> "png"
            else -> "jpg"
        }
        val mime = when (ext) {
            "gif" -> "image/gif"
            "png" -> "image/png"
            else -> "image/jpeg"
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "dqd_${System.currentTimeMillis()}.$ext")
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Lite-DQD")
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@runCatching false
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
        true
    }.getOrDefault(false)
}
