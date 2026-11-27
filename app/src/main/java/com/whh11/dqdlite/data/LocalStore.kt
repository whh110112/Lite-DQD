package com.whh11.dqdlite.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色白底"),
    DARK("深色护眼")
}

object LocalStore {
    private const val PREF_NAME = "dqd_lite_v3"
    private var sp: SharedPreferences? = null

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _amoled = MutableStateFlow(false)
    val amoled: StateFlow<Boolean> = _amoled.asStateFlow()

    private val _fontScale = MutableStateFlow(1.0f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    private val _primaryTeamId = MutableStateFlow("50001755")
    val primaryTeamId: StateFlow<String> = _primaryTeamId.asStateFlow()

    private val _followedTeams = MutableStateFlow<List<FollowedTeam>>(defaultTeams())
    val followedTeams: StateFlow<List<FollowedTeam>> = _followedTeams.asStateFlow()

    private val _favorites = MutableStateFlow<List<SavedArticle>>(emptyList())
    val favorites: StateFlow<List<SavedArticle>> = _favorites.asStateFlow()

    private val _history = MutableStateFlow<List<SavedArticle>>(emptyList())
    val history: StateFlow<List<SavedArticle>> = _history.asStateFlow()

    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp = prefs
        _themeMode.value = runCatching {
            ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
        _amoled.value = prefs.getBoolean("amoled", false)
        _fontScale.value = prefs.getFloat("font_scale", 1.0f).coerceIn(0.85f, 1.3f)
        _primaryTeamId.value = prefs.getString("primary_team_id", "50001755") ?: "50001755"
        _followedTeams.value = loadTeams(prefs.getString("followed_teams", null))
        _favorites.value = loadArticles(prefs.getString("favorites", null))
        _history.value = loadArticles(prefs.getString("history", null))
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        sp?.edit()?.putString("theme_mode", mode.name)?.apply()
    }

    fun setAmoled(enabled: Boolean) {
        _amoled.value = enabled
        sp?.edit()?.putBoolean("amoled", enabled)?.apply()
    }

    fun setFontScale(scale: Float) {
        val clamped = scale.coerceIn(0.85f, 1.3f)
        _fontScale.value = clamped
        sp?.edit()?.putFloat("font_scale", clamped)?.apply()
    }

    fun setPrimaryTeam(teamId: String) {
        _primaryTeamId.value = teamId
        sp?.edit()?.putString("primary_team_id", teamId)?.apply()
    }

    fun toggleFollowTeam(team: FollowedTeam) {
        val current = _followedTeams.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.teamId == team.teamId || it.name == team.name }
        if (existingIdx >= 0) {
            current.removeAt(existingIdx)
            if (_primaryTeamId.value == team.teamId) {
                setPrimaryTeam(current.firstOrNull()?.teamId.orEmpty())
            }
        } else {
            current.add(0, team)
            if (_primaryTeamId.value.isBlank()) setPrimaryTeam(team.teamId)
        }
        _followedTeams.value = current
        saveTeams(current)
    }

    fun isTeamFollowed(teamId: String, name: String): Boolean {
        return _followedTeams.value.any { (teamId.isNotBlank() && it.teamId == teamId) || it.name == name }
    }

    fun toggleFavorite(article: SavedArticle): Boolean {
        val current = _favorites.value.toMutableList()
        val idx = current.indexOfFirst { it.id == article.id }
        val nowSaved = if (idx >= 0) {
            current.removeAt(idx)
            false
        } else {
            current.add(0, article.copy(savedAt = System.currentTimeMillis()))
            true
        }
        _favorites.value = current
        saveArticles("favorites", current)
        return nowSaved
    }

    fun isFavorite(articleId: Long): Boolean = _favorites.value.any { it.id == articleId }

    fun recordHistory(article: SavedArticle) {
        if (article.id <= 0L) return
        val current = _history.value.toMutableList()
        current.removeAll { it.id == article.id }
        current.add(0, article.copy(savedAt = System.currentTimeMillis()))
        val trimmed = current.take(100)
        _history.value = trimmed
        saveArticles("history", trimmed)
    }

    fun clearHistory() {
        _history.value = emptyList()
        sp?.edit()?.remove("history")?.apply()
    }

    private fun defaultTeams(): List<FollowedTeam> = listOf(
        FollowedTeam("50001755", "皇家马德里", "https://sd.qunliao.info/fastdfs3/M00/B5/7E/ChOxM1xC2DuAb_47AAAnf1cQ9j0767.png", "西甲"),
        FollowedTeam("50001750", "巴塞罗那", "https://sd.qunliao.info/fastdfs3/M00/B5/7E/ChOxM1xC2DuAYy2PAAAz66V2w88443.png", "西甲"),
        FollowedTeam("50000516", "曼城", "https://sd.qunliao.info/fastdfs3/M00/B5/74/ChOxM1xC2CqAQ8qOAAAy7pH07g4724.png", "英超"),
        FollowedTeam("50000513", "阿森纳", "https://sd.qunliao.info/fastdfs3/M00/B5/74/ChOxM1xC2CmAV1z6AAAw3R9hK6g497.png", "英超"),
        FollowedTeam("50000515", "利物浦", "https://sd.qunliao.info/fastdfs3/M00/B5/74/ChOxM1xC2CqAcF2ZAAAy87V4343011.png", "英超"),
        FollowedTeam("50000433", "中国男足", null, "国家队")
    )

    private fun saveTeams(list: List<FollowedTeam>) {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(JSONObject().apply {
                put("teamId", t.teamId)
                put("name", t.name)
                put("logo", t.logo ?: "")
                put("leagueHint", t.leagueHint)
            })
        }
        sp?.edit()?.putString("followed_teams", arr.toString())?.apply()
    }

    private fun loadTeams(raw: String?): List<FollowedTeam> {
        if (raw.isNullOrBlank()) return defaultTeams()
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return defaultTeams()
        val out = ArrayList<FollowedTeam>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val name = o.optString("name")
            if (name.isBlank()) continue
            out.add(
                FollowedTeam(
                    teamId = o.optString("teamId"),
                    name = name,
                    logo = o.optString("logo").takeIf { it.isNotBlank() },
                    leagueHint = o.optString("leagueHint")
                )
            )
        }
        return out.ifEmpty { defaultTeams() }
    }

    private fun saveArticles(key: String, list: List<SavedArticle>) {
        val arr = JSONArray()
        list.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("title", a.title)
                put("thumb", a.thumb ?: "")
                put("author", a.author)
                put("savedAt", a.savedAt)
            })
        }
        sp?.edit()?.putString(key, arr.toString())?.apply()
    }

    private fun loadArticles(raw: String?): List<SavedArticle> {
        if (raw.isNullOrBlank()) return emptyList()
        val arr = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        val out = ArrayList<SavedArticle>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optLong("id", 0L)
            val title = o.optString("title")
            if (id <= 0L || title.isBlank()) continue
            out.add(
                SavedArticle(
                    id = id,
                    title = title,
                    thumb = o.optString("thumb").takeIf { it.isNotBlank() },
                    author = o.optString("author"),
                    savedAt = o.optLong("savedAt", 0L)
                )
            )
        }
        return out
    }
}
