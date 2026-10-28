// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 X Team
package io.github.bluew9.rasail.identity

import android.content.Context
import io.github.bluew9.rasail.crypto.KeystoreVault
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.kem.KEMKeyPair
import org.signal.libsignal.protocol.kem.KEMKeyType
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import java.io.File
import java.io.IOException
import java.security.SecureRandom

object PrekeyManager {
    private const val FILE = "prekeys.bin"
    private const val ONE_TIME_COUNT = 100

    /** Generates the prekeys once, then always returns the stored (encrypted) set. */
    @Synchronized
    fun loadOrCreate(context: Context, identity: Identity): PrekeyMaterial {
        val file = File(context.filesDir, FILE)
        if (file.exists()) {
            val plain = KeystoreVault.decrypt(file.readBytes())
            try {
                return PrekeyMaterial.decode(plain)
            } finally {
                plain.fill(0)
            }
        }
        val material = generate(identity)
        val plain = material.encode()
        try {
            val tmp = File(context.filesDir, "$FILE.tmp")
            tmp.writeBytes(KeystoreVault.encrypt(plain))
            if (!tmp.renameTo(file)) throw IOException("could not store prekeys")
        } finally {
            plain.fill(0)
        }
        return material
    }

    private fun generate(identity: Identity): PrekeyMaterial {
        val identityPrivate = identity.keyPair.privateKey
        val now = System.currentTimeMillis()

        val signedPair = Curve.generateKeyPair()
        val signedSignature = identityPrivate.calculateSignature(signedPair.publicKey.serialize())
        val signed = SignedPreKeyRecord(1, now, signedPair, signedSignature)

        val kyberPair = KEMKeyPair.generate(KEMKeyType.KYBER_1024)
        val kyberSignature = identityPrivate.calculateSignature(kyberPair.publicKey.serialize())
        val kyber = KyberPreKeyRecord(1, now, kyberPair, kyberSignature)

        val oneTime = (1..ONE_TIME_COUNT).map { PreKeyRecord(it, Curve.generateKeyPair()) }
        val registrationId = SecureRandom().nextInt(16380) + 1
        return PrekeyMaterial(registrationId, signed, kyber, oneTime)
    }
}
