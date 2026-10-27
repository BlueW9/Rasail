// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 X Team
package io.github.bluew9.rasail.net

import java.security.MessageDigest

object Fingerprints {
    /** First 8 bytes of SHA-256, lowercase hex in groups of four: "ab12 cd34 ef56 7890". */
    fun of(raw: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(raw)
            .take(8)
            .joinToString("") { "%02x".format(it) }
            .chunked(4)
            .joinToString(" ")
}
