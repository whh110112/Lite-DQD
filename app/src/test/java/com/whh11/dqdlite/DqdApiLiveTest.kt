package com.whh11.dqdlite

import com.whh11.dqdlite.data.DqdApi
import com.whh11.dqdlite.data.Match
import com.whh11.dqdlite.data.SportCategory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DqdApiLiveTest {

    @Test
    fun testNumericScoreComparison() {
        val m = Match(
            id = "1", teamA = "湖人", teamB = "掘金",
            logoA = null, logoB = null,
            scoreA = "98", scoreB = "105",
            status = "Played", period = "FT",
            competition = "NBA", competitionColor = null,
            startPlay = "2026-11-27 10:00", timeUtc = "10:00"
        )
        assertFalse("98 should NOT beat 105", m.teamAWon)
        assertTrue("105 should beat 98", m.teamBWon)
    }

    @Test
    fun testLiveNewsAndPagination() = runBlocking {
        val page1 = DqdApi.news(1)
        assertTrue("Expected news items", page1.items.isNotEmpty())
        assertNotNull("Expected next pagination url", page1.nextUrl)
        val page2 = DqdApi.news(1, nextUrl = page1.nextUrl)
        assertTrue("Expected page 2 items", page2.items.isNotEmpty())
    }

    @Test
    fun testLiveArticleDetailAndComments() = runBlocking {
        val detail = DqdApi.articleDetail(5000000L)
        assertTrue("Expected title", detail.title.isNotBlank())
        assertTrue("Expected native content blocks", detail.blocks.isNotEmpty())

        val comments = DqdApi.comments(5000000L)
        assertTrue("Expected comments", comments.comments.isNotEmpty() || comments.hotComments.isNotEmpty())
    }

    @Test
    fun testLiveDataMenuStandingsAndAssists() = runBlocking {
        val menu = DqdApi.dataMenu(forceRefresh = true)
        assertTrue("Expected menu groups", menu.isNotEmpty())
        val epl = menu.first().leagues.first()
        val standings = DqdApi.standings(epl.standingUrl)
        assertTrue("Expected EPL standings", standings.isNotEmpty())
        val subTabs = DqdApi.rankingSubTabs(epl.personRankUrl)
        assertTrue("Expected scorer & assist tabs", subTabs.size >= 2)
        val players = DqdApi.personRanking(subTabs[1].url)
        assertTrue("Expected assist ranking rows", players.isNotEmpty())
    }

    @Test
    fun testLiveMatchesAndMatchDetail() = runBlocking {
        val matches = DqdApi.matches(SportCategory.SOCCER)
        assertTrue("Expected soccer matches", matches.isNotEmpty())
        val played = matches.firstOrNull { it.isPlayed } ?: matches.first()
        val detail = DqdApi.matchDetail(played.id)
        assertTrue("Expected team names", detail.teamA.isNotBlank() && detail.teamB.isNotBlank())
        assertTrue("Expected stats or analysis", detail.stats.isNotEmpty() || detail.analysis != null)
        if (detail.stats.isNotEmpty()) {
            assertTrue("Stat values should not be empty", detail.stats.first().valueA.isNotBlank())
        }
    }

    @Test
    fun testLiveTeamAndPlayerDetail() = runBlocking {
        val td = DqdApi.teamDetail("50001755")
        assertTrue("Expected Real Madrid schedule", td.schedule.isNotEmpty())
        assertTrue("Expected Real Madrid members", td.members.isNotEmpty())

        val firstPlayer = td.members.first { it.positionGroup != "教练" }
        val pd = DqdApi.playerDetail(firstPlayer.personId)
        assertTrue("Expected player name", pd.name.isNotBlank())
        assertTrue("Expected player facts", pd.facts.isNotEmpty())
    }

    @Test
    fun testLiveSearch() = runBlocking {
        val res = DqdApi.search("皇马")
        assertTrue("Expected team search results", res.teams.isNotEmpty())
    }
}
