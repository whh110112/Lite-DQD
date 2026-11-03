package com.whh11.dqdlite.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 懂球帝数据层。
 *
 * 资讯走它的公开 JSON 接口；
 * 赛程走移动版比分页（m.dongqiudi.com/match）——那一页是服务端直出的，
 * HTML 里嵌着完整的 matchListStore.matchList 数组，100 场比赛带比分、队徽、状态。
 */
object DqdApi {

    private const val UA =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148"

    private const val UA_ANDROID =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    /**
     * 栏目白名单。
     *
     * 收录：图文资讯 + 比赛集锦（tab 11）。
     * 刻意排除：
     *  - tab 43「视频」：channel=mini_top 的竖屏短视频流（is_video=true，带 video_info.video_src），
     *    属短视频，不接入；
     *  - tab 58「彩经」、tab 59「预测」：彩票内容，不接入。
     */
    val TABS = listOf(
        FeedTab(1, "头条"),
        FeedTab(253, "足球"),
        FeedTab(3, "英超"),
        FeedTab(5, "西甲"),
        FeedTab(4, "意甲"),
        FeedTab(6, "德甲"),
        FeedTab(56, "中超"),
        FeedTab(11, "集锦")
    )

    /** 栏目 id 11 是比赛集锦，条目本身就是视频 */
    const val TAB_HIGHLIGHTS = 11

    suspend fun news(tabId: Int): List<News> = withContext(Dispatchers.IO) {
        val body = get(
            "https://api.dongqiudi.com/app/tabs/iphone/$tabId.json",
            UA,
            "application/json"
        )
        parseNews(body)
    }

    suspend fun matches(): List<Match> = withContext(Dispatchers.IO) {
        val html = get("https://m.dongqiudi.com/match", UA_ANDROID, "text/html")
        parseMatches(html)
    }

    private fun get(url: String, ua: String, accept: String): String {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", ua)
            .header("Accept", accept)
            .build()
        return client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            resp.body?.string().orEmpty()
        }
    }

    private fun parseNews(body: String): List<News> {
        val root = JSONObject(body)
        val arr = root.optJSONArray("articles") ?: return emptyList()
        val out = ArrayList<News>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val title = o.optString("title")
            if (title.isBlank()) continue
            out += News(
                id = o.optLong("id"),
                title = title,
                thumb = normalizeUrl(o.optString("thumb")),
                category = o.optString("category"),
                subCategory = o.optString("secondary_category"),
                comments = o.optInt("comments_total"),
                showTime = o.optLong("show_time"),
                // 集锦条目里 is_video 有时为 false，但 showtype=video / has_video=1，
                // 三个信号取并集才不会漏标
                isVideo = o.optBoolean("is_video") ||
                    o.optString("showtype") == "video" ||
                    o.optInt("has_video") == 1,
                isTop = o.optBoolean("top"),
                author = o.optString("author_name"),
                url = o.optString("url").takeIf { it.startsWith("http") } ?: ""
            )
        }
        return out
    }

    private fun parseMatches(html: String): List<Match> {
        val json = extractJsonArray(html, "\"matchListStore\":{\"matchList\":") ?: return emptyList()
        val arr = JSONArray(json)
        val out = ArrayList<Match>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optString("match_id")
            val a = o.optString("team_A_name")
            val b = o.optString("team_B_name")
            if (id.isBlank() || a.isBlank() || b.isBlank()) continue
            out += Match(
                id = id,
                teamA = a,
                teamB = b,
                logoA = normalizeUrl(o.optString("team_A_logo")),
                logoB = normalizeUrl(o.optString("team_B_logo")),
                scoreA = o.optString("fs_A"),
                scoreB = o.optString("fs_B"),
                status = o.optString("status"),
                period = o.optString("minute_period"),
                competition = o.optString("competition_name").ifBlank { "其他" },
                competitionColor = o.optString("competition_color").takeIf { it.startsWith("#") },
                startPlay = o.optString("start_play"),
                timeUtc = o.optString("time_utc")
            )
        }
        return out
    }

    /** 从 HTML 里抠出嵌在 JS state 中的 JSON 数组，靠括号配对找结尾 */
    private fun extractJsonArray(html: String, key: String): String? {
        val keyIndex = html.indexOf(key)
        if (keyIndex < 0) return null
        val start = html.indexOf('[', keyIndex)
        if (start < 0) return null

        var depth = 0
        var inString = false
        var escaped = false
        var p = start
        while (p < html.length) {
            val c = html[p]
            if (inString) {
                when {
                    escaped -> escaped = false
                    c == '\\' -> escaped = true
                    c == '"' -> inString = false
                }
            } else {
                when (c) {
                    '"' -> inString = true
                    '[' -> depth++
                    ']' -> {
                        depth--
                        if (depth == 0) return html.substring(start, p + 1)
                    }
                }
            }
            p++
        }
        return null
    }

    private fun normalizeUrl(s: String?): String? {
        if (s.isNullOrBlank()) return null
        return when {
            s.startsWith("//") -> "https:$s"
            s.startsWith("http") -> s
            else -> null
        }
    }
}
