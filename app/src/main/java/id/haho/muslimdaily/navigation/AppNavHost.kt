package id.haho.muslimdaily.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.PlaceholderScreen

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(navController = navController, startDestination = TodayGraph, modifier = modifier) {
        navigation<TodayGraph>(startDestination = TodayRoute) {
            composable<TodayRoute> { PlaceholderScreen(R.string.nav_today) }
        }
        navigation<DuasGraph>(startDestination = DuaLibraryRoute) {
            composable<DuaLibraryRoute> { PlaceholderScreen(R.string.nav_duas) }
        }
        navigation<MemorizeGraph>(startDestination = MemorizeHomeRoute) {
            composable<MemorizeHomeRoute> { PlaceholderScreen(R.string.nav_memorize) }
        }
        navigation<SettingsGraph>(startDestination = SettingsHomeRoute) {
            composable<SettingsHomeRoute> { PlaceholderScreen(R.string.nav_settings) }
        }
    }
}
