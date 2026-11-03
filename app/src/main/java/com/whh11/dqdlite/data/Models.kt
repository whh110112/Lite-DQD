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
    /** 优先用接口给的 url（集锦会带 from=tab_xx 参数），没有就按 id 拼 */
    val detailUrl: String
        get() = url.ifBlank { "https://n.dongqiudi.com/webapp/news.html?articleId=$id" }
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

    /** 卡片右侧显示的比分或开赛时间 */
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
