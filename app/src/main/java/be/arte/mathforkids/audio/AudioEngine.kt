package be.arte.mathforkids.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

enum class Sfx { CLICK, SHOOT, POP, WRONG, HEART, WIN, LOSE, COIN, BOSS }

/**
 * All sound is synthesised here: no audio files in the APK.
 * One streaming AudioTrack mixes a small chiptune sequencer with one-shot effects.
 */
class AudioEngine {
    @Volatile var sfxVolume = 0.6f
    @Volatile var musicVolume = 0.3f
    /** Index of the tune to loop, or -1 for silence. */
    @Volatile var tune = 0

    private class Voice(val data: FloatArray, var pos: Int = 0)

    private val voices = ArrayList<Voice>()
    private val samples: Map<Sfx, FloatArray> = Sfx.entries.associateWith { render(it) }
    @Volatile private var thread: Thread? = null

    fun play(sfx: Sfx) {
        if (thread == null || sfxVolume <= 0f) return
        synchronized(voices) {
            if (voices.size < 8) voices += Voice(samples.getValue(sfx))
        }
    }

    fun start() {
        if (thread != null) return
        val t = Thread({ loop() }, "audio-synth")
        thread = t
        t.start()
    }

    fun stop() {
        thread = null
        synchronized(voices) { voices.clear() }
    }

