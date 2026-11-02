package com.decisionexecution.app.presentation.sound

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.IBinder
import android.os.Process
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.decisionexecution.app.domain.model.FocusSound
import com.decisionexecution.app.presentation.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.concurrent.thread

/**
 * Plays the focus sound with the screen off. Android only lets audio keep going in the background
 * from a foreground service, so this borrows the live timer notification instead of adding a
 * second one, and leaves it behind when it stops.
 */
@AndroidEntryPoint
class FocusSoundService : Service() {

    @Inject lateinit var notificationHelper: NotificationHelper

    private var player: SoundPlayer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sound = intent?.getStringExtra(EXTRA_SOUND)?.let { name -> FocusSound.entries.find { it.name == name } }
        if (sound == null || sound == FocusSound.OFF) {
            stopSelf()
            return START_NOT_STICKY
        }
        ServiceCompat.startForeground(
            this,
            notificationHelper.timerNotificationId,
            notificationHelper.timerNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        )
        player?.stop()
        player = SoundPlayer(sound).also { it.start() }
        // If the process dies the session restore decides whether sound should play again
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        player?.stop()
        player = null
        // The timer notification belongs to the session, not to the sound: leave it up for a break,
        // but take it down if the session is over (it couldn't be cancelled while we held it)
        ServiceCompat.stopForeground(
            this,
            if (notificationHelper.hasTimer) ServiceCompat.STOP_FOREGROUND_DETACH else ServiceCompat.STOP_FOREGROUND_REMOVE
        )
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_SOUND = "sound"

        /** Returns false if Android wouldn't allow starting it right now (app in the background) */
        fun play(context: Context, sound: FocusSound): Boolean = runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, FocusSoundService::class.java).putExtra(EXTRA_SOUND, sound.name)
            )
        }.isSuccess

        fun stop(context: Context) {
            context.stopService(Intent(context, FocusSoundService::class.java))
        }
    }
}

/** Streams a [SoundGenerator] to the speaker on its own thread, fading in and out so it never pops */
class SoundPlayer(private val sound: FocusSound) {

    @Volatile private var running = false

    fun start() {
        running = true
        thread(name = "focus-sound", isDaemon = true) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            runCatching { play() }
        }
    }

    fun stop() {
        running = false
    }

    private fun play() {
        val rate = SoundGenerator.SAMPLE_RATE
        val minBuffer = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(rate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBuffer, rate / 2))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        val generator = SoundGenerator(sound)
        val buffer = ShortArray(rate / CHUNKS_PER_SECOND)
        try {
            track.setVolume(0f)
            track.play()
            var volume = 0f
            while (running) {
                volume = (volume + FADE_STEP).coerceAtMost(1f)
                track.setVolume(volume)
                generator.fill(buffer)
                track.write(buffer, 0, buffer.size)
            }
            while (volume > 0f) {
                volume = (volume - FADE_STEP * 2).coerceAtLeast(0f)
                track.setVolume(volume)
                generator.fill(buffer)
                track.write(buffer, 0, buffer.size)
            }
            track.stop()
        } finally {
            track.release()
        }
    }

    private companion object {
        const val CHUNKS_PER_SECOND = 10
        /** About a second and a half to fade in, half that to fade out */
        const val FADE_STEP = 1f / 15
    }
}
