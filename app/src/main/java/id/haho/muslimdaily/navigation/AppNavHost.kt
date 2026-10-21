package id.haho.muslimdaily.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.PlaceholderScreen
import id.haho.muslimdaily.feature.duas.detail.DuaDetailRoute
import id.haho.muslimdaily.feature.duas.event.EventRoute
import id.haho.muslimdaily.feature.duas.library.DuaLibraryRoute
import id.haho.muslimdaily.feature.duas.search.SearchRoute
import id.haho.muslimdaily.feature.settings.PrayerSettingsRoute
import id.haho.muslimdaily.feature.settings.SettingsDestinations
import id.haho.muslimdaily.feature.settings.SettingsHomeRoute
import id.haho.muslimdaily.feature.settings.location.LocationRoute
import id.haho.muslimdaily.feature.today.TodayRoute

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    val back: () -> Unit = { navController.popBackStack() }
    NavHost(navController = navController, startDestination = TodayGraph, modifier = modifier) {
        navigation<TodayGraph>(startDestination = TodayRoute) {
            composable<TodayRoute> {
                TodayRoute(
                    onStartAdhkar = { navController.navigate(RunRoute(it)) },
                    onOpenEvent = { navController.navigate(EventRoute(it)) },
                    onOpenLocation = { navController.navigate(LocationRoute) },
                    onOpenPrayerSettings = { navController.navigate(PrayerSettingsRoute) },
                )
            }
        }
        navigation<DuasGraph>(startDestination = DuaLibraryRoute) {
            composable<DuaLibraryRoute> {
                DuaLibraryRoute(
                    onOpenEvent = { navController.navigate(EventRoute(it)) },
                    onSearch = { navController.navigate(SearchRoute) },
                )
            }
            composable<SearchRoute> {
                SearchRoute(onBack = back, onOpenDua = { navController.navigate(DuaDetailRoute(it)) })
            }
            composable<EventRoute> {
                EventRoute(
                    onBack = back,
                    onOpenDua = { navController.navigate(DuaDetailRoute(it)) },
                    onCustomize = { navController.navigate(CustomizeRoute(it)) },
                    onStart = { navController.navigate(RunRoute(it)) },
                )
            }
            composable<DuaDetailRoute> {
                DuaDetailRoute(onBack = back, onMemorize = { navController.navigate(MemorizeSessionRoute(it)) })
            }
            composable<RunRoute> { PlaceholderScreen(R.string.nav_duas) }
        }
        navigation<MemorizeGraph>(startDestination = MemorizeHomeRoute) {
            composable<MemorizeHomeRoute> { PlaceholderScreen(R.string.nav_memorize) }
        }
        navigation<SettingsGraph>(startDestination = SettingsHomeRoute) {
            composable<SettingsHomeRoute> {
                SettingsHomeRoute(
                    SettingsDestinations(
                        onPrayer = { navController.navigate(PrayerSettingsRoute) },
                        onLocation = { navController.navigate(LocationRoute) },
                    ),
                )
            }
            composable<PrayerSettingsRoute> { PrayerSettingsRoute(onBack = back) }
            composable<LocationRoute> { LocationRoute(onBack = back) }
        }
    }
}
