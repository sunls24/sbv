package dev.sunls24.biliapi.entity.search


data class Hotword(
    val keyword: String,
    val showName: String,
    val icon: String?,
) {
    companion object {
        fun fromHttpWebHotword(hotword: dev.sunls24.biliapi.http.entity.search.Hotword) =
            Hotword(
                keyword = hotword.keyword,
                showName = hotword.showName,
                icon = hotword.icon
            )
    }
}
