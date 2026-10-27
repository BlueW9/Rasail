// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 X Team
package io.github.bluew9.rasail.identity

import android.content.Context
import io.github.bluew9.rasail.crypto.KeystoreVault
import org.signal.libsignal.protocol.IdentityKeyPair
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/** The user's anonymous identity: a Signal identity key pair, nothing else. */
class Identity(val keyPair: IdentityKeyPair) {
    private val publicKeyBytes: ByteArray = keyPair.publicKey.serialize()

    /** 160-bit fingerprint of the public key, shown in groups of four. */
    val anonymousId: String = Base32.encode(
        MessageDigest.getInstance("SHA-256").digest(publicKeyBytes).copyOf(20)
    ).chunked(4).joinToString(" ")

    /** What a contact needs to start an encrypted chat with you (public data only). */
    val shareCode: String = "rasail:" + Base32.encode(publicKeyBytes)
}

object IdentityManager {
    private const val FILE = "identity.bin"

    /**
     * Loads the identity, or creates it on first run. The private key is only ever
     * written to disk encrypted by a Keystore key. If an existing file cannot be
     * decrypted this throws instead of silently creating a new identity.
     */
    @Synchronized
    fun loadOrCreate(context: Context): Identity {
        val file = File(context.filesDir, FILE)
        if (file.exists()) {
            val plain = KeystoreVault.decrypt(file.readBytes())
            try {
                return Identity(IdentityKeyPair(plain))
            } finally {
                plain.fill(0)
            }
        }
        val pair = IdentityKeyPair.generate()
        val plain = pair.serialize()
        try {
            val tmp = File(context.filesDir, "$FILE.tmp")
            tmp.writeBytes(KeystoreVault.encrypt(plain))
            if (!tmp.renameTo(file)) throw IOException("could not store identity")
        } finally {
            plain.fill(0)
        }
        return Identity(pair)
    }
}
