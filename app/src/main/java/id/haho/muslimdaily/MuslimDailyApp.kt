package id.haho.muslimdaily

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import id.haho.muslimdaily.core.diagnostics.CrashLog
import id.haho.muslimdaily.core.ui.AppLanguage
import id.haho.muslimdaily.data.location.LocationRepository
import id.haho.muslimdaily.notifications.ReminderSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import id.haho.muslimdaily.data.myset.SetsToRoutines
import javax.inject.Inject

@HiltAndroidApp
class MuslimDailyApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var reminderSync: ReminderSync
    @Inject lateinit var locations: LocationRepository
    @Inject lateinit var setsToRoutines: SetsToRoutines

    override fun onCreate() {
        super.onCreate()
        CrashLog.install(this)
        AppLanguage.onAppStart(this)
        reminderSync.start()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            runCatching { locations.backfillElevation() }
            // Occasions customised with the removed Sesuaikan become routines (once).
            runCatching { setsToRoutines.run() }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
