package zw.co.aurorasystems.plugins.didit

import android.app.Application
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.didit.sdk.DiditSdk
import me.didit.sdk.DiditSdkState
import me.didit.sdk.VerificationResult

@CapacitorPlugin(name = "DiditVerification")
class DiditVerificationPlugin : Plugin() {

    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var stateJob: Job? = null

    @Volatile
    private var activeCallbackId: String? = null

    override fun load() {
        DiditSdk.initialize(context.applicationContext as Application)
    }

    override fun handleOnDestroy() {
        stateJob?.cancel()
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
                stateJob?.cancel()

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

            // startVerification only prepares the session — unlike iOS, the
            // Android SDK never shows its UI on its own. Watch the state flow
            // and launch the UI once the session is ready.
            var launched = false
            stateJob?.cancel()
            stateJob = mainScope.launch {
                DiditSdk.state.collect { state ->
                    when (state) {
                        is DiditSdkState.Ready -> {
                            if (!launched) {
                                launched = true
                                DiditSdk.launchVerificationUI(currentActivity)
                            }
                        }
                        is DiditSdkState.Error -> {
                            // Session preparation failed before the UI ever
                            // launched (bad token, network error) — the result
                            // callback won't fire, so settle the call here and
                            // stop watching, otherwise a later Ready emission
                            // could open the UI with no active call.
                            if (!launched) {
                                activeCallbackId = null
                                val savedCall = bridge.getSavedCall(call.callbackId)
                                if (savedCall != null) {
                                    bridge.releaseCall(savedCall)
                                    savedCall.reject(state.message, "FAILED")
                                }
                                stateJob?.cancel()
                            }
                        }
                        else -> {
                            // Idle / CreatingSession / Loading — keep waiting
                        }
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
