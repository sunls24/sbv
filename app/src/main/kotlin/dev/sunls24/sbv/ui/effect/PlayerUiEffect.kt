package dev.sunls24.sbv.ui.effect

sealed class PlayerUiEffect {
    data object PlayEnded : PlayerUiEffect()
    data object FinishActivity: PlayerUiEffect()
}