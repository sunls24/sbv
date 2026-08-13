package dev.sunls24.biliapi.http.entity.home

import kotlinx.serialization.Serializable

@Serializable
data class RcmdTopData(
    val item: List<RcmdItem>
) {
    @Serializable
    data class RcmdItem(
        val bvid: String,
        val duration: Int,
        val goto: String,
        val id: Long,
        val owner: Owner? = null,
        val pic: String,
        val pubdate: Int,
        val stat: Stat? = null,
        val title: String
    ) {
        @Serializable
        data class Owner(
            val mid: Long,
            val name: String
        )

        @Serializable
        data class Stat(
            val danmaku: Int,
            val view: Int
        )
    }
}
