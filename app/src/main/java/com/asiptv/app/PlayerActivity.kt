package com.asiptv.app

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.asiptv.app.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity() {

    private lateinit var b: ActivityPlayerBinding
    private var player: ExoPlayer? = null
    private var triedAsHls = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (PlayQueue.list.isEmpty()) {
            finish()
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        b = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(b.root)
        enterFullscreen()

        b.btnBack.setOnClickListener { finish() }
        b.btnPrev.setOnClickListener { step(-1) }
        b.btnNext.setOnClickListener { step(+1) }
        b.btnRetry.setOnClickListener { play(PlayQueue.index) }

        // Show the title/prev/next overlay together with the player controls.
        b.playerView.setControllerVisibilityListener(
            PlayerView.ControllerVisibilityListener { visibility -> b.overlay.visibility = visibility }
        )
    }

    override fun onStart() {
        super.onStart()
        if (PlayQueue.list.isNotEmpty()) {
            initPlayer()
            play(PlayQueue.index)
        }
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    // ---------- playback ----------

    private fun initPlayer() {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android 13) AS-IPTV/1.0")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(15000)

        val p = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(http))
            .build()

        p.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                b.buffering.visibility = if (state == Player.STATE_BUFFERING) View.VISIBLE else View.GONE
                if (state == Player.STATE_READY) b.errorBox.visibility = View.GONE
            }

            override fun onPlayerError(error: PlaybackException) {
                // Some URLs have no ".m3u8" extension; try once more forcing HLS.
                val unsupported = error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED ||
                    error.errorCode == PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED
                if (!triedAsHls && unsupported) {
                    triedAsHls = true
                    val ch = PlayQueue.list[PlayQueue.index]
                    p.setMediaItem(
                        MediaItem.Builder().setUri(ch.url).setMimeType(MimeTypes.APPLICATION_M3U8).build()
                    )
                    p.prepare()
                    p.playWhenReady = true
                    return
                }
                b.buffering.visibility = View.GONE
                b.errorBox.visibility = View.VISIBLE
            }
        })

        b.playerView.player = p
        player = p
    }

    private fun play(index: Int) {
        val list = PlayQueue.list
        if (list.isEmpty()) return
        val i = ((index % list.size) + list.size) % list.size
        PlayQueue.index = i
        val ch = list[i]

        triedAsHls = false
        b.title.text = ch.name
        b.errorBox.visibility = View.GONE
        player?.apply {
            setMediaItem(MediaItem.fromUri(ch.url))
            prepare()
            playWhenReady = true
        }
    }

    private fun step(delta: Int) = play(PlayQueue.index + delta)

    private fun releasePlayer() {
        player?.release()
        player = null
        if (::b.isInitialized) b.playerView.player = null
    }

    // ---------- fullscreen ----------

    private fun enterFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterFullscreen()
    }
}
