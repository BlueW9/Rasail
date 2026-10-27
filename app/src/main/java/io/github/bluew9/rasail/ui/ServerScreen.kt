// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 X Team
package io.github.bluew9.rasail.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.bluew9.rasail.R
import io.github.bluew9.rasail.net.ServerSetup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ServerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rasail", Context.MODE_PRIVATE) }
    var url by remember { mutableStateOf(prefs.getString("server_url", null) ?: "http://127.0.0.1:8080") }
    var busy by remember { mutableStateOf(false) }
    var lines by remember { mutableStateOf(emptyList<String>()) }
    var fingerprint by remember { mutableStateOf(ServerSetup.pinnedFingerprint(context)) }
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)

    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
            Text(stringResource(R.string.back), color = colors.primary)
        }
        Text(
            text = stringResource(R.string.server_title),
            style = MaterialTheme.typography.headlineLarge,
            color = colors.onBackground
        )
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(Modifier.size(width = 56.dp, height = 1.dp), thickness = 1.dp, color = colors.primary)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = url,
            onValueChange = { url = it },
            label = { Text(stringResource(R.string.server_address)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.outlineVariant,
                focusedTextColor = colors.onBackground,
                unfocusedTextColor = colors.onBackground,
                cursorColor = colors.primary,
                focusedLabelColor = colors.primary,
                unfocusedLabelColor = colors.onBackground.copy(alpha = 0.7f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = {
                if (!busy) {
                    busy = true
                    lines = emptyList()
                    scope.launch {
                        lines = try {
                            val r = withContext(Dispatchers.IO) {
                                ServerSetup.connect(context.applicationContext, url)
                            }
                            fingerprint = r.fingerprint
                            listOf(
                                if (r.newlyRegistered) "Registered on this server." else "Already registered on this server.",
                                "One-time keys on server: ${r.oneTimeKeys}"
                            )
                        } catch (e: Exception) {
                            listOf("Failed: ${e.message ?: e.javaClass.simpleName}")
                        }
                        busy = false
                    }
                }
            },
            enabled = !busy,
            border = BorderStroke(1.dp, colors.primary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
        ) {
            Text(if (busy) "Working…" else stringResource(R.string.server_connect))
        }

        for (line in lines) {
            Spacer(Modifier.height(10.dp))
            Text(line, color = colors.onBackground, textAlign = TextAlign.Center)
        }

        val pinned = fingerprint
        if (pinned != null) {
            Spacer(Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.server_fingerprint),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.secondary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = pinned,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
                color = colors.onBackground
            )
            TextButton(onClick = {
                ServerSetup.forgetPinnedKey(context)
                fingerprint = null
            }) {
                Text(stringResource(R.string.server_forget), color = colors.primary)
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.server_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
