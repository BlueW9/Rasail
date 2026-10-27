// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 X Team
package io.github.bluew9.rasail.identity

import android.util.Base64
import org.json.JSONObject
import org.signal.libsignal.protocol.state.KyberPreKeyRecord
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

/**
 * Everything a contact needs to start a session with you (public parts go to the
 * relay) plus the private halves, which stay on this device, encrypted.
 */
class PrekeyMaterial(
    val registrationId: Int,
    val signedPreKey: SignedPreKeyRecord,
    val kyberPreKey: KyberPreKeyRecord,
    val oneTimePreKeys: List<PreKeyRecord>
) {
    private fun b64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)

    /** Opaque to the relay. Clients verify the signatures with the identity key. */
    fun staticBundleJson(): String = JSONObject()
        .put("registration_id", registrationId)
        .put("signed_prekey_id", signedPreKey.id)
        .put("signed_prekey", b64(signedPreKey.keyPair.publicKey.serialize()))
        .put("signed_prekey_signature", b64(signedPreKey.signature))
        .put("kyber_prekey_id", kyberPreKey.id)
        .put("kyber_prekey", b64(kyberPreKey.keyPair.publicKey.serialize()))
        .put("kyber_prekey_signature", b64(kyberPreKey.signature))
        .toString()

    fun oneTimeKeysJson(): List<String> = oneTimePreKeys.map {
        JSONObject().put("id", it.id).put("key", b64(it.keyPair.publicKey.serialize())).toString()
    }

    /** Contains PRIVATE keys: only ever store the result encrypted. */
    fun encode(): ByteArray {
        val bytes = ByteArrayOutputStream()
        val out = DataOutputStream(bytes)
        out.writeInt(1)
        out.writeInt(registrationId)
        writeBlob(out, signedPreKey.serialize())
        writeBlob(out, kyberPreKey.serialize())
        out.writeInt(oneTimePreKeys.size)
        for (record in oneTimePreKeys) writeBlob(out, record.serialize())
        out.flush()
        return bytes.toByteArray()
    }

    private fun writeBlob(out: DataOutputStream, blob: ByteArray) {
        out.writeInt(blob.size)
        out.write(blob)
    }

    companion object {
        fun decode(data: ByteArray): PrekeyMaterial {
            val input = DataInputStream(ByteArrayInputStream(data))
            require(input.readInt() == 1) { "unknown prekey file version" }
            val registrationId = input.readInt()
            val signed = SignedPreKeyRecord(readBlob(input))
            val kyber = KyberPreKeyRecord(readBlob(input))
            val count = input.readInt()
            val oneTime = ArrayList<PreKeyRecord>(count)
            repeat(count) { oneTime.add(PreKeyRecord(readBlob(input))) }
            return PrekeyMaterial(registrationId, signed, kyber, oneTime)
        }

        private fun readBlob(input: DataInputStream): ByteArray {
            val size = input.readInt()
            require(size in 0..65536) { "corrupt prekey file" }
            val blob = ByteArray(size)
            input.readFully(blob)
            return blob
        }
    }
}
