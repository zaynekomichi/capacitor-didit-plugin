package zw.co.aurorasystems.plugins.didit

import android.app.Application
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import me.didit.sdk.DiditSdk
import me.didit.sdk.VerificationResult

@CapacitorPlugin(name = "DiditVerification")
class DiditVerificationPlugin : Plugin() {

    @Volatile
    private var activeCallbackId: String? = null

    override fun load() {
        DiditSdk.initialize(context.applicationContext as Application)
    }

    override fun handleOnDestroy() {
        activeCallbackId = null
    }

    @PluginMethod
    fun startVerification(call: PluginCall) {
        val sessionToken = call.getString("sessionToken")
        if (sessionToken.isNullOrEmpty()) {
            call.reject("sessionToken is required", "MISSING_TOKEN")
            return
        }

        val currentActivity = activity
        if (currentActivity == null) {
            call.reject("Activity is not available", "UNAVAILABLE")
            return
        }

        if (activeCallbackId != null) {
            call.reject("A verification is already in progress", "BUSY")
            return
        }

        // Save the call so it survives configuration changes while the
        // SDK's activity is in the foreground.
        bridge.saveCall(call)
        activeCallbackId = call.callbackId

        currentActivity.runOnUiThread {
            DiditSdk.startVerification(token = sessionToken) { result ->
                // Clear the busy flag before the null-guard: a WebView reload
                // resets the bridge and drops saved calls, and the guard must
                // not leave the plugin permanently locked in that case.
                activeCallbackId = null

                // getSavedCall returning null means the call was already
                // settled or dropped by a bridge reset — guards against the
                // SDK firing the callback twice.
                val savedCall = bridge.getSavedCall(call.callbackId) ?: return@startVerification
                bridge.releaseCall(savedCall)

                when (result) {
                    is VerificationResult.Completed -> {
                        val ret = JSObject()
                        ret.put("status", mapStatus(result.session.status.name))
                        ret.put("sessionId", result.session.sessionId)
                        savedCall.resolve(ret)
                    }
                    is VerificationResult.Cancelled -> {
                        savedCall.reject("Verification cancelled", "CANCELLED")
                    }
                    is VerificationResult.Failed -> {
                        savedCall.reject(result.error.message ?: "Verification failed", "FAILED")
                    }
                }
            }
        }
    }

    private fun mapStatus(rawStatus: String): String = when (rawStatus.uppercase()) {
        "APPROVED" -> "Approved"
        "DECLINED" -> "Declined"
        else -> "Pending"
    }
}
