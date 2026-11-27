package com.whh11.dqdlite.data

data class FeedTab(val id: Int, val label: String)

data class News(
    val id: Long,
    val title: String,
    val thumb: String?,
    val category: String,
    val subCategory: String,
    val comments: Int,
    val showTime: Long,
    val sortTimestamp: Long = showTime,
    val isVideo: Boolean,
    val isTop: Boolean,
    val author: String,
    val url: String = ""
) {
    val webUrl: String
        get() = if (id > 0) "https://m.dongqiudi.com/article/$id.html" else url.ifBlank { "https://m.dongqiudi.com" }
}

data class NewsPage(
    val items: List<News>,
    val nextUrl: String?
)

sealed class ContentBlock {
    data class Heading(val text: String) : ContentBlock()
    data class Paragraph(val text: String) : ContentBlock()
    data class Quote(val text: String) : ContentBlock()
    data class Image(val url: String, val caption: String = "") : ContentBlock()
}

data class ArticleDetail(
    val id: Long,
    val title: String,
    val writer: String,
    val time: String,
    val thumb: String?,
    val blocks: List<ContentBlock>,
    val imageUrls: List<String>,
    val videoUrl: String? = null,
    val webUrl: String = "https://m.dongqiudi.com/article/$id.html"
)

data class CommentItem(
    val id: String,
    val userName: String,
    val userAvatar: String?,
    val content: String,
    val time: String,
    val upCount: Int,
    val replyCount: Int,
    val isHot: Boolean = false,
    val quoteUser: String = "",
    val quoteContent: String = ""
)

data class CommentPage(
    val hotComments: List<CommentItem>,
    val comments: List<CommentItem>,
    val total: Int,
    val nextUrl: String?
)

enum class SportCategory(val label: String, val apiPath: String) {
    ALL("全部", ""),
    SOCCER("足球", "soccer"),
    BASKETBALL("篮球", "basketball"),
    ESPORTS("电竞", "esport"),
    TENNIS("网球", "tennis"),
    SPORTS("体坛", "sport")
}

data class Match(
    val id: String,
    val teamAId: String = "",
    val teamBId: String = "",
    val teamA: String,
    val teamB: String,
    val logoA: String?,
    val logoB: String?,
    val scoreA: String,
    val scoreB: String,
    val halfScoreA: String = "",
    val halfScoreB: String = "",
    val status: String,
    val period: String,
    val competition: String,
    val competitionColor: String?,
    val startPlay: String,
    val timeUtc: String,
    val category: String = "足球"
) {
    val isPlayed: Boolean
        get() = status.equals("Played", ignoreCase = true) || status.contains("完")
    val isLive: Boolean
        get() = status.equals("Playing", ignoreCase = true) ||
            status.equals("Live", ignoreCase = true) ||
            period.contains("分") || period.contains("节") ||
            (period.isNotBlank() && period != "FT" && !isPlayed && hasScore)
    val hasScore: Boolean get() = scoreA.isNotBlank() && scoreB.isNotBlank()

    val scoreAInt: Int? get() = scoreA.trim().toIntOrNull()
    val scoreBInt: Int? get() = scoreB.trim().toIntOrNull()
    val teamAWon: Boolean get() = hasScore && (scoreAInt ?: -1) > (scoreBInt ?: -1)
    val teamBWon: Boolean get() = hasScore && (scoreBInt ?: -1) > (scoreAInt ?: -1)

    val dateGroup: String
        get() = if (startPlay.length >= 10) startPlay.substring(0, 10) else "近期赛程"

    val timeLabel: String
        get() = when {
            startPlay.length >= 16 -> startPlay.substring(11, 16)
            timeUtc.length >= 5 -> timeUtc.take(5)
            else -> ""
        }

    val centreLabel: String
        get() = if (hasScore) "$scoreA - $scoreB" else timeLabel.ifBlank { "VS" }

    val stateLabel: String
        get() = when {
            isLive -> period.ifBlank { "进行中" }
            isPlayed -> if (halfScoreA.isNotBlank() && halfScoreB.isNotBlank()) "完场 ($halfScoreA-$halfScoreB)" else "完场"
            else -> "未开赛"
        }

    val webUrl: String get() = "https://m.dongqiudi.com/match/$id"
}

data class MatchEvent(
    val minute: String,
    val isTeamA: Boolean,
    val playerName: String,
    val iconUrl: String?,
    val typeText: String
)

data class MatchStat(
    val label: String,
    val valueA: String,
    val valueB: String,
    val ratioA: Float
)

data class LineupPlayer(
    val id: String,
    val name: String,
    val number: String,
    val position: String,
    val avatar: String?,
    val isCaptain: Boolean = false
)

