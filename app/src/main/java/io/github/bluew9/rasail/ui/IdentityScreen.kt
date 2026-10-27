// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 X Team
package io.github.bluew9.rasail.ui

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.bluew9.rasail.R
import io.github.bluew9.rasail.identity.Identity
import io.github.bluew9.rasail.identity.IdentityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private sealed interface IdentityState {
    data object Loading : IdentityState
    data object Failed : IdentityState
    data class Ready(val identity: Identity) : IdentityState
}

@Composable
fun IdentityScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<IdentityState>(IdentityState.Loading) }

    LaunchedEffect(Unit) {
        state = withContext(Dispatchers.Default) {
            try {
                IdentityState.Ready(IdentityManager.loadOrCreate(context.applicationContext))
            } catch (e: Exception) {
                IdentityState.Failed
            }
        }
    }
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
            text = stringResource(R.string.identity_title),
            style = MaterialTheme.typography.headlineLarge,
            color = colors.onBackground
        )
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(Modifier.size(width = 56.dp, height = 1.dp), thickness = 1.dp, color = colors.primary)
        Spacer(Modifier.height(24.dp))

        when (val s = state) {
            IdentityState.Loading -> Text(
                stringResource(R.string.identity_loading),
                color = colors.onBackground
            )
            IdentityState.Failed -> Text(
                stringResource(R.string.identity_failed),
                color = colors.onBackground,
                textAlign = TextAlign.Center
            )
            is IdentityState.Ready -> IdentityContent(s.identity)
        }
    }
}

@Composable
private fun IdentityContent(identity: Identity) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .background(RasailColors.Ivory, RoundedCornerShape(16.dp))
            .border(BorderStroke(2.dp, colors.primary), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        QrCode(identity.shareCode, Modifier.size(220.dp))
    }
    Spacer(Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.identity_id_label),
        style = MaterialTheme.typography.bodyLarge,
        color = colors.secondary
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = identity.anonymousId,
        style = MaterialTheme.typography.bodyLarge,
        fontFamily = FontFamily.Monospace,
        color = colors.onBackground,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(24.dp))
    OutlinedButton(
        onClick = { copyCode(context, identity.shareCode) },
        border = BorderStroke(1.dp, colors.primary),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.primary)
    ) {
        Text(stringResource(R.string.identity_copy))
    }
    Spacer(Modifier.height(24.dp))
    Text(
        text = stringResource(R.string.identity_hint),
        style = MaterialTheme.typography.bodyLarge,
        color = colors.onBackground.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun copyCode(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Rasail code", text)
    if (Build.VERSION.SDK_INT >= 33) {
        // Keeps the value out of the keyboard's clipboard preview.
        clip.description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
    } else {
        Toast.makeText(context, R.string.identity_copied, Toast.LENGTH_SHORT).show()
    }
    clipboard.setPrimaryClip(clip)
}
