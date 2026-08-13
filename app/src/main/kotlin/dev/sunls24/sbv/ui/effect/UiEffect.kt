package dev.sunls24.sbv.ui.effect

sealed class UiEffect {
    data class ShowToast(val message: String) : UiEffect()
}