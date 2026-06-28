package com.andrei.dracones.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MapThemeModel(
    val id: String,
    val name: String,
    val description: String,
    val style: List<MapStyleRuleModel>
)

@Serializable
data class MapStyleRuleModel(
    val featureType: String? = null,
    val elementType: String? = null,
    val stylers: List<Map<String, String>>
)
