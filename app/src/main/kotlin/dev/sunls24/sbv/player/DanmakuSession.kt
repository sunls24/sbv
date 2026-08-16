package dev.sunls24.sbv.player

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kuaishou.akdanmaku.DanmakuConfig
import com.kuaishou.akdanmaku.data.DanmakuItemData
import com.kuaishou.akdanmaku.ecs.component.filter.TypeFilter
import com.kuaishou.akdanmaku.render.SimpleRenderer
import com.kuaishou.akdanmaku.ui.DanmakuPlayer
import dev.sunls24.biliapi.http.entity.danmaku.DanmakuData
import dev.sunls24.sbv.component.controllers.DanmakuType
import dev.sunls24.sbv.ui.state.DanmakuState

internal class DanmakuSession {
    private val typeFilter = TypeFilter()
    private var config = DanmakuConfig()

    var player: DanmakuPlayer? = null
        private set

    fun initialize(state: DanmakuState): DanmakuPlayer {
        player?.let { return it }
        rebuildTypeFilter(state.enabledTypes)
        config = config.copy(
            density = 120,
            textSizeScale = state.scale,
            screenPart = state.area,
            dataFilter = listOf(typeFilter),
            rollingSpeedFactor = state.speedFactor
        ).also(DanmakuConfig::updateFilter)
        return DanmakuPlayer(SimpleRenderer()).also {
            player = it
            it.updateConfig(config)
        }
    }

    fun release() {
        player?.release()
        player = null
    }

    fun updateSettings(old: DanmakuState, new: DanmakuState) {
        if (new.enabledTypes != old.enabledTypes) {
            rebuildTypeFilter(new.enabledTypes)
            config.updateFilter()
            player?.updateConfig(config)
            player?.setDanmakuRollingSpeed(new.speedFactor)
        }
        if (new.scale != old.scale) applyConfig(config.copy(textSizeScale = new.scale), new.speedFactor)
        if (new.area != old.area) applyConfig(config.copy(screenPart = new.area), new.speedFactor)
        if (new.speedFactor != old.speedFactor) player?.setDanmakuRollingSpeed(new.speedFactor)
    }

    fun updateData(data: List<DanmakuItemData>) = player?.updateData(data)

    fun start() = player?.start()
    fun pause() = player?.pause()
    fun seekTo(positionMs: Long) = player?.seekTo(positionMs)
    fun updatePlaySpeed(speed: Float) = player?.updatePlaySpeed(speed)

    private fun applyConfig(newConfig: DanmakuConfig, rollingSpeed: Float) {
        config = newConfig
        player?.updateConfig(config)
        player?.setDanmakuRollingSpeed(rollingSpeed)
    }

    private fun rebuildTypeFilter(enabledTypes: List<DanmakuType>) {
        typeFilter.clear()
        DanmakuType.entries
            .filterNot(enabledTypes::contains)
            .map {
                when (it) {
                    DanmakuType.Rolling -> DanmakuItemData.DANMAKU_MODE_ROLLING
                    DanmakuType.Top -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
                    DanmakuType.Bottom -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
                }
            }
            .forEach(typeFilter::addFilterItem)
    }

    fun toItemData(data: DanmakuData) = DanmakuItemData(
        danmakuId = data.dmid,
        position = (data.time * 1000).toLong(),
        content = data.text,
        mode = when (data.type) {
            4 -> DanmakuItemData.DANMAKU_MODE_CENTER_BOTTOM
            5 -> DanmakuItemData.DANMAKU_MODE_CENTER_TOP
            else -> DanmakuItemData.DANMAKU_MODE_ROLLING
        },
        textSize = data.size,
        textColor = Color(data.color).toArgb()
    )
}
