

package com.edusolve.com.project.content.main

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.edusolve.com.project.content.ThemeSetting
import com.edusolve.com.project.content.about.AboutContent
import com.edusolve.com.project.content.chat.ChatContent
import com.edusolve.com.project.content.history.HistoryContent
import com.edusolve.com.project.content.home.HomeContent
import com.edusolve.com.project.content.images.ImagesContent
import com.edusolve.com.project.content.settings.SettingsContent
import com.edusolve.com.project.ui.Nav
import com.edusolve.com.project.ui.Nav.navigationDestinations
import com.edusolve.com.project.ui.theme.AppTheme

@Composable
internal fun MainContent(
    currentTheme: ThemeSetting
) {
    var theme by remember { mutableStateOf(currentTheme) }

    AppTheme(
        isDarkTheme = isDarkTheme(theme, isSystemInDarkTheme()),
        isDynamicColor = theme == ThemeSetting.System
    ) {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = Nav.Routes.home
        ) {
            composable(Nav.Routes.home) {
                HomeContent(
                    onNavigateTo = { navigationItem ->
                        navigationDestinations[navigationItem]?.let {
                            navController.navigate(it)
                        }
                    })
            }

            composable(
                route = "${Nav.Routes.chat}/{${Nav.Args.historyId}}",
                arguments = listOf(navArgument(Nav.Args.historyId) { type = NavType.LongType })
            ) {
                val id = it.arguments?.getLong(Nav.Args.historyId) ?: -1L
                ChatContent(
                    historyId = id,
                    onBackClick = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate(Nav.Routes.settings) }
                )
            }

            composable(Nav.Routes.settings) {
                SettingsContent(
                    onThemeChanged = { newTheme -> theme = newTheme },
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Nav.Routes.about) {
                AboutContent(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Nav.Routes.history) {
                HistoryContent(
                    onBackClick = { navController.popBackStack() },
                    onItemClick = { historyId -> navController.navigate("${Nav.Routes.chat}/$historyId") }
                )
            }

            composable(Nav.Routes.images) {
                ImagesContent(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}

private fun isDarkTheme(
    themeSetting: ThemeSetting,
    isSystemInDarkTheme: Boolean
): Boolean {
    if (themeSetting == ThemeSetting.Light) return false
    if (themeSetting == ThemeSetting.System) return isSystemInDarkTheme
    return themeSetting == ThemeSetting.Dark
}