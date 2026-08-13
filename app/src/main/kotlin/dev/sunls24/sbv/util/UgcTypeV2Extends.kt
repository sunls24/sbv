package dev.sunls24.sbv.util

import android.content.Context
import dev.sunls24.biliapi.entity.ugc.UgcTypeV2
import dev.sunls24.sbv.R

fun UgcTypeV2.getDisplayName(context: Context): String = context.getString(
    when (this) {
        UgcTypeV2.Douga -> R.string.ugc_type_v2_douga
        UgcTypeV2.Game -> R.string.ugc_type_v2_game
        UgcTypeV2.Kichiku -> R.string.ugc_type_v2_kichiku
        UgcTypeV2.Music -> R.string.ugc_type_v2_music
        UgcTypeV2.Dance -> R.string.ugc_type_v2_dance
        UgcTypeV2.Cinephile -> R.string.ugc_type_v2_cinephile
        UgcTypeV2.Ent -> R.string.ugc_type_v2_ent
        UgcTypeV2.Knowledge -> R.string.ugc_type_v2_knowledge
        UgcTypeV2.Tech -> R.string.ugc_type_v2_tech
        UgcTypeV2.Information -> R.string.ugc_type_v2_information
        UgcTypeV2.Food -> R.string.ugc_type_v2_food
        UgcTypeV2.LifeJoy -> R.string.ugc_type_v2_life_joy
        UgcTypeV2.Car -> R.string.ugc_type_v2_car
        UgcTypeV2.Fashion -> R.string.ugc_type_v2_fashion
        UgcTypeV2.Sports -> R.string.ugc_type_v2_sports
        UgcTypeV2.Animal -> R.string.ugc_type_v2_animal
    }
)
