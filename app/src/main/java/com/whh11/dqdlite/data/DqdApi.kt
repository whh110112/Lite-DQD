package com.whh11.dqdlite.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 懂球帝全原生数据层（v3.0 全官方免鉴权 JSON 接口，彻底移除第三方混血接口）。
 */
object DqdApi {

    private const val UA_IOS =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15E148"

    private const val UA_ANDROID =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // 精简并剔除已停更的 tab 11 集锦与彩票/竖屏短视频栏目
    val TABS = listOf(
        FeedTab(1, "头条"),
        FeedTab(253, "足球"),
        FeedTab(56, "中超"),
        FeedTab(3, "英超"),
        FeedTab(5, "西甲"),
        FeedTab(4, "意甲"),
        FeedTab(6, "德甲"),
        FeedTab(12, "法甲"),
        FeedTab(57, "五洲"),
        FeedTab(55, "深度"),
        FeedTab(284, "快讯"),
        FeedTab(247, "篮球"),
        FeedTab(248, "体坛"),
        FeedTab(249, "电竞")
    )

    @Volatile
    private var cachedMenu: List<DataMenuGroup>? = null

    private fun get(url: String, ua: String = UA_IOS, referer: String = "https://m.dongqiudi.com/"): String {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", ua)
            .header("Referer", referer)
            .build()
        return client.newCall(req).execute().use { res ->
            if (!res.isSuccessful) throw IllegalStateException("HTTP ${res.code}")
            res.body?.string().orEmpty()
        }
    }

    /** 1. 资讯流（支持无限上滑分页 nextUrl） */
    suspend fun news(tabId: Int, nextUrl: String? = null): NewsPage = withContext(Dispatchers.IO) {
        val url = nextUrl?.takeIf { it.startsWith("http") }
            ?: "https://api.dongqiudi.com/app/tabs/iphone/$tabId.json?mark=gif&version=714"
        val body = get(url)
        parseNewsPage(body)
    }

    internal fun parseNewsPage(json: String): NewsPage {
        val out = ArrayList<News>()
        if (json.isBlank()) return NewsPage(out, null)
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return NewsPage(out, null)
        val list = root.optJSONArray("articles") ?: root.optJSONArray("data") ?: return NewsPage(out, null)
        val nextUrl = root.optString("next").takeIf { it.startsWith("http") }

        for (i in 0 until list.length()) {
            val o = list.optJSONObject(i) ?: continue
            val title = o.optString("title").ifBlank { o.optString("share_title") }.trim()
            if (title.isBlank()) continue

            val isVideo = o.optBoolean("is_video") ||
                o.optString("showtype") == "video" ||
                o.optInt("has_video") == 1

            val authorObj = o.optJSONObject("author")
            val author = authorObj?.optString("username")
                ?: authorObj?.optString("name")
                ?: o.optString("writer")

            val rawUrl = o.optString("url")
            val schemeId = Regex("""article/(\d+)""").find(rawUrl)?.groupValues?.getOrNull(1)?.toLongOrNull()
                ?: Regex("""articleId=(\d+)""").find(rawUrl)?.groupValues?.getOrNull(1)?.toLongOrNull()
            val rawId = o.optLong("id", 0L)
            val resolvedId = if (schemeId != null && schemeId > 0L) schemeId else rawId

            val showTime = o.optLong("show_time", o.optLong("published_at", 0L))
            out.add(
                News(
                    id = resolvedId,
                    title = title,
                    thumb = normalizeUrl(o.optString("thumb")),
                    category = o.optString("category"),
                    subCategory = o.optString("secondary_category"),
                    comments = o.optInt("comments_total", 0),
                    showTime = showTime,
                    sortTimestamp = o.optLong("sort_timestamp", showTime),
                    isVideo = isVideo,
                    isTop = o.optInt("top", 0) == 1,
                    author = author.trim(),
                    url = rawUrl
                )
            )
        }
        return NewsPage(out, nextUrl)
    }

    /** 2. 原生文章详情（免 WebView 折叠破甲，直接解析官方 JSON 正文） */
    suspend fun articleDetail(articleId: Long): ArticleDetail = withContext(Dispatchers.IO) {
        val url = "https://api.dongqiudi.com/v2/article/detail/$articleId"
        val body = get(url)
        parseArticleDetail(articleId, body)
    }

    internal fun parseArticleDetail(articleId: Long, json: String): ArticleDetail {
        val root = JSONObject(json)
        val d = root.optJSONObject("data") ?: throw IllegalStateException("文章数据为空")
        val title = d.optString("title").trim()
        val writer = d.optString("writer").ifBlank { d.optString("source") }.trim()
        val time = d.optString("time").trim()
        val thumb = normalizeUrl(d.optString("thumb"))
        val htmlBody = d.optString("body")

        val blocks = ArrayList<ContentBlock>()
        val images = LinkedHashSet<String>()

        if (htmlBody.isNotBlank()) {
            val doc = Jsoup.parseBodyFragment(htmlBody)
            for (el in doc.body().children()) {
                val tag = el.tagName().lowercase()
                val imgs = el.select("img")
                if (imgs.isNotEmpty()) {
                    for (img in imgs) {
                        val src = normalizeUrl(
                            img.attr("data-original")
                                .ifBlank { img.attr("data-src") }
                                .ifBlank { img.attr("src") }
                        )
                        if (!src.isNullOrBlank()) {
                            images.add(src)
                            val cap = img.attr("alt").trim()
                            blocks.add(ContentBlock.Image(src, cap))
                        }
                    }
                }
                val text = el.text().trim()
                if (text.isNotBlank()) {
                    when (tag) {
                        "h1", "h2", "h3", "h4" -> blocks.add(ContentBlock.Heading(text))
                        "blockquote" -> blocks.add(ContentBlock.Quote(text))
                        else -> blocks.add(ContentBlock.Paragraph(text))
                    }
                }
            }
            if (blocks.isEmpty()) {
                val plain = doc.text().trim()
                if (plain.isNotBlank()) blocks.add(ContentBlock.Paragraph(plain))
            }
        }

        // 补充 img_list
        val imgList = d.optJSONArray("img_list")
        if (imgList != null && images.isEmpty()) {
            for (i in 0 until imgList.length()) {
                val u = normalizeUrl(imgList.optString(i))
                if (!u.isNullOrBlank()) {
                    images.add(u)
                    blocks.add(ContentBlock.Image(u))
                }
            }
        }

        val videoObj = d.optJSONObject("new_video_detail")
        val videoUrl = normalizeUrl(
            videoObj?.optString("video_src").orEmpty()
                .ifBlank { videoObj?.optString("src").orEmpty() }
        )

        return ArticleDetail(
            id = articleId,
            title = title,
            writer = writer,
            time = time,
            thumb = thumb,
            blocks = blocks,
            imageUrls = images.toList(),
            videoUrl = videoUrl
        )
    }