    private fun loop() {
        val min = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder().setSampleRate(RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                )
                .setBufferSizeInBytes(maxOf(min, 4096))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            thread = null
            return
        }
        val buf = ShortArray(512)
        val mix = FloatArray(buf.size)
        val music = Music()
        try {
            track.play()
            while (thread === Thread.currentThread()) {
                mix.fill(0f)
                val mv = musicVolume
                val t = tune
                if (mv > 0f && t >= 0) music.render(mix, t, mv * 0.22f) else music.rest()
                val sv = sfxVolume
                synchronized(voices) {
                    val it = voices.iterator()
                    while (it.hasNext()) {
                        val v = it.next()
                        val n = minOf(mix.size, v.data.size - v.pos)
                        for (i in 0 until n) mix[i] += v.data[v.pos + i] * sv * 0.6f
                        v.pos += n
                        if (v.pos >= v.data.size) it.remove()
                    }
                }
                for (i in buf.indices) buf[i] = (mix[i].coerceIn(-1f, 1f) * 32000f).toInt().toShort()
                if (track.write(buf, 0, buf.size) < 0) break
            }
        } catch (e: Exception) {
            // Sound is a bonus: never let an audio failure take the game down.
        } finally {
            runCatching { track.stop() }
            track.release()
        }
    }

    private class Music {
        private var pos = 0L
        private var phase = 0.0
        private var bassPhase = 0.0
        private var current = -1

        fun rest() {
            current = -1
        }

        fun render(out: FloatArray, tuneIndex: Int, gain: Float) {
            if (tuneIndex != current) {
                current = tuneIndex
                pos = 0
            }
            val tune = TUNES[tuneIndex % TUNES.size]
            val shift = TRANSPOSE[(tuneIndex / TUNES.size) % TRANSPOSE.size]
            val perNote = RATE * 60 / tune.bpm / 2
            for (i in out.indices) {
                val step = (pos / perNote).toInt()
                val within = (pos % perNote).toFloat() / perNote
                val note = tune.melody[step % tune.melody.size]
                val bass = tune.bass[(step / 4) % tune.bass.size]
                var s = 0f
                if (note > 0) {
                    phase += freq(note + shift) / RATE
                    phase -= phase.toInt()
                    val env = exp(-3.0f * within) * minOf(1f, within * 40f)
                    s += (if (phase < 0.25) 1f else -0.33f) * env * 0.7f
                }
                bassPhase += freq(bass + shift) / RATE
                bassPhase -= bassPhase.toInt()
                val tri = (4.0 * kotlin.math.abs(bassPhase - 0.5) - 1.0).toFloat()
                val beat = ((pos % (perNote * 2L)).toFloat() / (perNote * 2))
                s += tri * (0.9f - 0.5f * beat)
                out[i] += s * gain
                pos++
            }
        }
    }

    private class Tune(val bpm: Int, val melody: IntArray, val bass: IntArray)

    companion object {
        const val RATE = 22050
        /** Tunes 0..2 are the base melodies; higher indices replay them in another key. */
        const val MENU_TUNE = 0

        fun tuneForWorld(world: Int): Int = 1 + world

        private val TRANSPOSE = intArrayOf(0, 2, -3, 5)

        private val TUNES = arrayOf(
            Tune(
                108,
                intArrayOf(72, 0, 76, 0, 79, 76, 72, 0, 74, 0, 77, 0, 81, 77, 74, 0, 76, 0, 79, 0, 84, 79, 76, 0, 79, 77, 76, 74, 72, 0, 0, 0),
                intArrayOf(48, 55, 50, 57, 52, 55, 43, 48),
            ),
            Tune(
                120,
                intArrayOf(67, 72, 76, 72, 67, 72, 76, 0, 69, 72, 77, 72, 69, 72, 77, 0, 71, 74, 79, 74, 71, 74, 79, 0, 72, 76, 79, 84, 79, 76, 72, 0),
                intArrayOf(48, 48, 53, 53, 55, 55, 48, 48),
            ),
            Tune(
                112,
                intArrayOf(69, 0, 72, 69, 76, 0, 72, 0, 67, 0, 71, 67, 74, 0, 71, 0, 65, 0, 69, 65, 72, 0, 69, 0, 64, 68, 71, 76, 71, 68, 64, 0),
                intArrayOf(45, 45, 43, 43, 41, 41, 40, 40),
            ),
        )

        private fun freq(midi: Int): Double = 440.0 * 2.0.pow((midi - 69) / 12.0)

        private fun tone(out: FloatArray, from: Int, len: Int, f0: Double, f1: Double, square: Boolean, amp: Float) {
            var ph = 0.0
            for (i in 0 until len) {
                if (from + i >= out.size) return
                val t = i.toFloat() / len
                ph += (f0 + (f1 - f0) * t) / RATE
                val w = if (square) (if (ph - ph.toInt() < 0.5) 1f else -1f) * 0.6f else sin(2 * PI * ph).toFloat()
                out[from + i] += w * amp * (1f - t) * minOf(1f, i / 40f)
            }
        }

        private fun notes(midi: IntArray, each: Float, square: Boolean): FloatArray {
            val n = (RATE * each).toInt()
            val out = FloatArray(n * midi.size + n)
            midi.forEachIndexed { i, m -> tone(out, i * n, n * 2, freq(m), freq(m), square, 0.7f) }
            return out
        }

        private fun render(sfx: Sfx): FloatArray = when (sfx) {
            Sfx.CLICK -> FloatArray(RATE / 25).also { tone(it, 0, it.size, 900.0, 700.0, false, 0.5f) }
            Sfx.SHOOT -> FloatArray(RATE / 7).also { tone(it, 0, it.size, 500.0, 1500.0, false, 0.8f) }
            Sfx.POP -> FloatArray(RATE / 4).also { out ->
                tone(out, 0, out.size, 420.0, 90.0, false, 0.9f)
                val r = Random(7)
                for (i in 0 until out.size / 3) out[i] += (r.nextFloat() - 0.5f) * 0.8f * (1f - i / (out.size / 3f))
            }
            Sfx.WRONG -> FloatArray(RATE / 4).also {
                tone(it, 0, it.size / 2, 220.0, 200.0, true, 0.5f)
                tone(it, it.size / 2, it.size / 2, 170.0, 150.0, true, 0.5f)
            }
            Sfx.HEART -> notes(intArrayOf(67, 63, 58), 0.12f, true)
            Sfx.WIN -> notes(intArrayOf(72, 76, 79, 84, 79, 84), 0.11f, false)
            Sfx.LOSE -> notes(intArrayOf(67, 65, 62, 58), 0.2f, false)
            Sfx.COIN -> notes(intArrayOf(88, 93), 0.07f, false)
            Sfx.BOSS -> FloatArray(RATE * 3 / 4).also {
                tone(it, 0, it.size, 110.0, 55.0, true, 0.7f)
                tone(it, 0, it.size, 113.0, 58.0, true, 0.5f)
            }
        }
    }
}
