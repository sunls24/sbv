package dev.sunls24.biliapi.entity.video.season

data class Section(
    val id: Long,
    val title: String,
    val episodes: List<Episode>
) {
    companion object {
        fun fromSection(section: dev.sunls24.biliapi.http.entity.video.UgcSeason.Section) =
            Section(
                id = section.id,
                title = section.title,
                episodes = section.episodes.map { Episode.fromEpisode(it) }
            )

        fun fromSection(section: dev.sunls24.biliapi.http.entity.season.SeasonSection) =
            Section(
                id = section.id,
                title = section.title,
                episodes = section.episodes.map { Episode.fromEpisode(it) }
                    .filter { it.aid != 0L }    // aid 为 0 的视频是跳转到其它 PGC 页面的“链接”，暂不适配（
            )
    }
}
