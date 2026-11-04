package com.whh11.dqdlite.data

data class News(
    val id: Long,
    val title: String,
    val thumb: String?,
    val category: String,
    val subCategory: String,
    val comments: Int,
    val showTime: Long,
    val isVideo: Boolean,
    val isTop: Boolean,
    val author: String,
    val url: String = ""
) {
    /** 懂球帝文章移动端自适应页面，支持完整正文与评论展示 */
    val detailUrl: String
        get() = if (id > 0) "https://m.dongqiudi.com/article/$id.html" else url.ifBlank { "https://m.dongqiudi.com" }
}

enum class SportCategory(val label: String, val apiPath: String) {
    ALL("全部", ""),
    SOCCER("足球", "soccer"),
    BASKETBALL("篮球", "basketball"),
    ESPORTS("电竞", "esport"),
    TENNIS("网球", "tennis"),
    SPORTS("体坛", "sport")
}

/**
 * 一场比赛。字段支持移动版比分页与各项目专用接口。
 */
data class Match(
    val id: String,
    val teamA: String,
    val teamB: String,
    val logoA: String?,
    val logoB: String?,
    val scoreA: String,
    val scoreB: String,
    val status: String,
    val period: String,
    val competition: String,
    val competitionColor: String?,
    val startPlay: String,
    val timeUtc: String,
    val category: String = "足球"
) {
    val isPlayed: Boolean get() = status.equals("Played", ignoreCase = true) || status.contains("完")
    val isLive: Boolean get() = status.equals("Playing", ignoreCase = true) || status.equals("Live", ignoreCase = true) || period.contains("分") || period.contains("节")
    val hasScore: Boolean get() = scoreA.isNotBlank() && scoreB.isNotBlank()

    val centreLabel: String
        get() = if (hasScore) "$scoreA - $scoreB" else timeUtc.ifBlank { startPlay.takeLast(5) }

    val stateLabel: String
        get() = when {
            isLive -> period.ifBlank { "进行中" }
            isPlayed -> "完场"
            else -> if (startPlay.length >= 10) startPlay.substring(5, 10) else "未开赛"
        }

    val detailUrl: String get() = "https://m.dongqiudi.com/match/$id"
}

data class FeedTab(val id: Int, val label: String)

/** 懂球帝社区圈子模型 */
data class DqdCircle(
    val id: String,
    val name: String,
    val category: String,
    val tabId: Int = 1,
    val tag: String = ""
)

/** 懂球帝联赛数据榜单 */
data class DqdLeague(
    val id: String,
    val name: String,
    val category: String,
    val standingUrl: String,
    val scorersUrl: String = "",
    val assistsUrl: String = "",
    val isBasketball: Boolean = false,
    val description: String = "",
    /**
     * 懂球帝官方数据中台的 competition_id。
     * 有值时优先走 sport-data 官方接口（带 team_id，可点进球队页）；
     * 为 0 时退回第三方榜单（无 team_id，球队页降级为「该队新闻」）。
     */
    val competitionId: Int = 0
)

/** 积分榜行数据 */
data class DqdStandingItem(
    val rank: String,
    val teamName: String,
    val teamLogo: String?,
    val played: String,
    val win: String,
    val draw: String,
    val loss: String,
    val goals: String,
    val points: String,
    val winRate: String = "",
    val streak: String = "",
    val gamesBehind: String = "",
    val groupTitle: String = "",
    /** 懂球帝球队 id，用于打开球队详情页；空表示该榜无此数据 */
    val teamId: String = "",
    /** 排名升降：正数上升，负数下降，0 持平，null 未知 */
    val rankDelta: Int? = null
)

/** 球员榜行数据（射手、助攻） */
data class DqdPlayerItem(
    val rank: String,
    val playerName: String,
    val playerLogo: String?,
    val teamName: String,
    val teamLogo: String?,
    val count: String,
    val subCount: String = "",
    /** 球员 id，用于打开球员详情页 */
    val playerId: String = "",
    /** 所属球队 id，用于从球员页跳回球队 */
    val teamId: String = ""
)
