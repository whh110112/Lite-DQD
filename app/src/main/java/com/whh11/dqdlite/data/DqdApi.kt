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
 * 1. 资讯：从公开 JSON 接口拉取各栏目；
 * 2. 赛程：从移动版比分页（m.dongqiudi.com/match）提取 100 场完整赛程与比分；
 * 3. 圈子：按主队与专区（皇马、巴萨、曼城、阿森纳、曼联、拜仁、国足等）聚合讨论流，支持设为主队与关注；
 * 4. 联赛数据：提供欧冠、欧联、亚冠、英超、西甲、德甲、意甲、法甲、中超、NBA、CBA 完整的积分榜、球员榜（射手榜、助攻榜）。
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

    const val TAB_HIGHLIGHTS = 11

    // 社区圈子定义（支持主队与关注）
    val CIRCLES = listOf(
        DqdCircle("all", "全部热门", "热门", 1, ""),
        DqdCircle("realm", "皇家马德里", "西甲", 5, "皇马"),
        DqdCircle("barca", "巴塞罗那", "西甲", 5, "巴萨"),
        DqdCircle("mancity", "曼彻斯特城", "英超", 3, "曼城"),
        DqdCircle("arsenal", "阿森纳", "英超", 3, "阿森纳"),
        DqdCircle("liverpool", "利物浦", "英超", 3, "利物浦"),
        DqdCircle("manutd", "曼彻斯特联", "英超", 3, "曼联"),
        DqdCircle("bayern", "拜仁慕尼黑", "德甲", 6, "拜仁"),
        DqdCircle("chelsea", "切尔西", "英超", 3, "切尔西"),
        DqdCircle("china", "中国男足", "国足", 56, "国足"),
        DqdCircle("csl", "中超专区", "国内", 56, ""),
        DqdCircle("girls", "懂球妹", "社区", 37, ""),
        DqdCircle("gear", "足球装备", "装备", 68, ""),
        DqdCircle("deep", "深度专栏", "深度", 55, "")
    )

    // 官方联赛数据配置（覆盖欧冠、欧联、五大联赛、中超、篮球）
    val LEAGUES = listOf(
        DqdLeague(
            id = "ucl",
            name = "欧冠",
            category = "欧洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=10&league=%E6%AC%A7%E5%86%A0",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=371&league=%E6%AC%A7%E6%B4%B2%E5%86%A0%E5%86%9B%E8%81%94%E8%B5%9B",
            assistsUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E5%8A%A9%E6%94%BB&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=371&league=%E6%AC%A7%E6%B4%B2%E5%86%A0%E5%86%9B%E8%81%94%E8%B5%9B"
        ),
        DqdLeague(
            id = "epl",
            name = "英超",
            category = "欧洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=24&league=%E8%8B%B1%E8%B6%85",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=24&league=%E8%8B%B1%E8%B6%85",
            assistsUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E5%8A%A9%E6%94%BB&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=24&league=%E8%8B%B1%E8%B6%85"
        ),
        DqdLeague(
            id = "laliga",
            name = "西甲",
            category = "欧洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=25&league=%E8%A5%BF%E7%94%B2",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=25&league=%E8%A5%BF%E7%94%B2",
            assistsUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E5%8A%A9%E6%94%BB&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=25&league=%E8%A5%BF%E7%94%B2"
        ),
        DqdLeague(
            id = "seriea",
            name = "意甲",
            category = "欧洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=26&league=%E6%84%8F%E7%94%B2",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=26&league=%E6%84%8F%E7%94%B2",
            assistsUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E5%8A%A9%E6%94%BB&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=26&league=%E6%84%8F%E7%94%B2"
        ),
        DqdLeague(
            id = "bundesliga",
            name = "德甲",
            category = "欧洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=27&league=%E5%BE%B7%E7%94%B2",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=27&league=%E5%BE%B7%E7%94%B2",
            assistsUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E5%8A%A9%E6%94%BB&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=27&league=%E5%BE%B7%E7%94%B2"
        ),
        DqdLeague(
            id = "csl",
            name = "中超",
            category = "亚洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=1&league=%E4%B8%AD%E8%B6%85",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=1&league=%E4%B8%AD%E8%B6%85",
            assistsUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E5%8A%A9%E6%94%BB&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=1&league=%E4%B8%AD%E8%B6%85"
        ),
        DqdLeague(
            id = "ligue1",
            name = "法甲",
            category = "欧洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=28&league=%E6%B3%95%E7%94%B2",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=28&league=%E6%B3%95%E7%94%B2",
            assistsUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E5%8A%A9%E6%94%BB&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=28&league=%E6%B3%95%E7%94%B2"
        ),
        DqdLeague(
            id = "nba",
            name = "NBA",
            category = "篮球",
            standingUrl = "https://stats.qiumibao.com/data/json_v2/nba_paihang.htm"
        ),
        DqdLeague(
            id = "cba",
            name = "CBA",
            category = "篮球",
            standingUrl = "https://stats.qiumibao.com/data/json_v2/cba_paihang.htm"
        )
    )

    suspend fun news(tabId: Int): List<News> = withContext(Dispatchers.IO) {
        val body = get(
            "https://api.dongqiudi.com/app/tabs/iphone/$tabId.json",
            UA,
            "application/json"
        )
        parseNews(body)
    }

    suspend fun circleNews(circle: DqdCircle): List<News> = withContext(Dispatchers.IO) {
        val raw = news(circle.tabId)
        if (circle.tag.isBlank()) {
            raw
        } else {
            val filtered = raw.filter { it.title.contains(circle.tag) || it.category.contains(circle.tag) || it.subCategory.contains(circle.tag) }
            if (filtered.isNotEmpty()) filtered else raw
        }
    }

    suspend fun matches(): List<Match> = withContext(Dispatchers.IO) {
        val html = get("https://m.dongqiudi.com/match", UA_ANDROID, "text/html")
        parseMatches(html)
    }

    suspend fun fetchStandings(league: DqdLeague): List<DqdStandingItem> = withContext(Dispatchers.IO) {
        val raw = get(league.standingUrl, UA_ANDROID, "application/json")
        parseStandings(raw, league.id)
    }

    suspend fun fetchScorers(league: DqdLeague): List<DqdPlayerItem> = withContext(Dispatchers.IO) {
        if (league.scorersUrl.isBlank()) return@withContext emptyList()
        val raw = get(league.scorersUrl, UA_ANDROID, "application/json")
        parsePlayerStats(raw, "进球", "点球")
    }

    suspend fun fetchAssists(league: DqdLeague): List<DqdPlayerItem> = withContext(Dispatchers.IO) {
        if (league.assistsUrl.isBlank()) return@withContext emptyList()
        val raw = get(league.assistsUrl, UA_ANDROID, "application/json")
        parsePlayerStats(raw, "助攻", "")
    }

    private fun parseStandings(body: String, leagueId: String): List<DqdStandingItem> {
        val out = ArrayList<DqdStandingItem>()
        if (leagueId == "nba" || leagueId == "cba") {
            val root = JSONObject(body)
            val east = root.optJSONArray("east")
            val west = root.optJSONArray("west")
            if (east != null) {
                for (i in 0 until east.length()) {
                    val o = east.optJSONObject(i) ?: continue
                    out += DqdStandingItem(
                        rank = o.optString("ranking", "${i + 1}"),
                        teamName = o.optString("name") + " (东部)",
                        teamLogo = normalizeUrl(o.optString("logo")),
                        played = "${o.optInt("win") + o.optInt("loss")}",
                        win = o.optString("win"),
                        draw = o.optString("streak"),
                        loss = o.optString("loss"),
                        goals = o.optString("win_rate"),
                        points = o.optString("games_back", "0")
                    )
                }
            }
            if (west != null) {
                for (i in 0 until west.length()) {
                    val o = west.optJSONObject(i) ?: continue
                    out += DqdStandingItem(
                        rank = o.optString("ranking", "${i + 1}"),
                        teamName = o.optString("name") + " (西部)",
                        teamLogo = normalizeUrl(o.optString("logo")),
                        played = "${o.optInt("win") + o.optInt("loss")}",
                        win = o.optString("win"),
                        draw = o.optString("streak"),
                        loss = o.optString("loss"),
                        goals = o.optString("win_rate"),
                        points = o.optString("games_back", "0")
                    )
                }
            }
            return out
        }

        // 足球积分榜
        val root = JSONObject(body)
        val dataArr = root.optJSONArray("data") ?: return emptyList()
        for (i in 0 until dataArr.length()) {
            val group = dataArr.optJSONObject(i) ?: continue
            val list = group.optJSONArray("list") ?: continue
            for (j in 0 until list.length()) {
                val o = list.optJSONObject(j) ?: continue
                val goalsFor = o.optString("进球", "0")
                val goalsAgainst = o.optString("失球", "0")
                out += DqdStandingItem(
                    rank = o.optString("排名", "${j + 1}"),
                    teamName = o.optString("球队"),
                    teamLogo = normalizeUrl(o.optString("球队图标")),
                    played = o.optString("场次", "0"),
                    win = o.optString("胜", "0"),
                    draw = o.optString("平", "0"),
                    loss = o.optString("负", "0"),
                    goals = "$goalsFor/$goalsAgainst",
                    points = o.optString("积分", "0")
                )
            }
        }
        return out
    }

    private fun parsePlayerStats(body: String, mainKey: String, subKey: String): List<DqdPlayerItem> {
        val root = JSONObject(body)
        val dataArr = root.optJSONArray("data") ?: return emptyList()
        val out = ArrayList<DqdPlayerItem>(dataArr.length())
        for (i in 0 until dataArr.length()) {
            val o = dataArr.optJSONObject(i) ?: continue
            val countVal = o.optString(mainKey).ifBlank { o.optString("count", "0") }
            val subVal = if (subKey.isNotBlank()) o.optString(subKey) else ""
            out += DqdPlayerItem(
                rank = o.optString("排名", "${i + 1}"),
                playerName = o.optString("球员").ifBlank { o.optString("person_name") },
                playerLogo = normalizeUrl(o.optString("球员图标").ifBlank { o.optString("person_logo") }),
                teamName = o.optString("球队").ifBlank { o.optString("team_name") },
                teamLogo = normalizeUrl(o.optString("球队图标").ifBlank { o.optString("team_logo") }),
                count = countVal,
                subCount = if (subVal.isNotBlank() && subVal != "0") "(${subVal}点)" else ""
            )
        }
        return out
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
