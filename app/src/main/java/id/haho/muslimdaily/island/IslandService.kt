package id.haho.muslimdaily.island

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.android.AndroidEntryPoint
import id.haho.muslimdaily.MainActivity
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.model.FIVE_PRAYERS
import id.haho.muslimdaily.core.ui.TimeFormatter
import id.haho.muslimdaily.core.ui.labelRes
import id.haho.muslimdaily.data.settings.IslandRight
import id.haho.muslimdaily.data.settings.IslandSettings
import id.haho.muslimdaily.data.settings.SettingsRepository
import id.haho.muslimdaily.domain.prayer.PrayerCalculator
import id.haho.muslimdaily.domain.prayer.PrayerSnapshots
import id.haho.muslimdaily.notifications.Destination
import id.haho.muslimdaily.notifications.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * The island: a small black pill drawn over other apps around the camera cutout, with the next
 * prayer and a countdown. Tap it to expand; tap the card to go to Today, swipe it away to close it. Off by default (beta);
 * it needs the "Display over other apps" permission, and a foreground service keeps it alive.
 * The Live Update notification stays the accessible, system-owned way to see the same thing.
 */
@AndroidEntryPoint
class IslandService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var calculator: PrayerCalculator
    @Inject lateinit var clock: Clock

    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()
    private var scope: CoroutineScope? = null
    private var collapseJob: Job? = null
    private var view: View? = null

    private var model by mutableStateOf<IslandModel?>(null)
    private var expanded by mutableStateOf(false)
    private var big by mutableStateOf(false)
    private var alertedFor: Instant? = null
    private var alertShownAt: Instant? = null
    private var firstRefresh = true
    private var baseGapDp = 0
    private var config by mutableStateOf(IslandSettings())
    private var haptics by mutableStateOf(true)
    private var statusBarDp = 0
    private var params: WindowManager.LayoutParams? = null

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedState.performAttach()
        savedState.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (view == null) {
            registry.currentState = Lifecycle.State.RESUMED
            addOverlay()
            val s = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { scope = it }
            s.launch {
                // Look and position follow the settings screen live; switching the island off stops the service.
                settings.settings.collect { app ->
                    config = app.island
                    haptics = app.haptics
                    applyLayout()
                    if (!app.reminders.nextPrayerIsland) stopSelf()
                }
            }
            // The window's width and place depend on the mode (pill, card, alert): follow it.
            s.launch { snapshotFlow { big || expanded }.collect { applyLayout() } }
            s.launch {
                while (true) {
                    if (!refresh()) {
                        stopSelf()
                        return@launch
                    }
                    delay(REFRESH_MS)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope?.cancel()
        scope = null
        removeOverlay()
        registry.currentState = Lifecycle.State.DESTROYED
        store.clear()
        super.onDestroy()
    }

    /** Recomputes the next prayer; false when there is nothing to show, so the service stops. */
    private suspend fun refresh(): Boolean {
        val s = settings.current()
        if (!s.reminders.nextPrayerIsland) return false
        val now = Instant.now(clock)
        val snapshot = PrayerSnapshots.at(now, s, calculator) ?: return false
        val m = IslandModel.from(now, snapshot)
        model = m
        if (config.bigCard && IslandAlert.shouldAlert(m.minutesLeft, m.time, alertedFor, firstRefresh, config.bigCardLeadMinutes)) showBig(m.time)
        // At the prayer time the big card has done its job.
        if (m.minutesLeft == 0) big = false
        firstRefresh = false
        return true
    }

    private fun addOverlay() {
        val wm = getSystemService(WindowManager::class.java)
        baseGapDp = IslandGeometry.gapDp(cutoutWidthPx(wm), resources.displayMetrics.density)
        statusBarDp = statusBarHeightDp()
        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@IslandService)
            setViewTreeSavedStateRegistryOwner(this@IslandService)
            setViewTreeViewModelStoreOwner(this@IslandService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val m = model
                if (m != null) {
                    IslandOverlay(
                        texts = texts(m),
                        config = config,
                        gapDp = IslandGeometry.withAdjust(baseGapDp, config.gapAdjustDp),
                        mode = when {
                            big -> IslandMode.BIG
                            expanded -> IslandMode.CARD
                            else -> IslandMode.PILL
                        },
                        onToggle = ::toggle,
                        onOpen = ::openApp,
                        onDismiss = ::dismiss,
                        haptics = haptics,
                        statusBarDp = statusBarDp,
                    )
                }
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Without FLAG_HARDWARE_ACCELERATED an overlay window draws in software, which is why
            // the first version stuttered when it expanded.
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            title = getString(R.string.app_name)
        }
        this.params = params
        runCatching { wm.addView(composeView, params) }.onSuccess { view = composeView; applyLayout() }.onFailure { stopSelf() }
    }

    /** Moves the window to the chosen offsets and hides it in landscape when asked. */
    private fun applyLayout() {
        val v = view ?: return
        val p = params ?: return
        val density = resources.displayMetrics.density
        // The pill is placed by the window; the open card and alert fill the width and place themselves.
        val open = big || expanded
        p.width = if (open) WindowManager.LayoutParams.MATCH_PARENT else WindowManager.LayoutParams.WRAP_CONTENT
        p.x = if (open) 0 else (config.offsetXDp * density).toInt()
        p.y = if (open) 0 else (config.offsetYDp * density).toInt()
        runCatching { getSystemService(WindowManager::class.java).updateViewLayout(v, p) }
        val landscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        v.visibility = if (landscape && config.hideInLandscape) View.GONE else View.VISIBLE
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyLayout()
    }

    private fun removeOverlay() {
        val v = view ?: return
        view = null
        runCatching { getSystemService(WindowManager::class.java).removeView(v) }
    }

    /** The status bar's height, so the open card can cover it. 0 when the system does not say. */
    private fun statusBarHeightDp(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) (resources.getDimensionPixelSize(id) / resources.displayMetrics.density).toInt() else 0
    }

    /** Width of the camera cutout at the top, or null on a phone without one or before Android 11. */
    private fun cutoutWidthPx(wm: WindowManager): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return wm.currentWindowMetrics.windowInsets.displayCutout
            ?.boundingRectTop
            ?.width()
            ?.takeIf { it > 0 }
    }

    private fun showBig(time: Instant) {
        alertedFor = time
        alertShownAt = Instant.now(clock)
        big = true
        expanded = false
        collapseJob?.cancel()
        // "Stay until the prayer time": no timer, refresh() clears it when the time arrives.
        collapseJob = if (config.bigUntilPrayer) null else scope?.launch {
            delay(config.bigSeconds * 1000L)
            big = false
        }
    }

    /** A swipe on the card goes back to the pill; on the big card it removes it. */
    private fun dismiss() {
        if (big) {
            dismissBig()
        } else {
            expanded = false
            collapseJob?.cancel()
        }
    }

    private fun dismissBig() {
        collapseJob?.cancel()
        big = false
    }

    private fun toggle() {
        expanded = !expanded
        collapseJob?.cancel()
        if (expanded) {
            collapseJob = scope?.launch {
                delay(config.cardSeconds * 1000L)
                expanded = false
            }
        }
    }

    private fun openApp() {
        expanded = false
        dismissBig()
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(Notifier.EXTRA_DESTINATION, Destination.TODAY.name),
        )
    }

    private fun texts(m: IslandModel): IslandTexts {
        val name = getString(m.prayer.labelRes())
        val formatter = TimeFormatter(DateFormat.is24HourFormat(this))
        val clockText = formatter.format(m.time, m.zone)
        val left = if (m.minutesLeft >= 60) {
            getString(R.string.island_hours_minutes, m.minutesLeft / 60, m.minutesLeft % 60)
        } else {
            getString(R.string.island_minutes, m.minutesLeft)
        }
        val names = FIVE_PRAYERS.map { getString(it.labelRes()) }
        val times = m.five.mapIndexed { i, t -> formatter.format(if (i == 0 && m.nextIsTomorrow) m.time else t, m.zone) }
        val shownAt = alertShownAt ?: m.now
        val description = getString(R.string.island_card_description, name, clockText, left) + " " +
            names.indices.joinToString(", ") { names[it] + " " + times.getOrElse(it) { "" } }
        return IslandTexts(
            prayerName = name,
            clock = clockText,
            left = left,
            pillRight = if (config.right == IslandRight.TIME) clockText else left,
            place = m.place,
            openLabel = getString(R.string.island_open),
            dismissLabel = getString(R.string.island_dismiss),
            description = getString(R.string.island_description, name, clockText, left),
            cardDescription = description,
            arcNames = names,
            arcTimes = times,
            arcTicks = IslandModel.arcTicks(m.five),
            arcNext = m.nextIndex,
            arcProgress = m.arcProgress,
            tomorrow = if (m.nextIsTomorrow) getString(R.string.island_tomorrow) else null,
            alertProgress = IslandModel.alertProgress(m.now, shownAt, m.time),
            alertStart = getString(R.string.island_alert_opened, formatter.format(shownAt, m.zone)),
        )
    }

    private fun startInForeground() {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification: Notification = NotificationCompat.Builder(this, Notifier.CHANNEL_ISLAND)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(this, R.color.notification_accent))
            .setContentTitle(getString(R.string.island_notification_title))
            .setContentText(getString(R.string.island_notification_text))
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 101
        private const val REFRESH_MS = 20_000L

        /**
         * Starts the island when [wanted] and the permission is granted, stops it otherwise. Safe to
         * call often; a start the system refuses (background limits) is ignored and retried on the
         * next refresh.
         */
        fun sync(context: Context, wanted: Boolean) {
            val intent = Intent(context, IslandService::class.java)
            if (wanted && Settings.canDrawOverlays(context)) {
                runCatching { ContextCompat.startForegroundService(context, intent) }
            } else {
                context.stopService(intent)
            }
        }
    }
}
