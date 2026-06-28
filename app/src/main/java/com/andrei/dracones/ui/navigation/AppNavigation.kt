package com.andrei.dracones.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.andrei.dracones.ui.map.MapScreen
import com.andrei.dracones.ui.progress.ProgressScreen
import com.andrei.dracones.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination : NavKey {
    @Serializable
    data object Map : Destination

    @Serializable
    data object Progress : Destination

    @Serializable
    data object Settings : Destination
}

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(Destination.Map)

    val entryProvider: (NavKey) -> NavEntry<NavKey> = { key ->
        when (key) {
            is Destination.Map -> NavEntry(key = key) {
                MapScreen()
            }
            is Destination.Progress -> NavEntry(key = key) {
                ProgressScreen()
            }
            is Destination.Settings -> NavEntry(key = key) {
                SettingsScreen()
            }
            else -> error("Unknown destination: $key")
        }
    }

    Scaffold(
        bottomBar = {
            val currentDestination = backStack.lastOrNull() as? Destination
            NavigationBar {
                NavigationBarItem(
                    selected = currentDestination == Destination.Map || currentDestination == null,
                    onClick = {
                        if (currentDestination != Destination.Map) {
                            backStack.clear()
                            backStack.add(Destination.Map)
                        }
                    },
                    icon = { Icon(Icons.Default.Map, contentDescription = null) }
                )
                NavigationBarItem(
                    selected = currentDestination == Destination.Progress,
                    onClick = {
                        if (currentDestination != Destination.Progress) {
                            backStack.add(Destination.Progress)
                        }
                    },
                    icon = { Icon(Icons.Default.QueryStats, contentDescription = null) }
                )
                NavigationBarItem(
                    selected = currentDestination == Destination.Settings,
                    onClick = {
                        if (currentDestination != Destination.Settings) {
                            backStack.add(Destination.Settings)
                        }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                )
            }
        }
    ) { innerPadding ->
        NavDisplay(
            modifier = Modifier.padding(innerPadding),
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider
        )
    }
}
