// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 X Team
package io.github.bluew9.rasail

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.bluew9.rasail.ui.HomeScreen
import io.github.bluew9.rasail.ui.IdentityScreen
import io.github.bluew9.rasail.ui.RasailTheme
import io.github.bluew9.rasail.ui.ServerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Blocks screenshots, screen recording and the recent-apps preview.
        // Set before super.onCreate so no frame is ever capturable.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RasailTheme {
                var screen by rememberSaveable { mutableStateOf("home") }
                when (screen) {
                    "identity" -> IdentityScreen(onBack = { screen = "home" })
                    "server" -> ServerScreen(onBack = { screen = "home" })
                    else -> HomeScreen(
                        onOpenIdentity = { screen = "identity" },
                        onOpenServer = { screen = "server" }
                    )
                }
            }
        }
    }
}
