package id.haho.muslimdaily.navigation

import id.haho.muslimdaily.feature.settings.UpdatesRoute
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import id.haho.design.theme.HahoTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import id.haho.muslimdaily.feature.duas.customize.CustomizeRoute
import id.haho.muslimdaily.feature.duas.detail.DuaDetailRoute
import id.haho.muslimdaily.feature.duas.event.EventRoute
import id.haho.muslimdaily.feature.duas.library.DuaLibraryRoute
import id.haho.muslimdaily.feature.routines.RoutineEditRoute
import id.haho.muslimdaily.feature.duas.search.SearchRoute
import id.haho.muslimdaily.feature.settings.AboutRoute
import id.haho.muslimdaily.feature.settings.diagnostics.DiagnosticsRoute
import id.haho.muslimdaily.feature.settings.DisplaySettingsRoute
import id.haho.muslimdaily.feature.settings.MethodologyRoute
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
import id.haho.muslimdaily.feature.mosque.NearbyMosqueRoute
import id.haho.muslimdaily.feature.qibla.QiblaRoute
import id.haho.muslimdaily.feature.fasting.FastingRoute

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    val back: () -> Unit = { navController.popBackStack() }
    val reduced = HahoTheme.motion.reduced
    NavHost(
        navController = navController,
        startDestination = TodayGraph,
        modifier = modifier,
        // Material shared-axis (x): the new screen fades in while drifting a short distance.
        enterTransition = { if (reduced) EnterTransition.None else sharedAxisIn(forward = true) },
        exitTransition = { if (reduced) ExitTransition.None else sharedAxisOut(forward = true) },
        popEnterTransition = { if (reduced) EnterTransition.None else sharedAxisIn(forward = false) },
        popExitTransition = { if (reduced) ExitTransition.None else sharedAxisOut(forward = false) },
    ) {
        navigation<TodayGraph>(startDestination = TodayRoute) {
            composable<TodayRoute> {
                TodayRoute(
                    onStartAdhkar = { navController.navigate(RunRoute(it)) },
                    onOpenEvent = { navController.navigate(EventRoute(it)) },
                    onOpenLocation = { navController.navigate(TodayLocationRoute) },
                    onOpenPrayerSettings = { navController.navigate(TodayPrayerSettingsRoute) },
                    onOpenMosques = { navController.navigate(MosquesRoute) },
                    onOpenQibla = { navController.navigate(QiblaRoute) },
                    onOpenFasting = { navController.navigate(FastingRoute) },
                    onStartRun = { navController.navigate(RunRoute(it)) },
                )
            }
            composable<FastingRoute> { FastingRoute(onBack = back) }
            composable<QiblaRoute> { QiblaRoute(onBack = back, onOpenLocation = { navController.navigate(TodayLocationRoute) }) }
            composable<TodayLocationRoute> { LocationRoute(onBack = back) }
            composable<TodayPrayerSettingsRoute> { PrayerSettingsRoute(onBack = back) }
            composable<MosquesRoute> { NearbyMosqueRoute(onBack = back) }
        }
        navigation<DuasGraph>(startDestination = DuaLibraryRoute) {
            composable<DuaLibraryRoute> {
                DuaLibraryRoute(
                    onOpenEvent = { navController.navigate(EventRoute(it)) },
                    onSearch = { navController.navigate(SearchRoute) },
                    onOpenDua = { navController.navigate(DuaDetailRoute(it)) },
                    onCustomize = { navController.navigate(CustomizeRoute(it)) },
                    onStart = { navController.navigate(RunRoute(it)) },
                    onRoutine = { id, fromEvent -> navController.navigate(RoutineEditRoute(id, fromEvent)) },
                    onNewRoutineWith = { navController.navigate(RoutineEditRoute(withDua = it)) },
                )
            }
            composable<RoutineEditRoute> { RoutineEditRoute(onBack = back) }
            composable<SearchRoute> {
                SearchRoute(onBack = back, onOpenDua = { navController.navigate(DuaDetailRoute(it)) })
            }
            composable<EventRoute> {
                EventRoute(
                    onBack = back,
                    onOpenDua = { navController.navigate(DuaDetailRoute(it)) },
                    onCustomize = { navController.navigate(CustomizeRoute(it)) },
                    onStart = { navController.navigate(RunRoute(it)) },
                    onNewRoutine = { navController.navigate(RoutineEditRoute(withDua = it)) },
                )
            }
            composable<DuaDetailRoute> {
                DuaDetailRoute(
                    onBack = back,
                    onMemorize = { navController.navigate(MemorizeSessionRoute(it)) },
                    onNewRoutine = { navController.navigate(RoutineEditRoute(withDua = it)) },
                )
            }
            composable<CustomizeRoute> {
                CustomizeRoute(onBack = back, onSaveAsRoutine = { navController.navigate(RoutineEditRoute(fromEvent = it)) })
            }
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
                        onDisplay = { navController.navigate(DisplaySettingsRoute) },
                        onMethodology = { navController.navigate(MethodologyRoute) },
                        onAbout = { navController.navigate(AboutRoute) },
                        onDiagnostics = { navController.navigate(DiagnosticsRoute) },
                        onUpdates = { navController.navigate(UpdatesRoute) },
                    ),
                )
            }
            composable<PrayerSettingsRoute> { PrayerSettingsRoute(onBack = back) }
            composable<LocationRoute> { LocationRoute(onBack = back) }
            composable<MySetsDataRoute> { MySetsDataRoute(onBack = back) }
            composable<AudioSettingsRoute> { AudioSettingsRoute(onBack = back) }
            composable<RemindersRoute> { RemindersRoute(onBack = back) }
            composable<DisplaySettingsRoute> { DisplaySettingsRoute(onBack = back) }
            composable<MethodologyRoute> { MethodologyRoute(onBack = back) }
            composable<AboutRoute> { AboutRoute(onBack = back) }
            composable<DiagnosticsRoute> { DiagnosticsRoute(onBack = back) }
            composable<UpdatesRoute> { UpdatesRoute(onBack = back) }
        }
    }
}

private const val AXIS_IN_MS = 260
private const val AXIS_OUT_MS = 120

private fun sharedAxisIn(forward: Boolean): EnterTransition =
    fadeIn(tween(AXIS_IN_MS, delayMillis = AXIS_OUT_MS / 2, easing = LinearOutSlowInEasing)) +
        slideInHorizontally(tween(AXIS_IN_MS + AXIS_OUT_MS / 2, easing = FastOutSlowInEasing)) { w ->
            (if (forward) w else -w) / 12
        }

private fun sharedAxisOut(forward: Boolean): ExitTransition =
    fadeOut(tween(AXIS_OUT_MS, easing = FastOutLinearInEasing)) +
        slideOutHorizontally(tween(AXIS_IN_MS + AXIS_OUT_MS / 2, easing = FastOutSlowInEasing)) { w ->
            (if (forward) -w else w) / 12
        }
