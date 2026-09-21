package com.pilahito.cloudterm.android.auth

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

/** TOTP RFC 6238 (SHA-1, 30 s, 6 dígitos). El secreto no se registra. */
object Totp {
    fun now(secretBase32: String, digits: Int = 6, periodSec: Long = 30L): String {
        val key = decodeBase32(secretBase32.filter { !it.isWhitespace() })
        val counter = System.currentTimeMillis() / 1000L / periodSec
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(key, "HmacSHA1"))
        val hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array())
        val offset = hash.last().toInt() and 0x0f
        val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
            ((hash[offset + 1].toInt() and 0xff) shl 16) or
            ((hash[offset + 2].toInt() and 0xff) shl 8) or
            (hash[offset + 3].toInt() and 0xff)
        val otp = binary % 10.0.pow(digits).toInt()
        return otp.toString().padStart(digits, '0')
    }

    fun looksLikeTotpPrompt(prompt: String): Boolean {
        val p = prompt.lowercase()
        return listOf("otp", "totp", "2fa", "verification code", "authenticator", "código", "codigo", "token").any { p.contains(it) }
    }

    private fun decodeBase32(input: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        val clean = input.uppercase().replace("=", "").filter { it in alphabet }
        var buffer = 0
        var bits = 0
        val out = ArrayList<Byte>()
        for (c in clean) {
            buffer = (buffer shl 5) or alphabet.indexOf(c)
            bits += 5
            if (bits >= 8) {
                bits -= 8
                out.add(((buffer shr bits) and 0xff).toByte())
            }
        }
        return out.toByteArray()
    }
}

object HostFingerprint {
    fun extract(message: String): String? {
        val sha = Regex("SHA256:[A-Za-z0-9+/=]+").find(message)?.value
        val md5 = Regex("MD5(?::[0-9a-fA-F]{2}){16}").find(message)?.value
        return sha ?: md5
    }
}
