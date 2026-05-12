package com.pokecompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import com.pokecompanion.data.profile.ProfileManager
import com.pokecompanion.ui.screens.AppContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Init ProfileManager here so profiles load even if the accessibility
        // service hasn't been enabled yet (e.g. first-time setup).
        ProfileManager.init(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                AppContent()
            }
        }
    }
}
