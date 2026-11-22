package br.com.mediacar

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/** Frequências do equalizador (15 bandas, espaçadas a cada 2/3 de oitava). */
object EqBands {
    val FREQS = floatArrayOf(
        25f, 40f, 63f, 100f, 160f, 250f, 400f, 630f,
        1000f, 1600f, 2500f, 4000f, 6300f, 10000f, 16000f
    )
    val LABELS = arrayOf(
        "25", "40", "63", "100", "160", "250", "400", "630",
        "1k", "1,6k", "2,5k", "4k", "6,3k", "10k", "16k"
    )
    const val COUNT = 15
    const val MAX_DB = 12f
}

/** Predefinições (ganho em dB para cada uma das 15 bandas). */
object EqPresets {
    val list: List<Pair<String, FloatArray>> = listOf(
        "Plano" to FloatArray(EqBands.COUNT),
        "Graves" to floatArrayOf(6f, 6f, 5f, 4f, 3f, 1.5f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        "Voz" to floatArrayOf(-2f, -2f, -1f, 0f, 0f, 1f, 2f, 3f, 4f, 4f, 3f, 2f, 1f, 0f, -1f),
        "Rock" to floatArrayOf(5f, 4f, 3f, 2f, -1f, -2f, -1f, 1f, 2f, 3f, 4f, 4f, 4f, 3f, 3f),
        "Pop" to floatArrayOf(-1f, -1f, 0f, 1f, 2f, 3f, 3f, 2f, 1f, 0f, -1f, -1f, 0f, 1f, 1f),
        "Filme" to floatArrayOf(6f, 5f, 4f, 2f, 1f, 0f, 0f, 0f, 1f, 2f, 3f, 3f, 2f, 2f, 1f),
        "Festa" to floatArrayOf(5f, 5f, 4f, 3f, 1f, 0f, -1f, -2f, -1f, 0f, 1f, 3f, 4f, 5f, 5f),
        "Agudos" to floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 2f, 3f, 4f, 5f, 6f, 6f)
    )
}

/**
 * Equalizador gráfico por software (filtros "peaking" biquad em cascata).
 * Trabalha sobre o áudio PCM de 16 bits antes de ir para a saída de som.
 * Se o formato for diferente, o processador fica desativado e o som passa direto.
 */
class EqProcessor : BaseAudioProcessor() {

    private class Filters(val coef: Array<DoubleArray>, val active: IntArray, val preLinear: Double)

    @Volatile private var enabled = true
    @Volatile private var gains = FloatArray(EqBands.COUNT)
    @Volatile private var preampDb = 0f
    @Volatile private var sampleRate = 0
    @Volatile private var filters: Filters? = null

    // Estado dos filtros (só usado pela thread de áudio)
    private var channels = 0
    private var x1 = DoubleArray(0)
    private var x2 = DoubleArray(0)
    private var y1 = DoubleArray(0)
    private var y2 = DoubleArray(0)

    /** Chamado pela interface: aplica os novos ganhos na hora. */
    fun update(enabled: Boolean, gains: FloatArray, preampDb: Float) {
        this.enabled = enabled
        this.gains = gains.copyOf()
        this.preampDb = preampDb
        rebuild()
    }

    private fun rebuild() {
        val fs = sampleRate
        if (fs <= 0) {
            filters = null
            return
        }
        val g = gains
        val pre = preampDb
        val allFlat = g.all { abs(it) < 0.05f } && abs(pre) < 0.05f
        if (!enabled || allFlat) {
            filters = null
            return
        }
        val coef = Array(EqBands.COUNT) { b -> biquad(EqBands.FREQS[b].toDouble(), g[b].toDouble(), fs.toDouble()) }
        val active = (0 until EqBands.COUNT).filter {
            abs(g[it]) >= 0.05f && EqBands.FREQS[it] < fs * 0.45f
        }.toIntArray()
        filters = Filters(coef, active, 10.0.pow(pre / 20.0))
    }

    private fun biquad(f0: Double, gainDb: Double, fs: Double): DoubleArray {
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * f0 / fs
        val alpha = sin(w0) / (2.0 * Q)
        val cosW = cos(w0)
        val a0 = 1.0 + alpha / a
        return doubleArrayOf(
            (1.0 + alpha * a) / a0,
            (-2.0 * cosW) / a0,
            (1.0 - alpha * a) / a0,
            (-2.0 * cosW) / a0,
            (1.0 - alpha / a) / a0
        )
    }

    private fun resetState() {
        val n = EqBands.COUNT * maxOf(1, channels)
        x1 = DoubleArray(n)
        x2 = DoubleArray(n)
        y1 = DoubleArray(n)
        y2 = DoubleArray(n)
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount <= 0) {
            return AudioProcessor.AudioFormat.NOT_SET // fica desativado, o som passa direto
        }
        sampleRate = inputAudioFormat.sampleRate
        channels = inputAudioFormat.channelCount
        resetState()
        rebuild()
        return inputAudioFormat
    }

    override fun onFlush() {
        resetState()
    }

    override fun onReset() {
        sampleRate = 0
        channels = 0
        filters = null
        resetState()
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val out = replaceOutputBuffer(size)
        val f = filters
        val ch = channels
        if (f == null || ch <= 0) {
            out.put(inputBuffer)
        } else {
            val frameBytes = 2 * ch
            val coef = f.coef
            val active = f.active
            val pre = f.preLinear
            while (inputBuffer.remaining() >= frameBytes) {
                for (k in 0 until ch) {
                    var s = inputBuffer.short.toDouble() * pre
                    for (b in active) {
                        val c = coef[b]
                        val i = b * ch + k
                        val y = c[0] * s + c[1] * x1[i] + c[2] * x2[i] - c[3] * y1[i] - c[4] * y2[i]
                        x2[i] = x1[i]
                        x1[i] = s
                        y2[i] = y1[i]
                        y1[i] = y
                        s = y
                    }
                    val clipped = if (s > 32767.0) 32767.0 else if (s < -32768.0) -32768.0 else s
                    out.putShort(clipped.toInt().toShort())
                }
            }
            if (inputBuffer.hasRemaining()) out.put(inputBuffer)
        }
        out.flip()
    }

    companion object {
        /** Largura do filtro de cada banda (Q ~ 2,1 para espaçamento de 2/3 de oitava). */
        private const val Q = 2.1
    }
}
