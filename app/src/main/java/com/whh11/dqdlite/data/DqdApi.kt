package com.whh11.dqdlite.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 懂球帝数据层（全品类增强版）。
 * 1. 资讯：提供头条、足球、篮球、体坛、电竞、全球五洲、中超、中甲、五大联赛等16个核心分类；
 * 2. 赛程：支持全部、足球、篮球、电竞、网球、体坛全项目对阵、比分与实时状态；
 * 3. 圈子：主队与热门专区聚合；
 * 4. 联赛数据：覆盖欧洲足球、亚洲足球、美洲足球、非洲足球、女子足球、全民足球、篮球、电竞、体坛等全项目积分榜与球员榜。
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
        FeedTab(247, "篮球"),
        FeedTab(248, "体坛"),
        FeedTab(249, "电竞"),
        FeedTab(57, "全球五洲"),
        FeedTab(56, "中超"),
        FeedTab(241, "中甲"),
        FeedTab(37, "女足"),
        FeedTab(10344, "全民足球"),
        FeedTab(3, "英超"),
        FeedTab(5, "西甲"),
        FeedTab(4, "意甲"),
        FeedTab(6, "德甲"),
        FeedTab(12, "法甲"),
        FeedTab(55, "深度"),
        FeedTab(284, "快讯"),
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
        DqdCircle("nba", "NBA专区", "篮球", 247, "NBA"),
        DqdCircle("esport", "电竞圈", "电竞", 249, ""),
        DqdCircle("sports", "体坛风暴", "体坛", 248, ""),
        DqdCircle("girls", "懂球妹", "社区", 37, ""),
        DqdCircle("gear", "足球装备", "装备", 68, ""),
        DqdCircle("deep", "深度专栏", "深度", 55, "")
    )

    // 全项目联赛数据配置（覆盖欧洲、亚洲、美洲、非洲、女足、全民足球、篮球、电竞、体坛）
    val LEAGUES = listOf(
        // 1. 欧洲足球
        DqdLeague(
            id = "ucl",
            name = "欧冠",
            category = "欧洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=371&league=%E6%AC%A7%E6%B4%B2%E5%86%A0%E5%86%9B%E8%81%94%E8%B5%9B",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=371&league=%E6%AC%A7%E6%B4%B2%E5%86%A0%E5%86%9B%E8%81%94%E8%B5%9B"
        ),
        DqdLeague(
            id = "epl",
            name = "英超",
            category = "欧洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=24&league=%E8%8B%B1%E8%B6%85",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=24&league=%E8%8B%B1%E8%B6%85"
        ),
        DqdLeague(
            id = "laliga",
            name = "西甲",
            category = "欧洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=25&league=%E8%A5%BF%E7%94%B2",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=25&league=%E8%A5%BF%E7%94%B2"
        ),
        DqdLeague(
            id = "seriea",
            name = "意甲",
            category = "欧洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=26&league=%E6%84%8F%E7%94%B2",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=26&league=%E6%84%8F%E7%94%B2"
        ),
        DqdLeague(
            id = "bundesliga",
            name = "德甲",
            category = "欧洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=27&league=%E5%BE%B7%E7%94%B2",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=27&league=%E5%BE%B7%E7%94%B2"
        ),
        DqdLeague(
            id = "ligue1",
            name = "法甲",
            category = "欧洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=28&league=%E6%B3%95%E7%94%B2",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=28&league=%E6%B3%95%E7%94%B2"
        ),
        DqdLeague(
            id = "uel",
            name = "欧联杯",
            category = "欧洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=14&league=%E6%AC%A7%E8%81%94%E6%9D%AF"
        ),

        // 2. 亚洲足球
        DqdLeague(
            id = "csl",
            name = "中超",
            category = "亚洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=1&league=%E4%B8%AD%E8%B6%85",
            scorersUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BF%9B%E7%90%83&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=1&league=%E4%B8%AD%E8%B6%85"
        ),
        DqdLeague(
            id = "acl",
            name = "亚冠精英",
            category = "亚洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=226&league=%E4%BA%9A%E5%86%A0%E7%B2%BE%E8%8B%B1"
        ),
        DqdLeague(
            id = "saudi",
            name = "沙特联",
            category = "亚洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=318&league=%E6%B2%99%E7%89%B9%E8%B6%85"
        ),
        DqdLeague(
            id = "cl1",
            name = "中甲",
            category = "亚洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=129&league=%E4%B8%AD%E7%94%B2"
        ),
        DqdLeague(
            id = "aleague",
            name = "澳超",
            category = "亚洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=258&league=%E6%BE%B3A%E8%81%94"
        ),

        // 3. 美洲足球
        DqdLeague(
            id = "mls",
            name = "美职联",
            category = "美洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=244&league=%E7%BE%8E%E8%81%8C%E8%81%94",
            isBasketball = true // 美职联东西部分区
        ),
        DqdLeague(
            id = "libertadores",
            name = "解放者杯",
            category = "美洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=223&league=%E8%A7%A3%E6%94%BE%E8%80%85%E6%9D%AF"
        ),
        DqdLeague(
            id = "brasileirao",
            name = "巴甲",
            category = "美洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=219&league=%E5%B7%B4%E7%94%B2"
        ),
        DqdLeague(
            id = "arg",
            name = "阿超",
            category = "美洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=213&league=%E9%98%BF%E7%94%B2"
        ),

        // 4. 非洲足球
        DqdLeague(
            id = "afcon",
            name = "非洲杯",
            category = "非洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=385&league=%E9%9D%9E%E6%B4%B2%E6%9D%AF"
        ),
        DqdLeague(
            id = "cafcl",
            name = "非冠杯",
            category = "非洲足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=386&league=%E9%9D%9E%E5%86%A0%E6%9D%AF"
        ),

        // 5. 女子足球
        DqdLeague(
            id = "cswl",
            name = "中女超",
            category = "女子足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=357&league=%E5%A5%B3%E8%B6%85%E8%81%94%E8%B5%9B"
        ),
        DqdLeague(
            id = "uwcl",
            name = "女足欧冠",
            category = "女子足球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%A7%AF%E5%88%86%E6%A6%9C&league_id=380&league=%E5%A5%B3%E8%B6%B3%E6%AC%A7%E5%86%A0"
        ),

        // 6. 全民足球（省超联赛系列）
        DqdLeague(
            id = "yuechao",
            name = "粤超联赛",
            category = "全民足球",
            standingUrl = "https://api.dongqiudi.com/data/tab/league/new/10344",
            description = "广东省全民足球联赛"
        ),
        DqdLeague(
            id = "qlchao",
            name = "齐鲁超",
            category = "全民足球",
            standingUrl = "https://api.dongqiudi.com/data/tab/league/new/9687",
            description = "山东省齐鲁超级联赛"
        ),
        DqdLeague(
            id = "chuanchao",
            name = "川超联赛",
            category = "全民足球",
            standingUrl = "https://api.dongqiudi.com/data/tab/league/new/9930",
            description = "四川省超级联赛"
        ),
        DqdLeague(
            id = "ganchao",
            name = "赣超联赛",
            category = "全民足球",
            standingUrl = "https://api.dongqiudi.com/data/tab/league/new/8817",
            description = "江西省超级联赛"
        ),
        DqdLeague(
            id = "chuchao",
            name = "楚超联赛",
            category = "全民足球",
            standingUrl = "https://api.dongqiudi.com/data/tab/league/new/10288",
            description = "湖北省楚天超级联赛"
        ),
        DqdLeague(
            id = "wuyue",
            name = "吴越杯",
            category = "全民足球",
            standingUrl = "https://api.dongqiudi.com/data/tab/league/new/10284",
            description = "长三角吴越杯足球赛"
        ),

        // 7. 篮球项目
        DqdLeague(
            id = "nba",
            name = "NBA",
            category = "篮球",
            standingUrl = "https://stats.qiumibao.com/data/json_v2/nba_paihang.htm",
            isBasketball = true
        ),
        DqdLeague(
            id = "cba",
            name = "CBA",
            category = "篮球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E6%8E%92%E8%A1%8C&tab=%E6%8E%92%E8%A1%8C&league_id=925&league=CBA",
            isBasketball = true
        ),

        // 8. 电竞项目
        DqdLeague(
            id = "s_wc",
            name = "S赛 (LOL)",
            category = "电竞",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E6%8E%92%E8%A1%8C&tab=%E6%8E%92%E8%A1%8C&league_id=3534",
            description = "英雄联盟全球总决赛"
        ),
        DqdLeague(
            id = "kpl",
            name = "KPL (王者)",
            category = "电竞",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E6%8E%92%E8%A1%8C&tab=%E6%8E%92%E8%A1%8C&league_id=3538",
            description = "王者荣耀职业联赛"
        ),
        DqdLeague(
            id = "ti_dota",
            name = "TI (DOTA2)",
            category = "电竞",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E5%8F%82%E8%B5%9B%E9%98%9F%E4%BC%8D&tab=%E7%90%83%E9%98%9F%E6%A6%9C&league_id=3536",
            description = "DOTA2 国际邀请赛"
        ),

        // 9. 体坛与网球
        DqdLeague(
            id = "f1",
            name = "F1",
            category = "体坛与网球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E8%BD%A6%E6%89%8B%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=3132&league=F1",
            description = "F1 世界一级方程式锦标赛"
        ),
        DqdLeague(
            id = "atp",
            name = "ATP网球",
            category = "体坛与网球",
            standingUrl = "https://stats.qiumibao.com/shuju/public/index.php?_url=/data/index&year=&type=%E7%A7%AF%E5%88%86%E6%A6%9C&tab=%E7%90%83%E5%91%98%E6%A6%9C&league_id=3135&league=ATP",
            description = "ATP 世界职业网球巡回赛"
        )
    )

    /**
     * 资讯列表：支持下拉与上滑分页
     */
    suspend fun news(tabId: Int, before: Long = 0): List<News> = withContext(Dispatchers.IO) {
        val url = if (before > 0) {
            "https://api.dongqiudi.com/app/tabs/iphone/$tabId.json?mark=gif&version=576&before=$before"
        } else {
            "https://api.dongqiudi.com/app/tabs/iphone/$tabId.json?mark=gif&version=576"
        }
        val req = Request.Builder().url(url).header("User-Agent", UA).build()
        val res = client.newCall(req).execute()
        val body = res.body?.string().orEmpty()
        parseNewsJson(body)
    }

    /**
     * 圈子讨论/专属新闻列表
     */
    suspend fun circleNews(circle: DqdCircle): List<News> = withContext(Dispatchers.IO) {
        val tabId = if (circle.tabId > 0) circle.tabId else 1
        val list = news(tabId)
        if (circle.tag.isNotBlank()) {
            val filtered = list.filter { it.title.contains(circle.tag) || it.category.contains(circle.tag) }
            if (filtered.isNotEmpty()) return@withContext filtered
        }
        list
    }

    private fun parseNewsJson(json: String): List<News> {
        val out = ArrayList<News>()
        if (json.isBlank()) return out
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return out
        val list = root.optJSONArray("articles") ?: root.optJSONArray("data") ?: return out

        for (i in 0 until list.length()) {
            val o = list.optJSONObject(i) ?: continue
            val title = o.optString("title").ifBlank { o.optString("share_title") }
            if (title.isBlank()) continue

            val isVideo = o.optBoolean("is_video") ||
                o.optString("showtype") == "video" ||
                o.optInt("has_video") == 1

            val authorObj = o.optJSONObject("author")
            val author = authorObj?.optString("username")
                ?: authorObj?.optString("name")
                ?: o.optString("writer")

            out.add(
                News(
                    id = o.optLong("id", 0L),
                    title = title.trim(),
                    thumb = o.optString("thumb").takeIf { it.isNotBlank() },
                    category = o.optString("category"),
                    subCategory = o.optString("secondary_category"),
                    comments = o.optInt("comments_total", 0),
                    showTime = o.optLong("show_time", o.optLong("published_at", 0L)),
                    isVideo = isVideo,
                    isTop = o.optInt("top", 0) == 1,
                    author = author.trim(),
                    url = o.optString("url")
                )
            )
        }
        return out
    }

    /**
     * 赛程获取：支持全品类（全部 / 足球 / 篮球 / 电竞 / 网球 / 体坛）
     */
    suspend fun matches(category: SportCategory = SportCategory.ALL): List<Match> = withContext(Dispatchers.IO) {
        if (category == SportCategory.ALL) {
            // 从移动版比分页 SSR Pinia store 提取 100 场全网对阵
            val req = Request.Builder()
                .url("https://m.dongqiudi.com/match")
                .header("User-Agent", UA_ANDROID)
                .build()
            val res = client.newCall(req).execute()
            val html = res.body?.string().orEmpty()
            val parsed = parseMatchesFromHtml(html)
            if (parsed.isNotEmpty()) return@withContext parsed
        }

        // 专有分类接口
        val apiPath = if (category == SportCategory.ALL) "soccer" else category.apiPath
        val url = "https://api.dongqiudi.com/data/tab/new/$apiPath"
        val req = Request.Builder().url(url).header("User-Agent", UA).build()
        val res = client.newCall(req).execute()
        val json = res.body?.string().orEmpty()
        parseMatchesFromJson(json, category.label)
    }

    private fun parseMatchesFromJson(json: String, categoryName: String): List<Match> {
        val out = ArrayList<Match>()
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return out
        val list = root.optJSONArray("list") ?: return out

        for (i in 0 until list.length()) {
            val o = list.optJSONObject(i) ?: continue
            val teamA = o.optString("team_A_name").trim()
            val teamB = o.optString("team_B_name").trim()
            if (teamA.isBlank() || teamB.isBlank()) continue

            val matchId = o.optString("match_id", o.optString("relate_id", i.toString()))
            val scoreA = o.optString("fs_A")
            val scoreB = o.optString("fs_B")
            val status = o.optString("status")
            val period = o.optString("minute_period")
            val comp = o.optString("competition_name").ifBlank { o.optString("competition").ifBlank { categoryName } }

            out.add(
                Match(
                    id = matchId,
                    teamA = teamA,
                    teamB = teamB,
                    logoA = o.optString("team_A_logo").takeIf { it.isNotBlank() },
                    logoB = o.optString("team_B_logo").takeIf { it.isNotBlank() },
                    scoreA = scoreA,
                    scoreB = scoreB,
                    status = status,
                    period = period,
                    competition = comp,
                    competitionColor = o.optString("competition_color").takeIf { it.isNotBlank() },
                    startPlay = o.optString("start_play").ifBlank { o.optString("date_utc") },
                    timeUtc = o.optString("time_utc"),
                    category = categoryName
                )
            )
        }
        return out
    }

    private fun parseMatchesFromHtml(html: String): List<Match> {
        val out = ArrayList<Match>()
        val key = "\"matchListStore\":{\"matchList\":["
        val startIdx = html.indexOf(key)
        if (startIdx < 0) return out

        val arrayStart = startIdx + key.length - 1
        val jsonArrayStr = extractJsonArray(html, arrayStart) ?: return out
        val arr = runCatching { JSONArray(jsonArrayStr) }.getOrNull() ?: return out

        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val teamA = o.optString("team_A_name").trim()
            val teamB = o.optString("team_B_name").trim()
            if (teamA.isBlank() || teamB.isBlank()) continue

            out.add(
                Match(
                    id = o.optString("match_id", i.toString()),
                    teamA = teamA,
                    teamB = teamB,
                    logoA = o.optString("team_A_logo").takeIf { it.isNotBlank() },
                    logoB = o.optString("team_B_logo").takeIf { it.isNotBlank() },
                    scoreA = o.optString("fs_A"),
                    scoreB = o.optString("fs_B"),
                    status = o.optString("status"),
                    period = o.optString("minute_period"),
                    competition = o.optString("competition_name").ifBlank { "赛事" },
                    competitionColor = o.optString("competition_color").takeIf { it.isNotBlank() },
                    startPlay = o.optString("start_play"),
                    timeUtc = o.optString("time_utc")
                )
            )
        }
        return out
    }

    private fun extractJsonArray(s: String, startIdx: Int): String? {
        if (startIdx >= s.length || s[startIdx] != '[') return null
        var depth = 0
        var inStr = false
        var esc = false
        for (i in startIdx until s.length) {
            val c = s[i]
            if (esc) {
                esc = false
                continue
            }
            if (c == '\\') {
                esc = true
                continue
            }
            if (c == '"') {
                inStr = !inStr
                continue
            }
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

    /**
     * 联赛积分榜/排名数据获取
     */
    suspend fun standings(league: DqdLeague): List<DqdStandingItem> = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(league.standingUrl).header("User-Agent", UA).build()
        val res = client.newCall(req).execute()
        val json = res.body?.string().orEmpty()
        parseStandingsJson(json, league)
    }

    private fun parseStandingsJson(json: String, league: DqdLeague): List<DqdStandingItem> {
        val out = ArrayList<DqdStandingItem>()
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return out

        // 如果是全民足球（东球帝原生接口）
        if (league.standingUrl.contains("api.dongqiudi.com/data/tab/league/new/")) {
            val list = root.optJSONArray("list") ?: return out
            for (i in 0 until list.length()) {
                val o = list.optJSONObject(i) ?: continue
                val teamA = o.optString("team_A_name").trim()
                val teamB = o.optString("team_B_name").trim()
                if (teamA.isBlank()) continue
                out.add(
                    DqdStandingItem(
                        rank = (i + 1).toString(),
                        teamName = "$teamA vs $teamB",
                        teamLogo = o.optString("team_A_logo"),
                        played = o.optString("date_utc"),
                        win = o.optString("fs_A").ifBlank { "-" },
                        draw = ":",
                        loss = o.optString("fs_B").ifBlank { "-" },
                        goals = o.optString("status"),
                        points = o.optString("minute_period").ifBlank { "赛程" }
                    )
                )
            }
            return out
        }

        if (league.isBasketball) {
            // 篮球 NBA/CBA 或 美职联 分区模式
            val dataArr = root.optJSONArray("data") ?: return out
            for (i in 0 until dataArr.length()) {
                val g = dataArr.optJSONObject(i) ?: continue
                val gTitle = g.optString("title").ifBlank { if (i == 0) "西部" else "东部" }
                val lArr = g.optJSONArray("list") ?: continue
                for (j in 0 until lArr.length()) {
                    val o = lArr.optJSONObject(j) ?: continue
                    out.add(
                        DqdStandingItem(
                            rank = o.optString("排名", (j + 1).toString()),
                            teamName = o.optString("球队"),
                            teamLogo = o.optString("球队图标"),
                            played = o.optString("场数", o.optString("场次")),
                            win = o.optString("胜"),
                            draw = o.optString("平", "-"),
                            loss = o.optString("负"),
                            goals = o.optString("近况", o.optString("胜差")),
                            points = o.optString("积分", o.optString("胜率")),
                            winRate = o.optString("胜率"),
                            streak = o.optString("近况"),
                            gamesBehind = o.optString("胜差"),
                            groupTitle = gTitle
                        )
                    )
                }
            }
            return out
        }

        // 常规足球积分榜
        val dataArr = root.optJSONArray("data") ?: return out
        if (dataArr.length() > 0 && dataArr.optJSONObject(0)?.has("list") == true) {
            for (i in 0 until dataArr.length()) {
                val g = dataArr.optJSONObject(i) ?: continue
                val gTitle = g.optString("title")
                val lArr = g.optJSONArray("list") ?: continue
                for (j in 0 until lArr.length()) {
                    val o = lArr.optJSONObject(j) ?: continue
                    out.add(parseFootballRow(o, j + 1, gTitle))
                }
            }
        } else {
            for (i in 0 until dataArr.length()) {
                val o = dataArr.optJSONObject(i) ?: continue
                out.add(parseFootballRow(o, i + 1, ""))
            }
        }
        return out
    }

    private fun parseFootballRow(o: JSONObject, fallbackRank: Int, groupTitle: String): DqdStandingItem {
        val rank = o.optString("排名", fallbackRank.toString())
        val teamName = o.optString("球队名称").ifBlank { o.optString("球队") }.trim()
        return DqdStandingItem(
            rank = rank,
            teamName = teamName,
            teamLogo = o.optString("球队图标"),
            played = o.optString("场次"),
            win = o.optString("胜"),
            draw = o.optString("平"),
            loss = o.optString("负"),
            goals = o.optString("净胜球"),
            points = o.optString("积分"),
            groupTitle = groupTitle
        )
    }

    /**
     * 球员榜单数据（射手、助攻）
     */
    suspend fun players(url: String): List<DqdPlayerItem> = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext emptyList()
        val req = Request.Builder().url(url).header("User-Agent", UA).build()
        val res = client.newCall(req).execute()
        val json = res.body?.string().orEmpty()
        val out = ArrayList<DqdPlayerItem>()
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return@withContext out
        val dataArr = root.optJSONArray("data") ?: return@withContext out

        for (i in 0 until dataArr.length()) {
            val o = dataArr.optJSONObject(i) ?: continue
            val playerName = o.optString("球员").ifBlank { o.optString("player_name") }
            if (playerName.isBlank()) continue

            out.add(
                DqdPlayerItem(
                    rank = o.optString("排名", (i + 1).toString()),
                    playerName = playerName.trim(),
                    playerLogo = o.optString("球员头像").takeIf { it.isNotBlank() },
                    teamName = o.optString("球队").trim(),
                    teamLogo = o.optString("球队图标").takeIf { it.isNotBlank() },
                    count = o.optString("进球", o.optString("助攻", o.optString("数值"))),
                    subCount = o.optString("点球").takeIf { it.isNotBlank() }?.let { "点球 $it" } ?: ""
                )
            )
        }
        out
    }
}