    /** 3. 文章热评与全量评论（支持游标分页） */
    suspend fun comments(articleId: Long, nextUrl: String? = null): CommentPage = withContext(Dispatchers.IO) {
        val url = nextUrl?.takeIf { it.startsWith("http") }
            ?: "https://api.dongqiudi.com/v2/article/$articleId/comment?size=25"
        val body = get(url)
        parseCommentPage(body)
    }

    internal fun parseCommentPage(json: String): CommentPage {
        val root = runCatching { JSONObject(json) }.getOrNull()
            ?: return CommentPage(emptyList(), emptyList(), 0, null)
        val data = root.optJSONObject("data")
            ?: return CommentPage(emptyList(), emptyList(), 0, null)

        val users = HashMap<String, Pair<String, String?>>()
        val userArr = data.optJSONArray("user_list")
        if (userArr != null) {
            for (i in 0 until userArr.length()) {
                val u = userArr.optJSONObject(i) ?: continue
                val uid = u.optString("id")
                val name = u.optString("username").ifBlank { "热心球迷" }
                val avatar = normalizeUrl(u.optString("avatar"))
                if (uid.isNotBlank()) users[uid] = name to avatar
            }
        }

        fun parseList(arr: JSONArray?, isHot: Boolean): List<CommentItem> {
            if (arr == null) return emptyList()
            val list = ArrayList<CommentItem>(arr.length())
            for (i in 0 until arr.length()) {
                val c = arr.optJSONObject(i) ?: continue
                val content = c.optString("content").ifBlank { c.optString("content_old") }.trim()
                if (content.isBlank()) continue
                val uid = c.optString("user_id")
                val userPair = users[uid]
                val quoteObj = c.optJSONObject("quote")
                val qUid = quoteObj?.optString("user_id").orEmpty()
                val quoteUser = users[qUid]?.first ?: quoteObj?.optString("username").orEmpty()
                val quoteContent = quoteObj?.optString("content").orEmpty().trim()
                val up = c.optInt("up_count", c.optString("up").toIntOrNull() ?: 0)
                list.add(
                    CommentItem(
                        id = c.optString("id", "$isHot-$i"),
                        userName = userPair?.first ?: "热心球迷",
                        userAvatar = userPair?.second,
                        content = content,
                        time = c.optString("created_at"),
                        upCount = up,
                        replyCount = c.optString("reply_total").toIntOrNull() ?: c.optInt("reply_total", 0),
                        isHot = isHot,
                        quoteUser = quoteUser,
                        quoteContent = quoteContent
                    )
                )
            }
            return list
        }

        val hotList = parseList(data.optJSONArray("recommend_list"), isHot = true)
        val normalList = parseList(data.optJSONArray("comment_list"), isHot = false)
        val total = data.optInt("comment_total", normalList.size)
        val next = data.optString("next").takeIf { it.startsWith("http") }
        return CommentPage(hotList, normalList, total, next)
    }

    /** 4. 赛程获取（支持全品类） */
    suspend fun matches(category: SportCategory = SportCategory.ALL): List<Match> = withContext(Dispatchers.IO) {
        if (category == SportCategory.ALL) {
            val html = runCatching { get("https://m.dongqiudi.com/match", ua = UA_ANDROID) }.getOrDefault("")
            val parsed = parseMatchesFromHtml(html)
            if (parsed.isNotEmpty()) return@withContext parsed
        }
        val apiPath = if (category == SportCategory.ALL) "soccer" else category.apiPath
        val body = get("https://api.dongqiudi.com/data/tab/new/$apiPath")
        parseMatchesFromJson(body, category.label)
    }

    internal fun parseMatchesFromJson(json: String, categoryName: String): List<Match> {
        val out = ArrayList<Match>()
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return out
        val list = root.optJSONArray("list") ?: return out
        for (i in 0 until list.length()) {
            val o = list.optJSONObject(i) ?: continue
            parseMatchObject(o, categoryName)?.let { out.add(it) }
        }
        return out
    }

