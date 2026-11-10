package com.kosking.app.util

import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashLogger {
    fun log(e: Exception, context: String? = null) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        if (context != null) crashlytics.log(context)
        crashlytics.recordException(e)
    }
}
