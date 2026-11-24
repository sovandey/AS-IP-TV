package com.asiptv.app

data class Channel(
    val name: String,
    val url: String,
    val logo: String?,
    val group: String
)

/** Holds the channel list that the player screen steps through with Prev/Next. */
object PlayQueue {
    var list: List<Channel> = emptyList()
    var index: Int = 0
}
