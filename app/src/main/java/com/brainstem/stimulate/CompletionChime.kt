package com.brainstem.stimulate

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.VibrationEffect
import android.os.Vibrator
import kotlin.math.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** The reference's four sine notes, second partial, gain envelope, and completion vibration. */
suspend fun completionChime(context: Context) =
    withContext(Dispatchers.Default) {
        val rate = 44100
        val duration = 2.4
        val notes = listOf(523.25 to 0.0, 659.25 to .16, 783.99 to .32, 1046.5 to .5)
        val pcm =
            ShortArray((duration * rate).toInt()) { sample ->
                val t = sample.toDouble() / rate - .05
                var value = 0.0
                notes.forEachIndexed { i, (frequency, start) ->
                    val local = t - start
                    val length = if (i == 3) 1.8 else 1.1
                    if (local >= 0 && local <= length) {
                        listOf(1.0 to .32, 2.01 to .08).forEach { (partial, peak) ->
                            val gain =
                                if (local < .015) peak * local / .015
                                else peak * (.0004 / peak).pow((local - .015) / (length - .015))
                            value += sin(2 * PI * frequency * partial * local) * gain * .22
                        }
                    }
                }
                (value.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).roundToInt().toShort()
            }
        val track =
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()
        try {
            track.write(pcm, 0, pcm.size)
            track.play()
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 60, 60, 120), -1)
            )
            delay((duration * 1000).toLong() + 50)
        } finally {
            track.release()
        }
    }
