package com.asiptv.app

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.asiptv.app.databinding.ActivityMainBinding
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var repo: PlaylistRepository
    private lateinit var adapter: ChannelAdapter

    private var all: List<Channel> = emptyList()
    private var favorites: MutableSet<String> = mutableSetOf()
    private var query: String = ""
    private var group: String = GROUP_ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        setSupportActionBar(b.toolbar)

        repo = PlaylistRepository(this)
        favorites = repo.favorites().toMutableSet()

        adapter = ChannelAdapter(
            isFavorite = { favorites.contains(it.url) },
            onClick = { play(it) },
            onFavorite = { ch ->
                if (repo.toggleFavorite(ch.url)) favorites.add(ch.url) else favorites.remove(ch.url)
                if (group == GROUP_FAVORITES) applyFilter()
            }
        )
        b.list.layoutManager = LinearLayoutManager(this)
        b.list.adapter = adapter

        reloadLocal()
        // Refresh any custom playlists in the background (silently).
        if (repo.customUrls().isNotEmpty()) refreshOnline(showToast = false)
    }

    // ---------- data ----------

    private fun reloadLocal() {
        all = dedupe(repo.loadBundled() + repo.loadCached())
        buildChips()
        applyFilter()
    }

    private fun dedupe(list: List<Channel>): List<Channel> {
        val seen = HashSet<String>()
        return list.filter { seen.add(it.url) }
    }

    private fun buildChips() {
        val groups = all.map { it.group }.distinct().sorted()
        val labels = listOf(GROUP_ALL, GROUP_FAVORITES) + groups
        if (group !in labels) group = GROUP_ALL

        b.chips.removeAllViews()
        for (label in labels) {
            val chip = layoutInflater.inflate(R.layout.item_chip, b.chips, false) as Chip
            chip.text = label
            chip.isChecked = label == group
            chip.setOnClickListener {
                group = label
                applyFilter()
            }
            b.chips.addView(chip)
        }
    }

    private fun applyFilter() {
        val q = query.trim().lowercase()
        val list = all.filter { ch ->
            val inGroup = when (group) {
                GROUP_ALL -> true
                GROUP_FAVORITES -> favorites.contains(ch.url)
                else -> ch.group == group
            }
            inGroup && (q.isEmpty() || ch.name.lowercase().contains(q))
        }
        adapter.submitList(list)
        b.empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        b.empty.setText(if (group == GROUP_FAVORITES && q.isEmpty()) R.string.no_favorites else R.string.no_results)
        b.count.text = getString(R.string.channel_count, list.size)
    }

    private fun play(ch: Channel) {
        val current = adapter.currentList
        PlayQueue.list = current
        PlayQueue.index = current.indexOfFirst { it.url == ch.url }.coerceAtLeast(0)
        startActivity(Intent(this, PlayerActivity::class.java))
    }

    // ---------- menu ----------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val search = menu.findItem(R.id.action_search).actionView as SearchView
        search.queryHint = getString(R.string.search_hint)
        search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(text: String?) = true
            override fun onQueryTextChange(text: String?): Boolean {
                query = text ?: ""
                applyFilter()
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_add -> { showAddDialog(); true }
        R.id.action_refresh -> { refreshOnline(showToast = true); true }
        R.id.action_reset -> { confirmReset(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun showAddDialog() {
        val input = EditText(this).apply {
            hint = "https://example.com/playlist.m3u"
            setSingleLine()
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.add_playlist)
            .setMessage(R.string.add_playlist_hint)
            .setView(input)
            .setPositiveButton(R.string.add) { _, _ ->
                val url = input.text.toString().trim()
                if (url.startsWith("http://") || url.startsWith("https://")) addPlaylist(url)
                else Toast.makeText(this, R.string.invalid_url, Toast.LENGTH_LONG).show()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun addPlaylist(url: String) {
        b.progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val added = repo.fetch(url)
                repo.addUrl(url)
                reloadLocal()
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.playlist_added, added.size),
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.playlist_failed, e.message ?: e.javaClass.simpleName),
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                b.progress.visibility = View.GONE
            }
        }
    }

    private fun refreshOnline(showToast: Boolean) {
        if (repo.customUrls().isEmpty()) {
            if (showToast) Toast.makeText(this, R.string.nothing_to_refresh, Toast.LENGTH_SHORT).show()
            return
        }
        b.progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            val failed = repo.refreshAll()
            reloadLocal()
            b.progress.visibility = View.GONE
            if (showToast) {
                val msg = if (failed == 0) getString(R.string.refreshed)
                else getString(R.string.refresh_failed, failed)
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle(R.string.reset_title)
            .setMessage(R.string.reset_message)
            .setPositiveButton(R.string.reset) { _, _ ->
                repo.clearCustom()
                reloadLocal()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        favorites = repo.favorites().toMutableSet()
        if (::adapter.isInitialized) applyFilter()
    }

    companion object {
        const val GROUP_ALL = "All"
        const val GROUP_FAVORITES = "★ Favorites"
    }
}
