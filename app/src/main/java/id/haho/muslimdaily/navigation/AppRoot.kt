package id.haho.muslimdaily.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import id.haho.muslimdaily.core.designsystem.glass.Glass
import id.haho.muslimdaily.core.designsystem.theme.AppTheme
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import id.haho.muslimdaily.R
import id.haho.muslimdaily.notifications.Destination

/** Routes that take the whole screen (focus modes) and hide the navigation bar. */
private val immersiveRoutes = listOf(RunRoute::class, MemorizeSessionRoute::class, ListenOnlyRoute::class)

@Composable
fun AppRoot(
    reviewDueCount: Int,
    modifier: Modifier = Modifier,
    pendingDestination: Destination? = null,
    onDestinationHandled: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
) {
    LaunchedEffect(pendingDestination) {
        val target: Any = when (pendingDestination ?: return@LaunchedEffect) {
            Destination.TODAY -> TodayGraph
            Destination.MORNING_RUN -> RunRoute("morning")
            Destination.EVENING_RUN -> RunRoute("evening")
            Destination.SLEEP_EVENT -> EventRoute("sleeping")
            Destination.MEMORIZE -> MemorizeGraph
            Destination.MOSQUES -> MosquesRoute
            Destination.FASTING -> FastingRoute
        }
        navController.navigate(target) { launchSingleTop = true }
        onDestinationHandled()
    }
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val immersive = immersiveRoutes.any { destination?.hasRoute(it) == true }

    val motion = AppTheme.motion
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        containerColor = Color.Transparent,
        bottomBar = {
            AnimatedVisibility(
                visible = !immersive,
                enter = slideInVertically(motion.layout()) { it } + fadeIn(motion.fade()),
                exit = slideOutVertically(motion.layout()) { it } + fadeOut(motion.fade()),
            ) {
                AppNavigationBar(destination, reviewDueCount) { target ->
                    navController.navigate(target.graph) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        },
    ) { padding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        )
    }
}

@Composable
private fun AppNavigationBar(
    destination: NavDestination?,
    reviewDueCount: Int,
    onSelect: (TopLevelDestination) -> Unit,
) {
    val g = Glass.colors
    NavigationBar(
        containerColor = g.chrome,
        tonalElevation = 0.dp,
        modifier = Modifier.drawBehind {
            if (g.enabled) {
                drawRect(Brush.verticalGradient(0f to g.sheen, 0.6f to Color.Transparent))
                drawLine(g.rimTop, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
            }
        },
    ) {
        TopLevelDestination.entries.forEach { item ->
            val selected = destination?.hierarchy?.any { it.hasRoute(item.graph::class) } == true
            val label = stringResource(item.label)
            val badge = item == TopLevelDestination.MEMORIZE && reviewDueCount > 0
            val badgeDescription = if (badge) {
                pluralStringResource(R.plurals.review_due_badge, reviewDueCount, reviewDueCount)
            } else {
                null
            }
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(item) },
                icon = {
                    BadgedBox(badge = { if (badge) Badge { Text(reviewDueCount.toString()) } }) {
                        Crossfade(selected, animationSpec = AppTheme.motion.fade(), label = "navIcon") { on ->
                            Icon(
                                painter = painterResource(if (on) item.selectedIcon else item.icon),
                                contentDescription = null,
                            )
                        }
                    }
                },
                label = { NavLabel(label) },
                colors = NavigationBarItemDefaults.colors(indicatorColor = g.tinted),
                modifier = if (badgeDescription != null) {
                    Modifier.semantics { contentDescription = "$label, $badgeDescription" }
                } else {
                    Modifier
                },
            )
        }
    }
}

/**
 * Navigation labels share one size, capped at 1.5x the base size so four labels fit side by side at
 * 200% system font. A label that still does not fit steps down rather than breaking mid-word; the
 * full-size name stays available to TalkBack through the item semantics.
 */
@Composable
private fun NavLabel(text: String) {
    val color = LocalContentColor.current
    val style = MaterialTheme.typography.labelMedium
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, fontScale = density.fontScale.coerceAtMost(NAV_LABEL_MAX_SCALE)),
    ) {
        BasicText(
            text = text,
            style = style,
            color = ColorProducer { color },
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = style.fontSize, stepSize = 0.5.sp),
        )
    }
}

private const val NAV_LABEL_MAX_SCALE = 1.5f
