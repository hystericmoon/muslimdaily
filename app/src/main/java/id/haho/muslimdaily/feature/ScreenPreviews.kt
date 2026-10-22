package id.haho.muslimdaily.feature

import androidx.compose.runtime.Composable
import id.haho.muslimdaily.audio.PlaybackState
import id.haho.muslimdaily.core.model.DuaEvent
import id.haho.muslimdaily.core.ui.PreviewSurface
import id.haho.muslimdaily.core.ui.ThemePreviews
import id.haho.muslimdaily.data.review.ReviewItem
import id.haho.muslimdaily.data.run.RunItem
import id.haho.muslimdaily.data.settings.MemorizeSettings
import id.haho.muslimdaily.domain.memorize.FadeLevel
import id.haho.muslimdaily.domain.memorize.LeitnerState
import id.haho.muslimdaily.domain.memorize.Recall
import id.haho.muslimdaily.domain.myset.ActiveSet
import id.haho.muslimdaily.domain.myset.Customization
import id.haho.muslimdaily.feature.duas.DuaPreviewData
import id.haho.muslimdaily.feature.duas.components.ReadingPrefs
import id.haho.muslimdaily.feature.duas.customize.CustomizeActions
import id.haho.muslimdaily.feature.duas.customize.CustomizeContent
import id.haho.muslimdaily.feature.duas.customize.CustomizeUiState
import id.haho.muslimdaily.feature.duas.detail.DuaDetailActions
import id.haho.muslimdaily.feature.duas.detail.DuaDetailContent
import id.haho.muslimdaily.feature.duas.detail.DuaDetailState
import id.haho.muslimdaily.feature.duas.event.EventContent
import id.haho.muslimdaily.feature.duas.event.EventUiState
import id.haho.muslimdaily.feature.duas.library.DuaLibraryContent
import id.haho.muslimdaily.feature.duas.search.SearchContent
import id.haho.muslimdaily.feature.memorize.HifzEntry
import id.haho.muslimdaily.feature.memorize.ListenOnlyContent
import id.haho.muslimdaily.feature.memorize.ListenOnlyState
import id.haho.muslimdaily.feature.memorize.MemorizeHomeContent
import id.haho.muslimdaily.feature.memorize.MemorizeHomeState
import id.haho.muslimdaily.feature.memorize.MemorizePickerContent
import id.haho.muslimdaily.feature.memorize.PickerGroup
import id.haho.muslimdaily.feature.memorize.session.MemorizeActions
import id.haho.muslimdaily.feature.memorize.session.MemorizeSessionScreen
import id.haho.muslimdaily.feature.memorize.session.MemorizeUiState
import id.haho.muslimdaily.feature.memorize.session.Playing
import id.haho.muslimdaily.feature.memorize.session.Step
import id.haho.muslimdaily.feature.run.RunActions
import id.haho.muslimdaily.feature.run.RunScreen
import id.haho.muslimdaily.feature.run.RunUiState
import id.haho.muslimdaily.feature.settings.AboutContent
import id.haho.muslimdaily.feature.settings.AudioSettingsContent
import id.haho.muslimdaily.feature.settings.DisplaySettingsContent
import id.haho.muslimdaily.feature.settings.MethodologyContent
import id.haho.muslimdaily.feature.settings.PermissionState
import id.haho.muslimdaily.feature.settings.PrayerSettingsContent
import id.haho.muslimdaily.feature.settings.RemindersContent
import id.haho.muslimdaily.feature.settings.SettingsDestinations
import id.haho.muslimdaily.feature.settings.SettingsHomeContent
import id.haho.muslimdaily.feature.settings.data.MySetsDataContent
import id.haho.muslimdaily.feature.settings.location.LocationContent
import id.haho.muslimdaily.feature.today.TodayPreviewData
import java.time.LocalDate

/*
 * Every screen in light, dark and 200% font scale (@ThemePreviews). The Today previews live next
 * to TodayScreen. Sample data is structural only; see DuaPreviewData.
 */

private val event = DuaEvent("morning", "adhkar", "Dzikir pagi", 1)
private val settings = TodayPreviewData.settings

@ThemePreviews @Composable
private fun DuaLibraryPreview() = PreviewSurface { DuaLibraryContent(DuaPreviewData.categories) {} }

