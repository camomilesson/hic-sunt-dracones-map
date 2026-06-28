package com.andrei.dracones.domain.diagnostics

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

object CrashReporter {
    private const val TAG = "CrashReporter"

    fun log(message: String) {
        Log.d(TAG, message)
        try {
            FirebaseCrashlytics.getInstance().log(message)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log to Crashlytics: $message", e)
        }
    }

    fun recordException(throwable: Throwable) {
        Log.e(TAG, "Recording non-fatal exception", throwable)
        try {
            FirebaseCrashlytics.getInstance().recordException(throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to record exception to Crashlytics", e)
        }
    }

    fun forceCrash() {
        log("Forcing a manual development crash via CrashReporter")
        throw RuntimeException("Test crash for Firebase Crashlytics verification")
    }
}
