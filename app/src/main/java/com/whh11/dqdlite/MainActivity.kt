package com.whh11.dqdlite

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.whh11.dqdlite.ui.DetailScreen
import com.whh11.dqdlite.ui.DqdColors
import com.whh11.dqdlite.ui.DqdTheme
import com.whh11.dqdlite.ui.HomeScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.whh11.dqdlite.ui.ThemeManager.init(this)
        setContent {
            DqdTheme {
                var detail by remember { mutableStateOf<Pair<String, String>?>(null) }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(DqdColors.Bg)
                ) {
                    val d = detail
                    if (d == null) {
                        HomeScreen(onOpen = { url, title -> detail = url to title })
                    } else {
                        DetailScreen(url = d.first, title = d.second, onBack = { detail = null })
                    }
                }
            }
        }
    }
}
