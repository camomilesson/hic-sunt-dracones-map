package com.andrei.dracones.data.remote

import com.andrei.dracones.data.model.MapThemeModel
import retrofit2.http.GET

interface MapThemeApi {
    @GET("camomilesson/1221849a8add935f72f9c59d21e288dc/raw/ef7a742d845fd97cb8418636109f0d1b63094ba9/map-themes.json")
    suspend fun getMapThemes(): List<MapThemeModel>
}
