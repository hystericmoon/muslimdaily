package id.haho.muslimdaily.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import id.haho.muslimdaily.BuildConfig
import id.haho.muslimdaily.R

/**
 * Opens the user's email app with a report pre-filled with the dua id and app/content versions.
 * The recipient comes from `muslimdaily.reportEmail` in gradle.properties; if it is empty the user
 * chooses one. Nothing is sent by the app itself.
 */
fun reportError(context: Context, duaId: String?, contentVersion: Int?) {
    val subject = if (duaId != null) {
        context.getString(R.string.report_subject_dua, duaId)
    } else {
        context.getString(R.string.report_subject_general)
    }
    val body = context.getString(
        R.string.report_body,
        duaId ?: "-",
        BuildConfig.VERSION_NAME,
        contentVersion?.toString() ?: "-",
        "${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}",
    )
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        if (BuildConfig.REPORT_EMAIL.isNotBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(BuildConfig.REPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.settings_report)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.report_no_email_app, Toast.LENGTH_LONG).show()
    }
}
