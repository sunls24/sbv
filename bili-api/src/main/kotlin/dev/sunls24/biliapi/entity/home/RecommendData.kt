package dev.sunls24.biliapi.entity.home

import dev.sunls24.biliapi.entity.ugc.UgcItem

data class RecommendData(
    val items: List<UgcItem>,
    val nextPage: RecommendPage
)

data class RecommendPage(
    val nextWebIdx: Int = 1,
    val nextFetchRow: Int = 1,
    val showlistGroups: List<String> = emptyList()
)