    internal fun parseMatchesFromHtml(html: String): List<Match> {
        val out = ArrayList<Match>()
        val key = "\"matchListStore\":{\"matchList\":["
        val startIdx = html.indexOf(key)
        if (startIdx < 0) return out
        val arrayStart = startIdx + key.length - 1
        val jsonArrayStr = extractJsonArray(html, arrayStart) ?: return out
        val arr = runCatching { JSONArray(jsonArrayStr) }.getOrNull() ?: return out
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            parseMatchObject(o, "赛事")?.let { out.add(it) }
        }
        return out
    }

    private fun parseMatchObject(o: JSONObject, fallbackCat: String): Match? {
        val teamA = o.optString("team_A_name").trim()
        val teamB = o.optString("team_B_name").trim()
        if (teamA.isBlank() || teamB.isBlank()) return null
        val matchId = o.optString("match_id").ifBlank { o.optString("relate_id") }
        if (matchId.isBlank()) return null
        return Match(
            id = matchId,
            teamAId = o.optString("team_A_id"),
            teamBId = o.optString("team_B_id"),
            teamA = teamA,
            teamB = teamB,
            logoA = normalizeUrl(o.optString("team_A_logo")),
            logoB = normalizeUrl(o.optString("team_B_logo")),
            scoreA = o.optString("fs_A"),
            scoreB = o.optString("fs_B"),
            halfScoreA = o.optString("hts_A"),
            halfScoreB = o.optString("hts_B"),
            status = o.optString("status"),
            period = o.optString("minute_period"),
            competition = o.optString("competition_name").ifBlank { o.optString("competition").ifBlank { fallbackCat } },
            competitionColor = o.optString("competition_color").takeIf { it.startsWith("#") },
            startPlay = o.optString("start_play").ifBlank { o.optString("date_utc") },
            timeUtc = o.optString("time_utc"),
            category = fallbackCat
        )
    }

    /** 5. 原生比赛详情（并发拉取赛况/技术统计 + 首发阵型/替补 + 赛前前瞻） */
    suspend fun matchDetail(matchId: String): MatchDetail = coroutineScope {
        val sitDeferred = async(Dispatchers.IO) {
            runCatching { get("https://api.dongqiudi.com/mobile/match/situation/$matchId") }.getOrNull()
        }
        val linDeferred = async(Dispatchers.IO) {
            runCatching { get("https://api.dongqiudi.com/mobile/match/lineup/$matchId") }.getOrNull()
        }
        val anaDeferred = async(Dispatchers.IO) {
            runCatching { get("https://api.dongqiudi.com/mobile/match/analysis/$matchId") }.getOrNull()
        }
        parseMatchDetail(matchId, sitDeferred.await(), linDeferred.await(), anaDeferred.await())
    }

    internal fun parseMatchDetail(
        matchId: String,
        sitJson: String?,
        linJson: String?,
        anaJson: String? = null
    ): MatchDetail {
        val sitRoot = sitJson?.let { runCatching { JSONObject(it) }.getOrNull() }
        val linRoot = linJson?.let { runCatching { JSONObject(it) }.getOrNull() }
        val anaRoot = anaJson?.let { runCatching { JSONObject(it) }.getOrNull() }
        val matchObj = sitRoot?.optJSONObject("match")
            ?: linRoot?.optJSONObject("match")
            ?: anaRoot?.optJSONObject("match")
            ?: throw IllegalStateException("未获取到比赛详情数据")

        val comp = matchObj.optJSONObject("competition")?.optString("name").orEmpty().ifBlank { "赛事" }
        val teamAObj = matchObj.optJSONObject("team_A")
        val teamBObj = matchObj.optJSONObject("team_B")
        val anaInfo = anaRoot?.optJSONObject("info")
        val teamA = teamAObj?.optString("name").orEmpty()
            .ifBlank { matchObj.optString("new_Team_A_Name") }
            .ifBlank { anaInfo?.optString("team_A").orEmpty() }
        val teamB = teamBObj?.optString("name").orEmpty()
            .ifBlank { matchObj.optString("new_Team_B_Name") }
            .ifBlank { anaInfo?.optString("team_B").orEmpty() }
        val teamAId = teamAObj?.optString("id").orEmpty()
            .ifBlank { anaInfo?.optString("team_A_href").orEmpty().substringAfterLast("/") }
        val teamBId = teamBObj?.optString("id").orEmpty()
            .ifBlank { anaInfo?.optString("team_B_href").orEmpty().substringAfterLast("/") }
        val logoA = normalizeUrl(teamAObj?.optString("logo").orEmpty().ifBlank { anaInfo?.optString("team_A_logo").orEmpty() })
        val logoB = normalizeUrl(teamBObj?.optString("logo").orEmpty().ifBlank { anaInfo?.optString("team_B_logo").orEmpty() })
        val scoreA = teamAObj?.optString("score").orEmpty()
        val scoreB = teamBObj?.optString("score").orEmpty()
        val status = matchObj.optString("status")
        val period = matchObj.optString("minute").ifBlank { matchObj.optString("period") }
        val startTime = matchObj.optString("start_play")

        // 解析事件时间轴
        val events = ArrayList<MatchEvent>()
        val sitInfo = sitRoot?.optJSONObject("info")
        val eventsObj = sitInfo?.optJSONObject("events")
        if (eventsObj != null) {
            val keys = eventsObj.keys().asSequence().toList()
                .sortedBy { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 999 }
            for (k in keys) {
                val bucket = eventsObj.optJSONObject(k) ?: continue
                val min = bucket.optString("minute", k)
                fun addSide(arr: JSONArray?, isA: Boolean) {
                    if (arr == null) return
                    for (i in 0 until arr.length()) {
                        val e = arr.optJSONObject(i) ?: continue
                        val person = e.optString("person").trim()
                        if (person.isBlank()) continue
                        events.add(
                            MatchEvent(
                                minute = "${min}'",
                                isTeamA = isA,
                                playerName = person,
                                iconUrl = normalizeUrl(e.optString("event_pic")),
                                typeText = e.optString("type").ifBlank { e.optString("event_name") }
                            )
                        )
                    }
                }
                addSide(bucket.optJSONArray("teamAEvents"), true)
                addSide(bucket.optJSONArray("teamBEvents"), false)
            }
        }

        // 解析技术统计（修复：team_A/team_B 在官方接口中为 {"value": 70, "per": 70} 嵌套对象）
        val stats = ArrayList<MatchStat>()
        val statObj = sitInfo?.optJSONObject("statistics")
        val statList = statObj?.optJSONArray("list")
        if (statList != null) {
            for (i in 0 until statList.length()) {
                val s = statList.optJSONObject(i) ?: continue
                val type = s.optString("type").trim()
                if (type.isBlank()) continue
                val objA = s.optJSONObject("team_A")
                val objB = s.optJSONObject("team_B")
                val vA = objA?.optString("value")?.trim() ?: s.optString("team_A").trim()
                val vB = objB?.optString("value")?.trim() ?: s.optString("team_B").trim()
                val pA = objA?.optDouble("per", Double.NaN) ?: Double.NaN
                val pB = objB?.optDouble("per", Double.NaN) ?: Double.NaN
                val nA = if (!pA.isNaN()) pA.toFloat() else (vA.replace("%", "").toFloatOrNull() ?: 0f)
                val nB = if (!pB.isNaN()) pB.toFloat() else (vB.replace("%", "").toFloatOrNull() ?: 0f)
                val ratio = if (nA + nB > 0f) (nA / (nA + nB)).coerceIn(0.05f, 0.95f) else 0.5f
                val dispA = if (type.contains("率") && !vA.contains("%")) "$vA%" else vA
                val dispB = if (type.contains("率") && !vB.contains("%")) "$vB%" else vB
                stats.add(MatchStat(type, dispA, dispB, ratio))
            }
        }

        // 解析阵容与场地天气
        val linInfo = linRoot?.optJSONObject("info")
        val baseObj = linInfo?.optJSONObject("base")
        val venue = baseObj?.optString("field").orEmpty().ifBlank { matchObj.optJSONObject("venue")?.optString("name").orEmpty() }
        val weather = listOf(baseObj?.optString("weather").orEmpty(), baseObj?.optString("temperature").orEmpty())
            .filter { it.isNotBlank() }.joinToString(" ")
        val referee = baseObj?.optString("referee").orEmpty().trim()
        val formationA = statObj?.optJSONObject("team_A")?.optString("formation").orEmpty()
        val formationB = statObj?.optJSONObject("team_B")?.optString("formation").orEmpty()

        val lineupA = ArrayList<LineupPlayer>()
        val lineupB = ArrayList<LineupPlayer>()
        val subsA = ArrayList<LineupPlayer>()
        val subsB = ArrayList<LineupPlayer>()

        fun parsePlayer(p: JSONObject?): LineupPlayer? {
            if (p == null) return null
            val name = p.optString("person").trim()
            if (name.isBlank()) return null
            return LineupPlayer(
                id = p.optString("person_id"),
                name = name,
                number = p.optString("shirtnumber"),
                position = p.optString("position_x"),
                avatar = normalizeUrl(p.optString("person_logo")),
                isCaptain = p.optInt("captain", 0) == 1
            )
        }

        val linArr = linInfo?.optJSONArray("lineup")
        if (linArr != null) {
            for (i in 0 until linArr.length()) {
                val item = linArr.optJSONObject(i) ?: continue
                parsePlayer(item.optJSONObject("team_A"))?.let { lineupA.add(it) }
                parsePlayer(item.optJSONObject("team_B"))?.let { lineupB.add(it) }
            }
        }
        val subArr = linInfo?.optJSONArray("sub")
        if (subArr != null) {
            for (i in 0 until subArr.length()) {
                val item = subArr.optJSONObject(i) ?: continue
                parsePlayer(item.optJSONObject("team_A"))?.let { subsA.add(it) }
                parsePlayer(item.optJSONObject("team_B"))?.let { subsB.add(it) }
            }
        }

        val analysis = parseMatchAnalysis(anaInfo)

        return MatchDetail(
            matchId = matchId,
            competition = comp,
            status = status,
            period = period,
            startTime = startTime,
            teamA = teamA,
            teamB = teamB,
            teamAId = teamAId,
            teamBId = teamBId,
            logoA = logoA,
            logoB = logoB,
            scoreA = scoreA,
            scoreB = scoreB,
            venue = venue,
            weather = weather,
            referee = referee,
            formationA = formationA,
            formationB = formationB,
            events = events,
            stats = stats,
            lineupA = lineupA,
            lineupB = lineupB,
            subsA = subsA,
            subsB = subsB,
            analysis = analysis
        )
    }

    private fun parseMatchAnalysis(info: JSONObject?): MatchAnalysis? {
        if (info == null) return null
        fun parseRecords(arr: JSONArray?): List<AnalysisRecordItem> {
            if (arr == null) return emptyList()
            val out = ArrayList<AnalysisRecordItem>(arr.length())
            for (i in 0 until arr.length()) {
                val r = arr.optJSONObject(i) ?: continue
                val a = r.optString("team_A_name").trim()
                val b = r.optString("team_B_name").trim()
                if (a.isBlank() && b.isBlank()) continue
                out.add(
                    AnalysisRecordItem(
                        date = r.optString("date"),
                        competition = r.optString("competition"),
                        teamA = a,
                        teamB = b,
                        scoreA = r.optString("fs_A"),
                        scoreB = r.optString("fs_B")
                    )
                )
            }
            return out
        }

        val bh = info.optJSONObject("battle_history")
        val pct = bh?.optJSONObject("percent")
        val battleSummary = if (pct != null && (pct.optInt("win") + pct.optInt("draw") + pct.optInt("lose") > 0)) {
            "主队胜 ${pct.optInt("win")}% · 平 ${pct.optInt("draw")}% · 客队胜 ${pct.optInt("lose")}%"
        } else ""
        val battleList = parseRecords(bh?.optJSONArray("list"))

        val rr = info.optJSONObject("recent_record")
        val recentA = parseRecords(rr?.optJSONArray("team_A"))
        val recentB = parseRecords(rr?.optJSONArray("team_B"))

        val lt = info.optJSONObject("league_table")
        fun parseSide(o: JSONObject?): AnalysisTableSide? {
            val tot = o?.optJSONObject("total") ?: return null
            val rank = tot.optString("rank")
            if (rank.isBlank()) return null
            return AnalysisTableSide(
                rank = rank,
                played = tot.optString("matches_total"),
                win = tot.optString("matches_won"),
                draw = tot.optString("matches_draw"),
                loss = tot.optString("matches_lost"),
                points = tot.optString("points"),
                winRate = tot.optString("win_rate")
            )
        }
        val tableA = parseSide(lt?.optJSONObject("team_A"))
        val tableB = parseSide(lt?.optJSONObject("team_B"))

        val sl = info.optJSONObject("sideline")
        fun parseSideline(arr: JSONArray?): List<SidelinePlayer> {
            if (arr == null) return emptyList()
            val out = ArrayList<SidelinePlayer>(arr.length())
            for (i in 0 until arr.length()) {
                val p = arr.optJSONObject(i) ?: continue
                val n = p.optString("person_name").ifBlank { p.optString("name") }.trim()
                if (n.isBlank()) continue
                out.add(
                    SidelinePlayer(
                        name = n,
                        reason = p.optString("reason").ifBlank { p.optString("type") },
                        logo = normalizeUrl(p.optString("person_logo"))
                    )
                )
            }
            return out
        }
        val sideA = parseSideline(sl?.optJSONArray("team_A"))
        val sideB = parseSideline(sl?.optJSONArray("team_B"))

        if (battleList.isEmpty() && recentA.isEmpty() && recentB.isEmpty() && tableA == null && tableB == null && sideA.isEmpty() && sideB.isEmpty()) {
            return null
        }
        return MatchAnalysis(
            battleSummary = battleSummary,
            battleHistory = battleList.take(8),
            recentA = recentA.take(6),
            recentB = recentB.take(6),
            tableA = tableA,
            tableB = tableB,
            sidelineA = sideA,
            sidelineB = sideB
        )
    }

    /** 6. 动态全赛事配置菜单（覆盖全球足球/女足/全民足球/电竞等全部官方数据中台） */
    suspend fun dataMenu(forceRefresh: Boolean = false): List<DataMenuGroup> = withContext(Dispatchers.IO) {
        if (!forceRefresh) cachedMenu?.let { return@withContext it }
        val body = get("https://api.dongqiudi.com/v2/config/data_menu?mark=gif&platform=iphone&version=714")
        val parsed = parseDataMenu(body)
        if (parsed.isNotEmpty()) cachedMenu = parsed
        parsed
    }

    internal fun parseDataMenu(json: String): List<DataMenuGroup> {
        val out = ArrayList<DataMenuGroup>()
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return out
        val list = root.optJSONObject("data")?.optJSONArray("list") ?: return out

        for (i in 0 until list.length()) {
            val grp = list.optJSONObject(i) ?: continue
            val title = grp.optString("title").trim()
            if (title.isBlank() || title == "热榜") continue
            val arr = grp.optJSONArray("data") ?: continue
            val leagues = ArrayList<MenuLeague>()
            for (j in 0 until arr.length()) {
                val o = arr.optJSONObject(j) ?: continue
                val label = o.optString("label").trim()
                if (label.isBlank()) continue
                val subTabs = o.optJSONArray("sub_tabs")
                var standingUrl = ""
                var personUrl = ""
                var teamUrl = ""
                var scheduleUrl = ""
                if (subTabs != null) {
                    for (k in 0 until subTabs.length()) {
                        val st = subTabs.optJSONObject(k) ?: continue
                        val t = st.optString("title")
                        val u = st.optString("url")
                        when {
                            t.contains("积分") || t.contains("排名") -> if (standingUrl.isBlank()) standingUrl = u
                            t.contains("球员") -> if (personUrl.isBlank()) personUrl = u
                            t.contains("球队") -> if (teamUrl.isBlank()) teamUrl = u
                            t.contains("赛程") -> if (scheduleUrl.isBlank()) scheduleUrl = u
                        }
                    }
                }
                if (standingUrl.isBlank() && personUrl.isBlank() && scheduleUrl.isBlank()) continue
                leagues.add(
                    MenuLeague(
                        id = o.optString("id", "$i-$j"),
                        label = label,
                        logo = normalizeUrl(o.optString("logo")),
                        competitionId = o.optString("competition_id"),
                        seasonId = o.optString("season_id"),
                        seasonTitle = o.optJSONObject("season")?.optString("title").orEmpty(),
                        standingUrl = standingUrl,
                        personRankUrl = personUrl,
                        teamRankUrl = teamUrl,
                        scheduleUrl = scheduleUrl
                    )
                )
            }
            if (leagues.isNotEmpty()) out.add(DataMenuGroup(title, leagues))
        }
        return out
    }

    /** 7. 官方积分榜 */
    suspend fun standings(url: String): List<StandingRow> = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext emptyList()
        val body = get(url)
        parseStandings(body)
    }

    internal fun parseStandings(json: String): List<StandingRow> {
        val out = ArrayList<StandingRow>()
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return out
        val content = root.optJSONObject("content") ?: return out
        val rounds = content.optJSONArray("rounds") ?: return out
        for (ri in 0 until rounds.length()) {
            val round = rounds.optJSONObject(ri) ?: continue
            val rc = round.optJSONObject("content") ?: continue
            val groupName = if (rounds.length() > 1) rc.optString("name") else ""
            val data = rc.optJSONArray("data") ?: continue
            for (i in 0 until data.length()) {
                val o = data.optJSONObject(i) ?: continue
                val name = o.optString("team_name").trim()
                if (name.isBlank()) continue
                val rank = o.optInt("rank", i + 1).let { if (it <= 0) i + 1 else it }
                val lastRank = o.optInt("last_rank", 0)
                out.add(
                    StandingRow(
                        rank = rank,
                        teamId = o.optString("team_id"),
                        teamName = name,
                        teamLogo = normalizeUrl(o.optString("team_logo")),
                        played = o.optString("matches_total", "0"),
                        win = o.optString("matches_won", "0"),
                        draw = o.optString("matches_draw", "0"),
                        loss = o.optString("matches_lost", "0"),
                        goalsFor = o.optString("goals_pro", "0"),
                        goalsAgainst = o.optString("goals_against", "0"),
                        points = o.optString("points", "0"),
                        groupTitle = groupName,
                        zoneColor = o.optString("color").takeIf { it.startsWith("#") },
                        rankDelta = if (lastRank > 0) lastRank - rank else null
                    )
                )
            }
        }
        return out
    }

    /** 8. 官方球员榜子维度（射手榜、助攻榜、黄牌、传球等动态拉取） */
    suspend fun rankingSubTabs(url: String): List<RankSubTab> = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext emptyList()
        val body = get(url)
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return@withContext emptyList()
        val arr = root.optJSONObject("content")?.optJSONArray("data") ?: return@withContext emptyList()
        val out = ArrayList<RankSubTab>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val name = o.optString("name").trim()
            val u = o.optString("url").trim()
            if (name.isNotBlank() && u.startsWith("http")) {
                out.add(RankSubTab(name, o.optString("type"), u))
            }
        }
        out
    }

    /** 9. 官方球员榜明细（射手/助攻等） */
    suspend fun personRanking(url: String): List<PlayerRankRow> = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext emptyList()
        val body = get(url)
        parsePersonRanking(body)
    }

    internal fun parsePersonRanking(json: String): List<PlayerRankRow> {
        val out = ArrayList<PlayerRankRow>()
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return out
        val arr = root.optJSONObject("content")?.optJSONArray("data") ?: return out
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val name = o.optString("person_name").ifBlank { o.optString("name") }.trim()
            if (name.isBlank()) continue
            out.add(
                PlayerRankRow(
                    rank = o.optString("rank").toIntOrNull() ?: (i + 1),
                    playerId = o.optString("person_id"),
                    playerName = name,
                    playerLogo = normalizeUrl(o.optString("person_logo")),
                    teamId = o.optString("team_id"),
                    teamName = o.optString("team_name").trim(),
                    teamLogo = normalizeUrl(o.optString("team_logo")),
                    count = o.optString("count").ifBlank { o.optString("score") }
                )
            )
        }
        return out
    }

    /** 10. 官方球队主页（档案/荣誉 + 全赛季赛程 + 一线队大名单） */
    suspend fun teamDetail(teamId: String): TeamDetail = coroutineScope {
        val detailDeferred = async(Dispatchers.IO) {
            runCatching { get("https://sport-data.dongqiudi.com/soccer/biz/dqd/team/detail/$teamId") }.getOrNull()
        }
        val scheduleDeferred = async(Dispatchers.IO) {
            runCatching { get("https://sport-data.dongqiudi.com/soccer/biz/dqd/team/schedule/$teamId?app=dqd&version=0") }.getOrNull()
        }
        val memberDeferred = async(Dispatchers.IO) {
            runCatching { get("https://sport-data.dongqiudi.com/soccer/biz/dqd/team/member/$teamId") }.getOrNull()
        }
        parseTeamDetail(teamId, detailDeferred.await(), scheduleDeferred.await(), memberDeferred.await())
    }

    internal fun parseTeamDetail(
        teamId: String,
        detailJson: String?,
        scheduleJson: String?,
        memberJson: String?
    ): TeamDetail {
        val dRoot = detailJson?.let { runCatching { JSONObject(it) }.getOrNull() }
        val base = dRoot?.optJSONObject("base_info")
        val name = base?.optString("team_name").orEmpty().trim()
        val enName = base?.optString("team_en_name").orEmpty().trim()
        val logo = normalizeUrl(base?.optString("team_logo"))

        val facts = ArrayList<TeamFact>()
        val factArr = dRoot?.optJSONArray("base_info_v_1")
        if (factArr != null) {
            for (i in 0 until factArr.length()) {
                val f = factArr.optJSONObject(i) ?: continue
                val k = f.optString("type").trim()
                val v = f.optString("value").trim()
                if (k.isNotBlank() && v.isNotBlank()) facts.add(TeamFact(k, v))
            }
        }

        val honors = ArrayList<TeamHonor>()
        val honorArr = dRoot?.optJSONArray("honor_info")
        if (honorArr != null) {
            for (i in 0 until honorArr.length()) {
                val h = honorArr.optJSONObject(i) ?: continue
                val hn = h.optString("name").trim()
                if (hn.isBlank()) continue
                honors.add(TeamHonor(hn, h.optString("times", "1"), normalizeUrl(h.optString("logo"))))
            }
        }

        val members = ArrayList<TeamMemberItem>()
        val mRoot = memberJson?.let { runCatching { JSONObject(it) }.getOrNull() }
        val mGroups = mRoot?.optJSONObject("data")?.optJSONArray("list")
        if (mGroups != null) {
            for (gi in 0 until mGroups.length()) {
                val grp = mGroups.optJSONObject(gi) ?: continue
                val posTitle = grp.optString("title").trim()
                val arr = grp.optJSONArray("data") ?: continue
                for (pi in 0 until arr.length()) {
                    val p = arr.optJSONObject(pi) ?: continue
                    val pn = p.optString("person_name").trim()
                    val pid = p.optString("person_id")
                    if (pn.isBlank() || pid.isBlank()) continue
                    members.add(
                        TeamMemberItem(
                            personId = pid,
                            name = pn,
                            number = p.optString("shirtnumber"),
                            positionGroup = posTitle,
                            avatar = normalizeUrl(p.optString("person_logo")),
                            goals = p.optString("goals"),
                            appearances = p.optString("appearances")
                        )
                    )
                }
            }
        }

        val schedule = ArrayList<Match>()
        val sRoot = scheduleJson?.let { runCatching { JSONObject(it) }.getOrNull() }
        val sArr = sRoot?.optJSONArray("data") ?: sRoot?.optJSONArray("list")
        if (sArr != null) {
            for (i in 0 until sArr.length()) {
                val o = sArr.optJSONObject(i) ?: continue
                parseMatchObject(o, o.optString("competition_name", "赛事"))?.let { schedule.add(it) }
            }
        }

        return TeamDetail(
            teamId = teamId,
            name = name.ifBlank { "球队" },
            enName = enName,
            logo = logo,
            facts = facts,
            honors = honors,
            members = members,
            schedule = schedule
        )
    }

    /** 11. 官方球员主页（档案 + 技术特点 + 生涯履历 + 转会 + 伤病史） */
    suspend fun playerDetail(personId: String): PlayerDetail = withContext(Dispatchers.IO) {
        val body = get("https://sport-data.dongqiudi.com/soccer/biz/dqd/person/detail/$personId")
        parsePlayerDetail(personId, body)
    }

    internal fun parsePlayerDetail(personId: String, json: String): PlayerDetail {
        val root = JSONObject(json)
        val base = root.optJSONObject("base_info") ?: JSONObject()
        val name = base.optString("person_name").trim().ifBlank { "球员" }
        val enName = base.optString("person_en_name").trim()
        val avatar = normalizeUrl(base.optString("person_logo"))

        val facts = ArrayList<TeamFact>()
        val factArr = root.optJSONArray("base_info_v_1")
        if (factArr != null) {
            for (i in 0 until factArr.length()) {
                val f = factArr.optJSONObject(i) ?: continue
                val k = f.optString("type").trim()
                val v = f.optString("value").trim()
                if (k.isNotBlank() && v.isNotBlank()) facts.add(TeamFact(k, v))
            }
        }

        val charObj = root.optJSONObject("character_info")
        val styles = ArrayList<String>()
        val stArr = charObj?.optJSONArray("styles")
        if (stArr != null) {
            for (i in 0 until stArr.length()) {
                stArr.optString(i).trim().takeIf { it.isNotBlank() }?.let { styles.add(it) }
            }
        }
        val strengths = ArrayList<String>()
        val strObj = charObj?.optJSONObject("strength")
        listOf("very_strong", "strong").forEach { key ->
            val arr = strObj?.optJSONArray(key)
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    arr.optString(i).trim().takeIf { it.isNotBlank() }?.let { strengths.add(it) }
                }
            }
        }

        val honors = ArrayList<TeamHonor>()
        val honorArr = root.optJSONArray("honor_info")
        if (honorArr != null) {
            for (i in 0 until honorArr.length()) {
                val h = honorArr.optJSONObject(i) ?: continue
                val hn = h.optString("name").trim()
                if (hn.isNotBlank()) honors.add(TeamHonor(hn, h.optString("times", "1"), normalizeUrl(h.optString("logo"))))
            }
        }

        val careers = ArrayList<PlayerCareerItem>()
        val cArr = root.optJSONArray("player_career_info")
        if (cArr != null) {
            for (i in 0 until cArr.length()) {
                val c = cArr.optJSONObject(i) ?: continue
                val tn = c.optString("team_name").trim()
                if (tn.isBlank()) continue
                careers.add(
                    PlayerCareerItem(
                        teamId = c.optString("team_id"),
                        teamName = tn,
                        teamLogo = normalizeUrl(c.optString("team_logo")),
                        startDate = c.optString("start_date"),
                        endDate = c.optString("end_date")
                    )
                )
            }
        }

        val transfers = ArrayList<PlayerTransferItem>()
        val tArr = root.optJSONArray("transfer_info")
        if (tArr != null) {
            for (i in 0 until tArr.length()) {
                val t = tArr.optJSONObject(i) ?: continue
                transfers.add(
                    PlayerTransferItem(
                        date = t.optString("announced_date"),
                        fromClub = t.optString("from_club_name").ifBlank { "未知" },
                        toClub = t.optString("to_club_name").ifBlank { t.optString("club_name") },
                        fee = t.optString("money")
                    )
                )
            }
        }

        val injuries = ArrayList<PlayerInjuryItem>()
        val iArr = root.optJSONObject("injury_records")?.optJSONArray("history")
        if (iArr != null) {
            for (i in 0 until iArr.length()) {
                val inj = iArr.optJSONObject(i) ?: continue
                val reason = inj.optString("injury").trim()
                if (reason.isBlank()) continue
                injuries.add(
                    PlayerInjuryItem(
                        injury = reason,
                        dateFrom = inj.optString("date_from"),
                        dateUntil = inj.optString("date_until"),
                        days = inj.optString("days")
                    )
                )
            }
        }

        val currentClub = careers.firstOrNull()
        return PlayerDetail(
            personId = personId,
            name = name,
            enName = enName,
            avatar = avatar,
            teamId = base.optString("team_id").ifBlank { currentClub?.teamId.orEmpty() },
            teamName = base.optString("team_name").ifBlank { currentClub?.teamName.orEmpty() },
            facts = facts,
            styles = styles,
            strengths = strengths,
            honors = honors,
            careers = careers,
            transfers = transfers,
            injuries = injuries
        )
    }

    /** 12. 球队/球员关联新闻精准聚合（并发拉取官方频道多页并按名称及常见别名匹配） */
    suspend fun relatedNews(keywords: List<String>, extraTabIds: List<Int> = listOf(1, 253, 3, 5, 4, 6)): List<News> = coroutineScope {
        val cleanKeys = keywords.map { it.trim() }.filter { it.length >= 2 }.distinct()
        if (cleanKeys.isEmpty()) return@coroutineScope emptyList()
        val pages = extraTabIds.distinct().take(6).map { tid ->
            async(Dispatchers.IO) {
                val p1 = runCatching { news(tid) }.getOrNull()
                val p2 = p1?.nextUrl?.let { next -> runCatching { news(tid, next) }.getOrNull() }
                (p1?.items.orEmpty() + p2?.items.orEmpty())
            }
        }.flatMap { it.await() }

        pages.distinctBy { if (it.id > 0) it.id.toString() else it.title }
            .filter { item ->
                cleanKeys.any { kw ->
                    item.title.contains(kw, ignoreCase = true) || item.subCategory.contains(kw, ignoreCase = true)
                }
            }
            .sortedByDescending { it.sortTimestamp }
            .take(30)
    }

    /** 13. 全局官方搜索（替代报错 567 的 PC 网页搜索） */
    suspend fun search(keyword: String): SearchResult = withContext(Dispatchers.IO) {
        val q = keyword.trim()
        if (q.isBlank()) return@withContext SearchResult(emptyList(), emptyList(), emptyList())
        val enc = URLEncoder.encode(q, "UTF-8")
        val body = get("https://api.dongqiudi.com/search?keywords=$enc&type=all&page=1")
        parseSearchResult(body)
    }

    internal fun parseSearchResult(json: String): SearchResult {
        val root = runCatching { JSONObject(json) }.getOrNull()
            ?: return SearchResult(emptyList(), emptyList(), emptyList())

        val teams = ArrayList<SearchTeam>()
        val tArr = root.optJSONArray("teams")
        if (tArr != null) {
            for (i in 0 until tArr.length()) {
                val o = tArr.optJSONObject(i) ?: continue
                val name = o.optString("team_name").trim()
                if (name.isBlank()) continue
                teams.add(
                    SearchTeam(
                        teamId = o.optString("team_id"),
                        name = name,
                        enName = o.optString("team_en_name"),
                        logo = normalizeUrl(o.optString("team_img"))
                    )
                )
            }
        }

        val players = ArrayList<SearchPlayer>()
        val pArr = root.optJSONArray("players")
        if (pArr != null) {
            for (i in 0 until pArr.length()) {
                val o = pArr.optJSONObject(i) ?: continue
                val name = o.optString("person_name").ifBlank { o.optString("name") }.trim()
                if (name.isBlank()) continue
                players.add(
                    SearchPlayer(
                        playerId = o.optString("person_id"),
                        name = name,
                        teamName = o.optString("team_name"),
                        logo = normalizeUrl(o.optString("person_img").ifBlank { o.optString("person_logo") })
                    )
                )
            }
        }

        val news = ArrayList<News>()
        val nArr = root.optJSONArray("news")
        if (nArr != null) {
            for (i in 0 until nArr.length()) {
                val o = nArr.optJSONObject(i) ?: continue
                val title = o.optString("title").replace(Regex("<[^>]+>"), "").trim()
                if (title.isBlank()) continue
                news.add(
                    News(
                        id = o.optLong("id", 0L),
                        title = title,
                        thumb = normalizeUrl(o.optString("thumb")),
                        category = o.optString("category"),
                        subCategory = "",
                        comments = o.optInt("comments_total", 0),
                        showTime = o.optLong("show_time", 0L),
                        isVideo = o.optBoolean("is_video"),
                        isTop = false,
                        author = o.optString("writer"),
                        url = o.optString("url")
                    )
                )
            }
        }
        return SearchResult(teams, players, news)
    }

    private fun extractJsonArray(s: String, startIdx: Int): String? {
        if (startIdx >= s.length || s[startIdx] != '[') return null
        var depth = 0
        var inStr = false
        var esc = false
        for (i in startIdx until s.length) {
            val c = s[i]
            if (esc) { esc = false; continue }
            if (c == '\\') { esc = true; continue }
            if (c == '"') { inStr = !inStr; continue }
            if (!inStr) {
                if (c == '[') depth++
                else if (c == ']') {
                    depth--
                    if (depth == 0) return s.substring(startIdx, i + 1)
                }
            }
        }
        return null
    }

    private fun normalizeUrl(raw: String?): String? {
        val u = raw?.trim().orEmpty()
        if (u.isBlank()) return null
        return when {
            u.startsWith("//") -> "https:$u"
            u.startsWith("http://") -> "https://${u.removePrefix("http://")}"
            else -> u
        }
    }
}
