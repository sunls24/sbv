package dev.sunls24.biliapi.entity.ugc.region

import dev.sunls24.biliapi.entity.ugc.UgcItem

data class UgcFeedData(
    val nextPage: UgcFeedPage,
    val items: List<UgcItem> = emptyList()
) {
    companion object {
        fun fromRegionFeedRcmd(
            data: dev.sunls24.biliapi.http.entity.region.RegionFeedRcmd,
            nextPage: UgcFeedPage
        ): UgcFeedData {
            return UgcFeedData(
                nextPage = nextPage,
                items = data.archives.map { UgcItem.fromRegionRcmdArchive(it) }
            )
        }
    }
}
