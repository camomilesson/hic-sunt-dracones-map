package com.andrei.dracones.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ShareLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.andrei.dracones.domain.theme.MapStyleBuilder
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState

private const val MIN_ZOOM_FOR_FOG = 1f

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = viewModel(),
    parentH3Index: String? = null,
    parentResolution: Int? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(parentH3Index, parentResolution) {
        if (parentH3Index != null && parentResolution != null) {
            viewModel.setFocusedRegion(parentH3Index, parentResolution)
        }
    }

    val activeTheme = uiState.availableThemes.firstOrNull { it.name == uiState.mapTheme }
        ?: uiState.availableThemes.firstOrNull()

    val mapStyleJson = remember(
        uiState.showBusinesses,
        uiState.showTransit,
        uiState.showOtherPoi,
        activeTheme
    ) {
        MapStyleBuilder.buildStyleJson(
            theme = activeTheme,
            showBusinesses = uiState.showBusinesses,
            showTransit = uiState.showTransit,
            showOtherPoi = uiState.showOtherPoi
        )
    }

    val mapProperties = remember(mapStyleJson) {
        MapProperties(
            mapStyleOptions = if (mapStyleJson == "[]") null else MapStyleOptions(mapStyleJson)
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        viewModel.onLocationPermissionResult(granted)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(
                uiState.initialCameraPosition.latitude,
                uiState.initialCameraPosition.longitude
            ),
            uiState.initialCameraPosition.zoom
        )
    }

    LaunchedEffect(uiState.focusedRegion) {
        val region = uiState.focusedRegion
        if (region != null) {
            val builder = LatLngBounds.builder()
            region.boundary.forEach { builder.include(it) }
            val bounds = builder.build()

            try {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(bounds, 120)
                )
            } catch (_: Exception) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(bounds.center, 15f)
                )
            }
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (
            cameraPositionState.isMoving &&
            cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE
        ) {
            viewModel.setFollowing(false)
            viewModel.clearFocusedRegion()
        }
    }

    LaunchedEffect(uiState.lastKnownLocation, uiState.isFollowingUser) {
        val location = uiState.lastKnownLocation
        if (uiState.isFollowingUser && location != null) {
            val currentZoom = cameraPositionState.position.zoom
            val targetZoom =
                if (uiState.shouldApplyDefaultZoom) {
                    MapViewModel.DEFAULT_EXPLORATION_ZOOM
                } else {
                    currentZoom
                }

            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(location, targetZoom)
            )

            if (uiState.shouldApplyDefaultZoom) {
                viewModel.onInitialDiveCompleted()
            }
        }
    }

    fun hasRequiredTrackingPermissions(): Boolean {
        val fineLocationGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val notificationsGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted && coarseLocationGranted && notificationsGranted
    }

    fun requestTrackingPermissions() {
        val permissionsToRequest = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = remember { MapUiSettings(zoomControlsEnabled = false) },
            onMapClick = { viewModel.clearFocusedRegion() }
        ) {
            val currentZoom = cameraPositionState.position.zoom

            if (currentZoom >= MIN_ZOOM_FOR_FOG) {
                val projection = cameraPositionState.projection
                val visibleRegion = projection?.visibleRegion
                val bounds = visibleRegion?.latLngBounds

                if (bounds != null) {
                    val latSpan = bounds.northeast.latitude - bounds.southwest.latitude
                    val lngSpan = bounds.northeast.longitude - bounds.southwest.longitude

                    val latPadding = latSpan * 0.2
                    val lngPadding = lngSpan * 0.2

                    val south = bounds.southwest.latitude - latPadding
                    val north = bounds.northeast.latitude + latPadding
                    val west = bounds.southwest.longitude - lngPadding
                    val east = bounds.northeast.longitude + lngPadding

                    val outerPolygonPoints = listOf(
                        LatLng(south, west),
                        LatLng(north, west),
                        LatLng(north, east),
                        LatLng(south, east)
                    )

                    val holes = uiState.visitedRegionOutlines
                        .filter { outline ->
                            outline.any { point ->
                                point.latitude in south..north &&
                                        point.longitude in west..east
                            }
                        }

                    val pockets = uiState.unexploredPockets
                        .filter { pocket ->
                            pocket.any { point ->
                                point.latitude in south..north &&
                                        point.longitude in west..east
                            }
                        }

                    val fogColor = when (uiState.fogColorName) {
                        "Silvery" -> Color(0xFFC5D1D6)
                        "Blue" -> Color(0xFF2E3A52)
                        else -> Color(0xFFE0D2B8)
                    }

                    Polygon(
                        points = outerPolygonPoints,
                        holes = holes,
                        fillColor = fogColor.copy(alpha = uiState.fogOpacity),
                        strokeColor = Color.Transparent,
                        strokeWidth = 0f,
                        zIndex = 1f
                    )

                    pockets.forEach { pocket ->
                        Polygon(
                            points = pocket,
                            fillColor = fogColor.copy(alpha = uiState.fogOpacity),
                            strokeColor = Color.Transparent,
                            strokeWidth = 0f,
                            zIndex = 1f
                        )
                    }
                }
            }

            uiState.lastKnownLocation?.let { location ->
                MarkerComposable(
                    state = MarkerState(position = location),
                    title = "Current Position",
                    anchor = Offset(0.5f, 0.5f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(
                                Color(0xFF1A73E8).copy(alpha = 0.25f),
                                CircleShape
                            )
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(Color.White, CircleShape)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF1A73E8), CircleShape)
                            )
                        }
                    }
                }
            }

            uiState.focusedRegion?.let { region ->
                Polygon(
                    points = region.boundary,
                    fillColor = Color(0xFFFFB300).copy(alpha = 0.25f),
                    strokeColor = Color(0xFFFFB300),
                    strokeWidth = 8f,
                    zIndex = 2f
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 36.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Hic Sunt Dracones",
                style = MaterialTheme.typography.headlineLarge,
                color = Color(0xFF3B342C)
            )

            val greetingName = uiState.travelerName.trim()
            val greeting =
                if (greetingName.isBlank()) {
                    "Hello, traveler! Ready to explore?"
                } else {
                    "Hello, $greetingName! Ready to explore?"
                }

            Text(
                text = greeting,
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF4A4035)
            )

            uiState.permissionMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (uiState.isThemesLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Refreshing map themes...",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }

            uiState.themesErrorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (uiState.isWaitingForLocation) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Waiting for GPS fix...",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.End
        ) {
            AnimatedVisibility(visible = uiState.isTracking) {
                Column(horizontalAlignment = Alignment.End) {
                    FloatingActionButton(
                        onClick = {
                            viewModel.setFollowing(!uiState.isFollowingUser)
                        },
                        containerColor =
                            if (uiState.isFollowingUser) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        contentColor =
                            if (uiState.isFollowingUser) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Follow")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            ExtendedFloatingActionButton(
                text = { Text("Track") },
                icon = {
                    Icon(
                        Icons.Default.ShareLocation,
                        contentDescription = null
                    )
                },
                onClick = {
                    if (uiState.isTracking) {
                        viewModel.stopTracking()
                    } else {
                        if (hasRequiredTrackingPermissions()) {
                            viewModel.startTracking()
                        } else {
                            requestTrackingPermissions()
                        }
                    }
                },
                containerColor =
                    if (uiState.isTracking) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                contentColor =
                    if (uiState.isTracking) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
            )
        }
    }
}