@ThemePreviews @Composable
private fun EventPreview() = PreviewSurface {
    val items = ActiveSet.items(ActiveSet.canonical(DuaPreviewData.duas), Customization())
    EventContent(EventUiState(event, false, false, items, 0, ReadingPrefs()), {}, {}, { _, _ -> }, {})
}

@ThemePreviews @Composable
private fun DuaDetailPreview() = PreviewSurface {
    DuaDetailContent(
        DuaDetailState(DuaPreviewData.dua("d", 1, count = 3), event, ReadingPrefs(), PlaybackState(duaId = "d", activePhrase = 1), true),
        DuaDetailActions(),
    )
}

@ThemePreviews @Composable
private fun SearchPreview() = PreviewSurface { SearchContent("tes", DuaPreviewData.duas.take(2), ReadingPrefs(), {}, {}, autoFocus = false) }

@ThemePreviews @Composable
private fun CustomizePreview() = PreviewSurface {
    val all = ActiveSet.items(ActiveSet.canonical(DuaPreviewData.duas), Customization())
    CustomizeContent(CustomizeUiState(event, all, all, emptyList(), emptyMap(), false), CustomizeActions())
}

@ThemePreviews @Composable
private fun RunPreview() = PreviewSurface {
    val duas = DuaPreviewData.duas.take(2)
    RunScreen(
        RunUiState("Dzikir pagi", duas, duas.map { RunItem(it.id, it.count) }, listOf(0, 1), 1, false, ReadingPrefs(), true, true),
        RunActions(),
    )
}

@ThemePreviews @Composable
private fun MemorizeHomePreview() = PreviewSurface {
    val today = LocalDate.of(2026, 10, 21)
    MemorizeHomeContent(
        MemorizeHomeState(today, listOf(HifzEntry(ReviewItem("a", LeitnerState(2, today), null), DuaPreviewData.dua("a", 1), "Bangun tidur", true))),
    ) { _, _ -> }
}

@ThemePreviews @Composable
private fun MemorizeSessionPreview() = PreviewSurface {
    val dua = DuaPreviewData.dua("m", 1)
    val words = Recall.words(dua)
    MemorizeSessionScreen(
        MemorizeUiState(
            dua, "Dzikir pagi", Step.RECALL, Playing.NONE, PlaybackState(), MemorizeSettings(), FadeLevel.LIGHT, words,
            Recall.hiddenWords(words, FadeLevel.LIGHT, dua.id), null, null, false, null, 0..1,
        ),
        MemorizeActions(),
        talkBack = false,
    )
}

@ThemePreviews @Composable
private fun ListenOnlyPreview() = PreviewSurface { ListenOnlyContent(ListenOnlyState(DuaPreviewData.dua("l", 1), PlaybackState()), {}, {}, {}) }

@ThemePreviews @Composable
private fun MemorizePickerPreview() = PreviewSurface {
    MemorizePickerContent("", listOf(PickerGroup("Dzikir pagi", DuaPreviewData.duas.take(2))), ReadingPrefs(), {}, {})
}

@ThemePreviews @Composable
private fun SettingsHomePreview() = PreviewSurface { SettingsHomeContent(settings, SettingsDestinations()) }

@ThemePreviews @Composable
private fun PrayerSettingsPreview() = PreviewSurface { PrayerSettingsContent(settings) {} }

@ThemePreviews @Composable
private fun LocationPreview() = PreviewSurface { LocationContent(settings.location, "", emptyList(), false, {}, {}, {}) }

@ThemePreviews @Composable
private fun RemindersPreview() = PreviewSurface { RemindersContent(settings, PermissionState(true, false), {}, {}, {}) }

@ThemePreviews @Composable
private fun DisplayPreview() = PreviewSurface { DisplaySettingsContent(settings) {} }

@ThemePreviews @Composable
private fun AudioPreview() = PreviewSurface { AudioSettingsContent(settings) {} }

@ThemePreviews @Composable
private fun MySetsDataPreview() = PreviewSurface { MySetsDataContent({}, {}, {}) }

@ThemePreviews @Composable
private fun MethodologyPreview() = PreviewSurface { MethodologyContent(3) }

@ThemePreviews @Composable
private fun AboutPreview() = PreviewSurface { AboutContent() }
