package com.asiptv.app

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Loads channels from: the bundled playlist (assets/channels.m3u) plus any playlist URLs
 * the user added. Downloaded playlists are cached so the app works offline.
 */
class PlaylistRepository(private val ctx: Context) {

    private val prefs = ctx.getSharedPreferences("asiptv", Context.MODE_PRIVATE)

    // ---------- playlist URLs ----------

    fun customUrls(): List<String> =
        (prefs.getString("urls", "") ?: "").split("\n").map { it.trim() }.filter { it.isNotEmpty() }

    fun addUrl(url: String) {
        val list = customUrls().toMutableList()
        if (!list.contains(url)) list.add(url)
        prefs.edit().putString("urls", list.joinToString("\n")).apply()
    }

    fun clearCustom() {
        prefs.edit().remove("urls").apply()
        cacheDir().listFiles()?.forEach { it.delete() }
    }

    // ---------- loading ----------

    fun loadBundled(): List<Channel> =
        ctx.assets.open("channels.m3u").bufferedReader().use { M3uParser.parse(it.readText()) }

    /** Channels from previously downloaded custom playlists (no network). */
    fun loadCached(): List<Channel> =
        customUrls().flatMap { url ->
            val f = cacheFile(url)
            if (f.exists()) M3uParser.parse(f.readText()) else emptyList()
        }

    /** Downloads one playlist, caches it, and returns its channels. Throws on failure. */
    suspend fun fetch(url: String): List<Channel> = withContext(Dispatchers.IO) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 30000
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "AS-IPTV/1.0")
        try {
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val channels = M3uParser.parse(text)
            if (channels.isEmpty()) throw IllegalStateException("No channels found in that playlist")
            cacheFile(url).writeText(text)
            channels
        } finally {
            conn.disconnect()
        }
    }

    /** Re-downloads every custom playlist. Returns how many failed. */
    suspend fun refreshAll(): Int {
        var failed = 0
        for (u in customUrls()) {
            try { fetch(u) } catch (e: Exception) { failed++ }
        }
        return failed
    }

    // ---------- favorites ----------

    fun favorites(): Set<String> = HashSet(prefs.getStringSet("fav", emptySet()) ?: emptySet())

    fun toggleFavorite(url: String): Boolean {
        val set = favorites().toMutableSet()
        val nowFav = if (set.contains(url)) { set.remove(url); false } else { set.add(url); true }
        prefs.edit().putStringSet("fav", set).apply()
        return nowFav
    }

    // ---------- cache helpers ----------

    private fun cacheDir() = File(ctx.filesDir, "playlists").apply { mkdirs() }
    private fun cacheFile(url: String) = File(cacheDir(), "pl_${url.hashCode()}.m3u")
}