data class MatchDetail(
    val matchId: String,
    val competition: String,
    val status: String,
    val period: String,
    val startTime: String,
    val teamA: String,
    val teamB: String,
    val teamAId: String = "",
    val teamBId: String = "",
    val logoA: String?,
    val logoB: String?,
    val scoreA: String,
    val scoreB: String,
    val venue: String = "",
    val weather: String = "",
    val referee: String = "",
    val formationA: String = "",
    val formationB: String = "",
    val events: List<MatchEvent> = emptyList(),
    val stats: List<MatchStat> = emptyList(),
    val lineupA: List<LineupPlayer> = emptyList(),
    val lineupB: List<LineupPlayer> = emptyList(),
    val subsA: List<LineupPlayer> = emptyList(),
    val subsB: List<LineupPlayer> = emptyList(),
    val analysis: MatchAnalysis? = null
)

data class AnalysisRecordItem(
    val date: String,
    val competition: String,
    val teamA: String,
    val teamB: String,
    val scoreA: String,
    val scoreB: String
)

data class AnalysisTableSide(
    val rank: String,
    val played: String,
    val win: String,
    val draw: String,
    val loss: String,
    val points: String,
    val winRate: String = ""
)

data class SidelinePlayer(
    val name: String,
    val reason: String,
    val logo: String? = null
)

data class MatchAnalysis(
    val battleSummary: String = "",
    val battleHistory: List<AnalysisRecordItem> = emptyList(),
    val recentA: List<AnalysisRecordItem> = emptyList(),
    val recentB: List<AnalysisRecordItem> = emptyList(),
    val tableA: AnalysisTableSide? = null,
    val tableB: AnalysisTableSide? = null,
    val sidelineA: List<SidelinePlayer> = emptyList(),
    val sidelineB: List<SidelinePlayer> = emptyList()
)

data class TeamFact(val label: String, val value: String)
data class TeamHonor(val name: String, val times: String, val logo: String? = null)

data class TeamMemberItem(
    val personId: String,
    val name: String,
    val number: String,
    val positionGroup: String,
    val avatar: String?,
    val goals: String = "",
    val appearances: String = ""
)

data class TeamDetail(
    val teamId: String,
    val name: String,
    val enName: String,
    val logo: String?,
    val facts: List<TeamFact>,
    val honors: List<TeamHonor>,
    val members: List<TeamMemberItem>,
    val schedule: List<Match>
)

data class PlayerCareerItem(
    val teamId: String,
    val teamName: String,
    val teamLogo: String?,
    val startDate: String,
    val endDate: String
)

data class PlayerTransferItem(
    val date: String,
    val fromClub: String,
    val toClub: String,
    val fee: String
)

data class PlayerInjuryItem(
    val injury: String,
    val dateFrom: String,
    val dateUntil: String,
    val days: String
)

data class PlayerDetail(
    val personId: String,
    val name: String,
    val enName: String,
    val avatar: String?,
    val teamId: String = "",
    val teamName: String = "",
    val facts: List<TeamFact>,
    val styles: List<String>,
    val strengths: List<String>,
    val honors: List<TeamHonor>,
    val careers: List<PlayerCareerItem>,
    val transfers: List<PlayerTransferItem>,
    val injuries: List<PlayerInjuryItem>
)

data class MenuLeague(
    val id: String,
    val label: String,
    val logo: String?,
    val competitionId: String,
    val seasonId: String,
    val seasonTitle: String,
    val standingUrl: String,
    val personRankUrl: String,
    val teamRankUrl: String,
    val scheduleUrl: String
)

data class DataMenuGroup(
    val title: String,
    val leagues: List<MenuLeague>
)

data class StandingRow(
    val rank: Int,
    val teamId: String,
    val teamName: String,
    val teamLogo: String?,
    val played: String,
    val win: String,
    val draw: String,
    val loss: String,
    val goalsFor: String,
    val goalsAgainst: String,
    val points: String,
    val groupTitle: String = "",
    val zoneColor: String? = null,
    val rankDelta: Int? = null
)

data class RankSubTab(
    val name: String,
    val type: String,
    val url: String
)

data class PlayerRankRow(
    val rank: Int,
    val playerId: String,
    val playerName: String,
    val playerLogo: String?,
    val teamId: String,
    val teamName: String,
    val teamLogo: String?,
    val count: String
)

data class SearchTeam(
    val teamId: String,
    val name: String,
    val enName: String,
    val logo: String?
)

data class SearchPlayer(
    val playerId: String,
    val name: String,
    val teamName: String,
    val logo: String?
)

data class SearchResult(
    val teams: List<SearchTeam>,
    val players: List<SearchPlayer>,
    val news: List<News>
)

data class FollowedTeam(
    val teamId: String,
    val name: String,
    val logo: String? = null,
    val leagueHint: String = ""
)

data class SavedArticle(
    val id: Long,
    val title: String,
    val thumb: String?,
    val author: String = "",
    val savedAt: Long = System.currentTimeMillis()
)
