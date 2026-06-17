package com.andrei.dracones.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.andrei.dracones.ui.map.MapScreen
import com.andrei.dracones.ui.progress.ProgressScreen
import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination : NavKey {
    @Serializable
    data object Map : Destination

    @Serializable
    data class Progress(val explorerName: String) : Destination
}

@Composable
fun AppNavigation() {
    val backStack = rememberNavBackStack(Destination.Map)

    val entryProvider: (NavKey) -> NavEntry<NavKey> = { key ->
        when (key) {
            is Destination.Map -> NavEntry(
                key = key,
            ) {
                MapScreen(
                    onNavigateToProgress = { name ->
                        (backStack as MutableList<NavKey>).add(Destination.Progress(name))
                    }
                )
            }
            is Destination.Progress -> NavEntry(
                key = key,
            ) {
                ProgressScreen(
                    explorerName = key.explorerName,
                    onNavigateBack = {
                        (backStack as MutableList<NavKey>).removeAt(backStack.size - 1)
                    }
                )
            }
            else -> error("Unknown destination")
        }
    }

    NavDisplay(
        backStack = backStack,
        onBack = { (backStack as MutableList<NavKey>).removeAt(backStack.size - 1) },
        entryProvider = entryProvider
    )
}
