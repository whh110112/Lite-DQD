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

/**
 * 一场比赛。字段直接对齐懂球帝 matchListStore.matchList 的结构。
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
    val timeUtc: String
) {
    val isPlayed: Boolean get() = status == "Played"
    val isLive: Boolean get() = status == "Playing" || status == "Live"
    val hasScore: Boolean get() = scoreA.isNotBlank() && scoreB.isNotBlank()

    val centreLabel: String
        get() = if (hasScore) "$scoreA - $scoreB" else timeUtc

    val stateLabel: String
        get() = when {
            isLive -> period.ifBlank { "进行中" }
            isPlayed -> "完场"
            else -> startPlay.take(10)
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
    val assistsUrl: String = ""
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
    val points: String
)

/** 球员榜行数据（射手、助攻） */
data class DqdPlayerItem(
    val rank: String,
    val playerName: String,
    val playerLogo: String?,
    val teamName: String,
    val teamLogo: String?,
    val count: String,
    val subCount: String = ""
)
