package com.andrei.dracones.domain.theme

import com.andrei.dracones.data.model.MapStyleRuleModel
import com.andrei.dracones.data.model.MapThemeModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object MapStyleBuilder {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun buildStyleJson(
        theme: MapThemeModel?,
        showBusinesses: Boolean,
        showTransit: Boolean,
        showOtherPoi: Boolean
    ): String {
        val rules = mutableListOf<MapStyleRuleModel>()

        // 1. Add base theme rules if present
        if (theme != null) {
            rules.addAll(theme.style)
        }

        // 2. Add dynamic local visibility filter rules
        if (!showBusinesses) {
            rules.add(
                MapStyleRuleModel(
                    featureType = "poi.business",
                    elementType = "all",
                    stylers = listOf(mapOf("visibility" to "off"))
                )
            )
        }
        if (!showTransit) {
            rules.add(
                MapStyleRuleModel(
                    featureType = "transit",
                    elementType = "all",
                    stylers = listOf(mapOf("visibility" to "off"))
                )
            )
        }
        if (!showOtherPoi) {
            val otherPoiTypes = listOf(
                "poi.attraction",
                "poi.government",
                "poi.medical",
                "poi.school",
                "poi.sports_complex",
                "poi.place_of_worship"
            )
            for (type in otherPoiTypes) {
                rules.add(
                    MapStyleRuleModel(
                        featureType = type,
                        elementType = "all",
                        stylers = listOf(mapOf("visibility" to "off"))
                    )
                )
            }
        }

        // 3. Global overrides (Hides driving direction/one-way arrows & shields)
        rules.add(
            MapStyleRuleModel(
                featureType = "road",
                elementType = "labels.icon",
                stylers = listOf(mapOf("visibility" to "off"))
            )
        )

        return json.encodeToString(rules)
    }
}
