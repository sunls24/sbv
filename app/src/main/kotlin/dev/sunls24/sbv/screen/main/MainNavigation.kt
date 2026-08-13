package dev.sunls24.sbv.screen.main

import dev.sunls24.sbv.component.HomeTopNavItem

sealed interface MainDestination {
    val section: MainSection
    val displayName: String

    data class Home(val tab: HomeTopNavItem) : MainDestination {
        override val section = MainSection.Home
        override val displayName = tab.getDisplayName()
    }

    data object Ugc : MainDestination {
        override val section = MainSection.Ugc
        override val displayName = "分区"
    }

    data object Personal : MainDestination {
        override val section = MainSection.Personal
        override val displayName = "个人"
    }

    data object Search : MainDestination {
        override val section = MainSection.Search
        override val displayName = "搜索"
    }
}

enum class MainSection {
    Home,
    Ugc,
    Personal,
    Search,
}
