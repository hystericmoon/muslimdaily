package id.haho.muslimdaily

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import id.haho.muslimdaily.notifications.ReminderSync
import javax.inject.Inject

@HiltAndroidApp
class MuslimDailyApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var reminderSync: ReminderSync

    override fun onCreate() {
        super.onCreate()
        reminderSync.start()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
