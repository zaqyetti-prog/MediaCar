package br.com.mediacar

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import java.io.File

/** Cuida do player de vídeo, da lista da pasta e de lembrar onde parou. */
class PlayerController(context: Context, private val prefs: Prefs) {

    val eq = EqProcessor()
    val player: ExoPlayer

    // Estado que a tela observa
    var playing by mutableStateOf(false)
    var loaded by mutableStateOf(false)
    var title by mutableStateOf("")
    var positionMs by mutableLongStateOf(0L)
    var durationMs by mutableLongStateOf(0L)
    var aspect by mutableFloatStateOf(16f / 9f)
    var errorMsg by mutableStateOf<String?>(null)

    /** O app está na frente? (só então gravamos "estava tocando"). */
    var foreground = false

    /** Estava tocando quando o app saiu da frente; volta a tocar quando voltar. */
    var pendingResume = false

    private var tickCount = 0

    init {
        val factory = object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .setAudioProcessors(arrayOf<AudioProcessor>(eq))
                    .build()
            }
        }
        factory.setEnableDecoderFallback(true)

        player = ExoPlayer.Builder(context, factory).build()
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true
        )
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) errorMsg = null
                refreshMeta()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                refreshMeta()
                save()
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    aspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                errorMsg = "Este vídeo não pôde ser reproduzido. O formato ou o áudio talvez não seja suportado por este aparelho."
            }
        })

        // Equalizador salvo da última vez
        eq.update(prefs.eqEnabled, prefs.bands, prefs.preamp)
    }

    private fun refreshMeta() {
        val d = player.duration
        durationMs = if (d == C.TIME_UNSET || d < 0) 0L else d
        title = player.currentMediaItem?.mediaMetadata?.title?.toString() ?: ""
    }

    /** Toca [file] e coloca os outros vídeos da mesma pasta na fila. */
    fun playFile(file: File, startMs: Long = 0L, autoPlay: Boolean = true) {
        val folderVideos = FileUtil.videosIn(file.parentFile)
        val list = if (folderVideos.any { it.absolutePath == file.absolutePath }) folderVideos else listOf(file)
        val items = list.map {
            MediaItem.Builder()
                .setUri(Uri.fromFile(it))
                .setMediaId(it.absolutePath)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(it.nameWithoutExtension).build())
                .build()
        }
        val index = list.indexOfFirst { it.absolutePath == file.absolutePath }.coerceAtLeast(0)
        errorMsg = null
        player.setMediaItems(items, index, startMs)
        player.prepare()
        player.playWhenReady = autoPlay
        loaded = true
        prefs.lastPath = file.absolutePath
        prefs.lastPos = startMs
        prefs.wasPlaying = autoPlay
        refreshMeta()
    }

    fun togglePlay() {
        if (player.playWhenReady && player.playbackState != Player.STATE_ENDED) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
            if (player.playbackState == Player.STATE_IDLE && player.currentMediaItem != null) player.prepare()
            player.play()
        }
    }

    fun seekBy(deltaMs: Long) {
        val dur = if (durationMs > 0) durationMs else Long.MAX_VALUE
        player.seekTo((player.currentPosition + deltaMs).coerceIn(0L, dur))
        positionMs = player.currentPosition
    }

    fun next() {
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        }
    }

    fun previous() {
        // Se já passou de 4 s, volta ao começo; senão vai para o vídeo anterior.
        if (player.currentPosition > 4000 || !player.hasPreviousMediaItem()) {
            player.seekTo(0)
        } else {
            player.seekToPreviousMediaItem()
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        }
    }

    /** Chamado pela tela a cada meio segundo. */
    fun tick() {
        if (!loaded) return
        positionMs = player.currentPosition
        if (durationMs <= 0L) refreshMeta()
        tickCount++
        if (tickCount % 6 == 0) save() // a cada ~3 s
    }

    /** Grava onde parou, para retomar depois (inclusive se o carro desligar de repente). */
    fun save() {
        val id = player.currentMediaItem?.mediaId ?: return
        if (id.isEmpty()) return
        prefs.lastPath = id
        prefs.lastPos = if (player.playbackState == Player.STATE_ENDED) 0L else player.currentPosition
        if (foreground) {
            prefs.wasPlaying = player.playWhenReady && player.playbackState != Player.STATE_ENDED
        }
    }

    fun release() {
        player.release()
    }
}
