package com.whh11.dqdlite

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.whh11.dqdlite.data.LocalStore
import com.whh11.dqdlite.ui.ArticleDetailScreen
import com.whh11.dqdlite.ui.DataTab
import com.whh11.dqdlite.ui.DetailScreen
import com.whh11.dqdlite.ui.DqdBottomBar
import com.whh11.dqdlite.ui.DqdColors
import com.whh11.dqdlite.ui.DqdTheme
import com.whh11.dqdlite.ui.FollowTab
import com.whh11.dqdlite.ui.MatchDetailScreen
import com.whh11.dqdlite.ui.MatchTab
import com.whh11.dqdlite.ui.NewsHomeTab
import com.whh11.dqdlite.ui.PlayerDetailScreen
import com.whh11.dqdlite.ui.ProfileSettingsScreen
import com.whh11.dqdlite.ui.SearchScreen
import com.whh11.dqdlite.ui.TeamDetailScreen
import kotlinx.coroutines.delay
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        LocalStore.init(this)
        setContent {
            DqdTheme {
                DqdAppRoot()
            }
        }
    }
}

private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
private fun dec(s: String?): String = runCatching { URLDecoder.decode(s.orEmpty(), "UTF-8") }.getOrDefault(s.orEmpty())

@Composable
private fun DqdAppRoot() {
    val navController = rememberNavController()
    val context = LocalContext.current

    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var homeScrollTrigger by remember { mutableIntStateOf(0) }
    var matchScrollTrigger by remember { mutableIntStateOf(0) }
    var followScrollTrigger by remember { mutableIntStateOf(0) }
    var dataScrollTrigger by remember { mutableIntStateOf(0) }

    var backPressedOnce by remember { mutableStateOf(false) }
    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            delay(2000)
            backPressedOnce = false
        }
    }

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            BackHandler {
                if (currentTab != 0) {
                    currentTab = 0
                } else if (backPressedOnce) {
                    (context as? Activity)?.finish()
                } else {
                    backPressedOnce = true
                    Toast.makeText(context, "再按一次退出懂球帝 Lite", Toast.LENGTH_SHORT).show()
                }
            }

            Scaffold(
                containerColor = DqdColors.Bg,
                contentWindowInsets = WindowInsets(0),
                bottomBar = {
                    DqdBottomBar(
                        selectedIndex = currentTab,
                        onSelect = { currentTab = it },
                        onReselect = { idx ->
                            when (idx) {
                                0 -> homeScrollTrigger++
                                1 -> matchScrollTrigger++
                                2 -> followScrollTrigger++
                                else -> dataScrollTrigger++
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    when (currentTab) {
                        0 -> NewsHomeTab(
                            scrollToTopTrigger = homeScrollTrigger,
                            onOpenArticle = { news ->
                                if (news.id > 0L) {
                                    navController.navigate("article/${news.id}?title=${enc(news.title)}")
                                } else {
                                    navController.navigate("web?url=${enc(news.webUrl)}&title=${enc(news.title)}")
                                }
                            },
                            onOpenMatch = { mid -> navController.navigate("match/$mid") },
                            onOpenSearch = { navController.navigate("search?q=") },
                            onOpenSettings = { navController.navigate("settings") }
                        )
                        1 -> MatchTab(
                            scrollToTopTrigger = matchScrollTrigger,
                            onOpenMatch = { mid -> navController.navigate("match/$mid") }
                        )
                        2 -> FollowTab(
                            scrollToTopTrigger = followScrollTrigger,
                            onOpenArticle = { news ->
                                if (news.id > 0L) {
                                    navController.navigate("article/${news.id}?title=${enc(news.title)}")
                                } else {
                                    navController.navigate("web?url=${enc(news.webUrl)}&title=${enc(news.title)}")
                                }
                            },
                            onOpenMatch = { mid -> navController.navigate("match/$mid") },
                            onOpenSearch = { navController.navigate("search?q=") }
                        )
                        else -> DataTab(
                            scrollToTopTrigger = dataScrollTrigger,
                            onOpenTeam = { tid, name ->
                                if (tid.isNotBlank()) navController.navigate("team/$tid?name=${enc(name)}")
                                else navController.navigate("search?q=${enc(name)}")
                            },
                            onOpenPlayer = { pid, name ->
                                if (pid.isNotBlank()) navController.navigate("player/$pid?name=${enc(name)}")
                                else navController.navigate("search?q=${enc(name)}")
                            }
                        )
                    }
                }
            }
        }

        composable(
            route = "article/{id}?title={title}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("title") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            val title = dec(entry.arguments?.getString("title"))
            ArticleDetailScreen(
                articleId = id,
                fallbackTitle = title,
                onBack = { navController.popBackStack() },
                onOpenWeb = { url, t -> navController.navigate("web?url=${enc(url)}&title=${enc(t)}") }
            )
        }

        composable(
            route = "match/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            MatchDetailScreen(
                matchId = id,
                onBack = { navController.popBackStack() },
                onOpenWeb = { url, t -> navController.navigate("web?url=${enc(url)}&title=${enc(t)}") },
                onOpenTeam = { tid, name -> navController.navigate("team/$tid?name=${enc(name)}") },
                onOpenPlayer = { pid, name -> navController.navigate("player/$pid?name=${enc(name)}") }
            )
        }

        composable(
            route = "team/{id}?name={name}",
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType; defaultValue = "球队" }
            )
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            val name = dec(entry.arguments?.getString("name")).ifBlank { "球队" }
            TeamDetailScreen(
                teamId = id,
                fallbackName = name,
                onBack = { navController.popBackStack() },
                onOpenMatch = { mid -> navController.navigate("match/$mid") },
                onOpenArticle = { news ->
                    if (news.id > 0L) navController.navigate("article/${news.id}?title=${enc(news.title)}")
                    else navController.navigate("web?url=${enc(news.webUrl)}&title=${enc(news.title)}")
                },
                onOpenPlayer = { pid, pname -> navController.navigate("player/$pid?name=${enc(pname)}") }
            )
        }

        composable(
            route = "player/{id}?name={name}",
            arguments = listOf(
                navArgument("id") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType; defaultValue = "球员" }
            )
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            val name = dec(entry.arguments?.getString("name")).ifBlank { "球员" }
            PlayerDetailScreen(
                personId = id,
                fallbackName = name,
                onBack = { navController.popBackStack() },
                onOpenArticle = { news ->
                    if (news.id > 0L) navController.navigate("article/${news.id}?title=${enc(news.title)}")
                    else navController.navigate("web?url=${enc(news.webUrl)}&title=${enc(news.title)}")
                },
                onOpenMatch = { mid -> navController.navigate("match/$mid") },
                onOpenTeam = { tid, tname -> navController.navigate("team/$tid?name=${enc(tname)}") }
            )
        }

        composable(
            route = "search?q={q}",
            arguments = listOf(navArgument("q") { type = NavType.StringType; defaultValue = "" })
        ) { entry ->
            val q = dec(entry.arguments?.getString("q"))
            SearchScreen(
                initialQuery = q,
                onBack = { navController.popBackStack() },
                onOpenArticle = { news ->
                    if (news.id > 0L) {
                        navController.navigate("article/${news.id}?title=${enc(news.title)}")
                    } else {
                        navController.navigate("web?url=${enc(news.webUrl)}&title=${enc(news.title)}")
                    }
                }
            )
        }

        composable("settings") {
            ProfileSettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenArticle = { id, title ->
                    navController.navigate("article/$id?title=${enc(title)}")
                }
            )
        }

        composable(
            route = "web?url={url}&title={title}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType; defaultValue = "" },
                navArgument("title") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entry ->
            DetailScreen(
                url = dec(entry.arguments?.getString("url")),
                title = dec(entry.arguments?.getString("title")),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
