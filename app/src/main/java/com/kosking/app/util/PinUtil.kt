package com.kosking.app.util

import java.security.MessageDigest

object PinUtil {
    fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(pin.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
