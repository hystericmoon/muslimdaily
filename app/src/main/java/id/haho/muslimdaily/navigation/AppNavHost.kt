package id.haho.muslimdaily.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.designsystem.component.PlaceholderScreen
import id.haho.muslimdaily.feature.duas.customize.CustomizeRoute
import id.haho.muslimdaily.feature.duas.detail.DuaDetailRoute
import id.haho.muslimdaily.feature.duas.event.EventRoute
import id.haho.muslimdaily.feature.duas.library.DuaLibraryRoute
import id.haho.muslimdaily.feature.duas.search.SearchRoute
import id.haho.muslimdaily.feature.settings.PrayerSettingsRoute
import id.haho.muslimdaily.feature.settings.RemindersRoute
import id.haho.muslimdaily.feature.settings.SettingsDestinations
import id.haho.muslimdaily.feature.memorize.ListenOnlyRoute
import id.haho.muslimdaily.feature.memorize.MemorizeHomeRoute
import id.haho.muslimdaily.feature.memorize.MemorizePickerRoute
import id.haho.muslimdaily.feature.memorize.session.MemorizeSessionRoute
import id.haho.muslimdaily.feature.run.RunRoute
import id.haho.muslimdaily.feature.settings.AudioSettingsRoute
import id.haho.muslimdaily.feature.settings.SettingsHomeRoute
import id.haho.muslimdaily.feature.settings.data.MySetsDataRoute
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
            composable<CustomizeRoute> { CustomizeRoute(onBack = back) }
            composable<RunRoute> { RunRoute(onClose = back) }
        }
        navigation<MemorizeGraph>(startDestination = MemorizeHomeRoute) {
            composable<MemorizeHomeRoute> {
                MemorizeHomeRoute(
                    onOpen = { id, fromReview -> navController.navigate(MemorizeSessionRoute(id, fromReview)) },
                    onPick = { navController.navigate(MemorizePickerRoute) },
                )
            }
            composable<MemorizePickerRoute> {
                MemorizePickerRoute(onBack = back, onPick = {
                    navController.navigate(MemorizeSessionRoute(it)) { popUpTo<MemorizeHomeRoute>() }
                })
            }
            composable<MemorizeSessionRoute> {
                MemorizeSessionRoute(
                    onClose = back,
                    onListenOnly = { navController.navigate(ListenOnlyRoute(it)) },
                    onNext = { navController.navigate(MemorizeSessionRoute(it, fromReview = true)) { popUpTo<MemorizeHomeRoute>() } },
                )
            }
            composable<ListenOnlyRoute> { ListenOnlyRoute(onClose = back) }
        }
        navigation<SettingsGraph>(startDestination = SettingsHomeRoute) {
            composable<SettingsHomeRoute> {
                SettingsHomeRoute(
                    SettingsDestinations(
                        onPrayer = { navController.navigate(PrayerSettingsRoute) },
                        onLocation = { navController.navigate(LocationRoute) },
                        onMySets = { navController.navigate(MySetsDataRoute) },
                        onAudio = { navController.navigate(AudioSettingsRoute) },
                        onReminders = { navController.navigate(RemindersRoute) },
                    ),
                )
            }
            composable<PrayerSettingsRoute> { PrayerSettingsRoute(onBack = back) }
            composable<LocationRoute> { LocationRoute(onBack = back) }
            composable<MySetsDataRoute> { MySetsDataRoute(onBack = back) }
            composable<AudioSettingsRoute> { AudioSettingsRoute(onBack = back) }
            composable<RemindersRoute> { RemindersRoute(onBack = back) }
        }
    }
}
