package com.bankcallguard.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.bankcallguard.R

object ScamAlertSoundPlayer {
    private var mediaPlayer: MediaPlayer? = null

    fun start(context: Context) {
        stop()
        try {
            val appContext = context.applicationContext
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                setDataSource(
                    appContext.resources.openRawResourceFd(R.raw.scam_alert)
                )
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
                    stop()
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = player
        } catch (exception: Exception) {
            Log.e(TAG, "Unable to play scam alert sound", exception)
            stop()
        }
    }

    fun stop() {
        mediaPlayer?.run {
            runCatching {
                if (isPlaying) {
                    stop()
                }
            }
            release()
        }
        mediaPlayer = null
    }

    private const val TAG = "ScamAlertSound"
}
