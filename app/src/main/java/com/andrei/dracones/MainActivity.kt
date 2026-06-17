package com.andrei.dracones

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.andrei.dracones.ui.navigation.AppNavigation
import com.andrei.dracones.ui.theme.HicSuntDraconesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HicSuntDraconesTheme {
                AppNavigation()
            }
        }
    }
}
