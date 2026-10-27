// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 X Team
package io.github.bluew9.rasail.net

import android.content.Context
import android.util.Base64
import io.github.bluew9.rasail.identity.IdentityManager
import io.github.bluew9.rasail.identity.PrekeyManager
import org.json.JSONArray
import org.json.JSONObject

object ServerSetup {
    class Result(val fingerprint: String, val newlyRegistered: Boolean, val oneTimeKeys: Int)

    private const val PREFS = "rasail"
    private const val KEY_URL = "server_url"
    private const val KEY_PINNED = "server_key"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun pinnedFingerprint(context: Context): String? =
        prefs(context).getString(KEY_PINNED, null)?.let { Fingerprints.of(Base64.decode(it, Base64.NO_WRAP)) }

    fun forgetPinnedKey(context: Context) {
        prefs(context).edit().remove(KEY_PINNED).apply()
    }

    private fun normalize(raw: String): String {
        val url = raw.trim().trimEnd('/')
        require(url.startsWith("http://") || url.startsWith("https://")) {
            "The address must start with http:// or https://"
        }
        return url
    }

    /** Blocking: call from a background thread. */
    fun connect(context: Context, rawUrl: String): Result {
        val baseUrl = normalize(rawUrl)
        val prefs = prefs(context)

        // Trust on first use, then pin: a different key later means a different server.
        val serverKey = RelayApi.fetchServerKey(baseUrl)
        val pinned = prefs.getString(KEY_PINNED, null)?.let { Base64.decode(it, Base64.NO_WRAP) }
        if (pinned != null && !pinned.contentEquals(serverKey)) {
            throw IllegalStateException(
                "The server key is different from the one you pinned. Not connecting."
            )
        }
        if (pinned == null) {
            prefs.edit().putString(KEY_PINNED, Base64.encodeToString(serverKey, Base64.NO_WRAP)).apply()
        }
        prefs.edit().putString(KEY_URL, baseUrl).apply()

        val identity = IdentityManager.loadOrCreate(context)
        val material = PrekeyManager.loadOrCreate(context, identity)

        var newlyRegistered = false
        var account = RelayApi.signedRequest(baseUrl, identity, serverKey, "GET", "/v1/account")
        if (account.status == 404) {
            val body = JSONObject()
                .put("static_bundle", material.staticBundleJson())
                .put("one_time_prekeys", JSONArray(material.oneTimeKeysJson()))
                .toString()
                .toByteArray(Charsets.UTF_8)
            val put = RelayApi.signedRequest(baseUrl, identity, serverKey, "PUT", "/v1/keys", body)
            if (put.status != 200) {
                throw IllegalStateException("Registration failed (${put.status}): ${put.body}")
            }
            newlyRegistered = true
            account = RelayApi.signedRequest(baseUrl, identity, serverKey, "GET", "/v1/account")
        }
        if (account.status != 200) {
            throw IllegalStateException("Server answered ${account.status}: ${account.body}")
        }
        val count = JSONObject(account.body).getInt("one_time_prekeys")
        return Result(Fingerprints.of(serverKey), newlyRegistered, count)
    }
}
