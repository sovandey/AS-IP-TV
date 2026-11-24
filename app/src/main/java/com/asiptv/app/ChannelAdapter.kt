package com.asiptv.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.asiptv.app.databinding.ItemChannelBinding

class ChannelAdapter(
    private val isFavorite: (Channel) -> Boolean,
    private val onClick: (Channel) -> Unit,
    private val onFavorite: (Channel) -> Unit
) : ListAdapter<Channel, ChannelAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<Channel>() {
        override fun areItemsTheSame(a: Channel, b: Channel) = a.url == b.url
        override fun areContentsTheSame(a: Channel, b: Channel) = a == b
    }

    inner class VH(val b: ItemChannelBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ch = getItem(position)
        val b = holder.b
        b.name.text = ch.name
        b.group.text = ch.group

        if (ch.logo.isNullOrBlank()) {
            b.logo.setImageResource(R.drawable.ic_tv)
        } else {
            b.logo.load(ch.logo) {
                placeholder(R.drawable.ic_tv)
                error(R.drawable.ic_tv)
            }
        }

        b.star.setImageResource(if (isFavorite(ch)) R.drawable.ic_star else R.drawable.ic_star_border)
        b.star.setOnClickListener {
            onFavorite(ch)
            b.star.setImageResource(if (isFavorite(ch)) R.drawable.ic_star else R.drawable.ic_star_border)
        }
        b.root.setOnClickListener { onClick(ch) }
    }
}
