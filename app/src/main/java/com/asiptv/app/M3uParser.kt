package com.asiptv.app

object M3uParser {

    private val attrRegex = Regex("""([\w-]+)="([^"]*)"""")

    fun parse(text: String): List<Channel> {
        val out = ArrayList<Channel>()
        var name: String? = null
        var logo: String? = null
        var group = "Other"

        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF")) {
                val attrs = attrRegex.findAll(line)
                    .associate { it.groupValues[1].lowercase() to it.groupValues[2] }
                val title = line.substringAfterLast(',', "").trim()
                name = title.ifEmpty { attrs["tvg-name"] ?: "Unknown" }
                logo = attrs["tvg-logo"]?.takeIf { it.isNotBlank() }
                group = attrs["group-title"]?.takeIf { it.isNotBlank() } ?: "Other"
            } else if (!line.startsWith("#")) {
                val n = name
                if (n != null) out.add(Channel(n, line, logo, group))
                name = null
                logo = null
                group = "Other"
            }
        }
        return out
    }
}
