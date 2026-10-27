// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 X Team
package io.github.bluew9.rasail.net

import android.util.Base64
import io.github.bluew9.rasail.identity.Identity
import org.json.JSONObject
import org.signal.libsignal.protocol.ecc.ECPublicKey
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class RelayResponse(val status: Int, val body: String)

/** Minimal client for the Rasail relay (see rasail-server/README.md for the protocol). */
object RelayApi {
    private val random = SecureRandom()

    fun request(
        baseUrl: String,
        method: String,
        pathAndQuery: String,
        body: ByteArray = ByteArray(0),
        authorization: String? = null
    ): RelayResponse {
        val conn = URL(baseUrl + pathAndQuery).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 10_000
            conn.readTimeout = 40_000
            conn.instanceFollowRedirects = false
            if (authorization != null) conn.setRequestProperty("Authorization", authorization)
            if (body.isNotEmpty()) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setFixedLengthStreamingMode(body.size)
                conn.outputStream.use { it.write(body) }
            }
            val status = conn.responseCode
            val stream = if (status >= 400) conn.errorStream else conn.inputStream
            val text = stream?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
            return RelayResponse(status, text)
        } finally {
            conn.disconnect()
        }
    }

    /** Raw 32-byte X25519 public key of the relay. */
    fun fetchServerKey(baseUrl: String): ByteArray {
        val r = request(baseUrl, "GET", "/v1/info")
        if (r.status != 200) throw IllegalStateException("Server answered ${r.status}")
        val key = Base64.decode(JSONObject(r.body).getString("server_key"), Base64.NO_WRAP)
        if (key.size != 32) throw IllegalStateException("Unexpected server key")
        return key
    }

    /**
     * Proves ownership of the identity private key without sending any secret:
     * MAC the request with the X25519 secret shared with the relay.
     */
    private fun authorization(
        identity: Identity,
        serverKey: ByteArray,
        method: String,
        pathAndQuery: String,
        body: ByteArray
    ): String {
        val serverPublic = ECPublicKey(byteArrayOf(5) + serverKey)
        val secret = identity.keyPair.privateKey.calculateAgreement(serverPublic)
        val ts = System.currentTimeMillis() / 1000
        val nonce = Base64.encodeToString(ByteArray(12).also { random.nextBytes(it) }, Base64.NO_WRAP)
        val bodyHash = MessageDigest.getInstance("SHA-256").digest(body)
            .joinToString("") { "%02x".format(it) }
        val text = "rasail-auth-v1\n$method\n$pathAndQuery\n$ts\n$nonce\n$bodyHash"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret, "HmacSHA256"))
        val tag = mac.doFinal(text.toByteArray(Charsets.UTF_8))
        secret.fill(0)
        val identityKey = Base64.encodeToString(identity.publicKeyBytes, Base64.NO_WRAP)
        return "Rasail v1 $identityKey.$ts.$nonce.${Base64.encodeToString(tag, Base64.NO_WRAP)}"
    }

    fun signedRequest(
        baseUrl: String,
        identity: Identity,
        serverKey: ByteArray,
        method: String,
        pathAndQuery: String,
        body: ByteArray = ByteArray(0)
    ): RelayResponse = request(
        baseUrl, method, pathAndQuery, body,
        authorization(identity, serverKey, method, pathAndQuery, body)
    )
}
