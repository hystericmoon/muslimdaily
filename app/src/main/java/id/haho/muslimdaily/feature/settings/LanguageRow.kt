package id.haho.muslimdaily.feature.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import id.haho.muslimdaily.R
import id.haho.design.component.SegmentedRow
import id.haho.muslimdaily.core.ui.AppLanguage

/** Indonesian / English, each named in its own language so either reader can find theirs. */
@Composable
fun LanguageRow(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val current = remember(context) { AppLanguage.current(context) }
    SegmentedRow(
        title = stringResource(R.string.display_language_app),
        options = listOf(AppLanguage.INDONESIAN, AppLanguage.ENGLISH),
        selected = current,
        onSelect = { tag -> if (tag != current) context.findActivity()?.let { AppLanguage.set(it, tag) } },
        label = { stringResource(if (it == AppLanguage.ENGLISH) R.string.language_english else R.string.language_indonesian) },
        modifier = modifier,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
