package id.haho.muslimdaily.audio

import android.content.ComponentName
import android.content.Context
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import dagger.hilt.android.qualifiers.ApplicationContext
import id.haho.muslimdaily.R
import id.haho.muslimdaily.core.model.Dua
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One step of a playback sequence. A [Clip] plays part of a dua's recitation; a [Pause] is silence
 * (for repeat-aloud gaps in memorize mode), played from a silent asset so the screen can be off
 * and the lock-screen controls keep working throughout.
 */
sealed interface SequenceItem {
    data class Clip(
        val duaId: String,
        val file: String,
        val startMs: Long,
        /** C.TIME_END_OF_SOURCE for the whole file. */
        val endMs: Long,
        /** Phrases this clip covers, for display ("phrases 1–3"). */
        val phrases: IntRange,
        val title: String,
    ) : SequenceItem

    data class Pause(val durationMs: Long, val afterPhrases: IntRange, val title: String) : SequenceItem
}

data class PlaybackState(
    val duaId: String? = null,
    val isPlaying: Boolean = false,
    /** Index into the dua's phrases currently being recited, or -1. */
    val activePhrase: Int = -1,
    /** Index into the current sequence, or -1 when nothing is queued. */
    val sequenceIndex: Int = -1,
    val sequenceSize: Int = 0,
    val inPause: Boolean = false,
    val ended: Boolean = false,
    val speed: Float = 1f,
    val repeat: Boolean = false,
    val positionMs: Long = 0,
    /** The last clip could not be played (a missing or unreadable file). Cleared on the next play. */
    val failed: Boolean = false,
)

/**
 * Owns the single ExoPlayer shared by the dua detail player, memorize mode and the media session.
 * Clip playback uses MediaItem clipping on segment timestamps; the active phrase is derived from
 * the playback position and the dua's segments.
 */
@Singleton
class AudioEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var player: ExoPlayer? = null
    private var controller: MediaController? = null
    private var ticker: Job? = null
    private var currentDua: Dua? = null
    private var sequence: List<SequenceItem> = emptyList()

    @MainThread
    fun player(): ExoPlayer = player ?: ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true)
        .setWakeMode(C.WAKE_MODE_LOCAL)
        .build()
        .also { p ->
            player = p
            p.addListener(listener)
        }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish()
            if (player.isPlaying) startTicker() else stopTicker()
        }

        override fun onPlayerError(error: PlaybackException) {
            stopTicker()
            _state.value = _state.value.copy(isPlaying = false, activePhrase = -1, failed = true)
        }
    }

    /** Binds the media session service so playback continues in the background and on the lock screen. */
    private fun ensureSession() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({ controller = runCatching { future.get() }.getOrNull() }, ContextCompat.getMainExecutor(context))
    }

    /** Plays the whole recitation of [dua], optionally starting at a phrase. */
    fun playDua(dua: Dua, speed: Float, fromPhrase: Int = 0, title: String = "") {
        val audio = dua.audio ?: return
        val startMs = dua.phrases.getOrNull(fromPhrase)?.segment?.startMs ?: audio.startMs
        playSequence(
            dua,
            listOf(SequenceItem.Clip(dua.id, audio.file, startMs, audio.endMs ?: C.TIME_END_OF_SOURCE, fromPhrase until dua.phrases.size, title)),
            speed,
        )
    }

    fun playSequence(dua: Dua, items: List<SequenceItem>, speed: Float) {
        val p = player()
        ensureSession()
        currentDua = dua
        sequence = items
        val metadataTitle = context.getString(R.string.app_name)
        p.setMediaItems(
            items.mapIndexed { i, item ->
                when (item) {
                    is SequenceItem.Clip -> mediaItem("$i", "asset:///audio/${item.file}", item.startMs, item.endMs, item.title, metadataTitle)
                    is SequenceItem.Pause -> mediaItem("$i", "asset:///audio/$SILENCE", 0, item.durationMs, item.title, metadataTitle)
                }
            },
        )
        p.repeatMode = if (_state.value.repeat) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        p.playbackParameters = PlaybackParameters(speed)
        p.prepare()
        p.play()
        _state.value = _state.value.copy(duaId = dua.id, speed = speed, ended = false, failed = false, sequenceSize = items.size)
    }

    private fun mediaItem(id: String, uri: String, startMs: Long, endMs: Long, title: String, artist: String) =
        MediaItem.Builder()
            .setMediaId(id)
            .setUri(uri)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build(),
            )
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title.ifBlank { artist }).setArtist(artist).build())
            .build()

    fun toggle() {
        val p = player ?: return
        if (p.isPlaying) {
            p.pause()
        } else {
            if (p.playbackState == Player.STATE_ENDED) p.seekTo(0, 0)
            p.play()
        }
    }

    fun pause() = player?.pause()

    fun stop() {
        player?.stop()
        player?.clearMediaItems()
        sequence = emptyList()
        currentDua = null
        _state.value = PlaybackState(speed = _state.value.speed, repeat = _state.value.repeat)
    }

    fun setSpeed(speed: Float) {
        player?.playbackParameters = PlaybackParameters(speed)
        _state.value = _state.value.copy(speed = speed)
    }

    fun setRepeat(repeat: Boolean) {
        player?.repeatMode = if (repeat) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        _state.value = _state.value.copy(repeat = repeat)
    }

    fun skipTo(index: Int) {
        val p = player ?: return
        if (index in 0 until p.mediaItemCount) {
            p.seekTo(index, 0)
            p.play()
        }
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                publish()
                delay(TICK_MS)
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun publish() {
        val p = player ?: return
        val index = p.currentMediaItemIndex
        val item = sequence.getOrNull(index)
        val dua = currentDua
        val active = when (item) {
            is SequenceItem.Clip -> {
                val absolute = item.startMs + p.currentPosition
                dua?.phrases?.indexOfFirst { ph -> ph.segment?.let { absolute >= it.startMs && absolute < it.endMs } == true } ?: -1
            }
            else -> -1
        }
        _state.value = _state.value.copy(
            isPlaying = p.isPlaying,
            activePhrase = active,
            sequenceIndex = if (sequence.isEmpty()) -1 else index,
            inPause = item is SequenceItem.Pause,
            ended = p.playbackState == Player.STATE_ENDED,
            positionMs = p.currentPosition,
        )
    }

    fun release() {
        stopTicker()
        controller?.release()
        controller = null
        player?.release()
        player = null
    }

    companion object {
        const val SILENCE = "silence.opus"
        private const val TICK_MS = 80L
    }
}
