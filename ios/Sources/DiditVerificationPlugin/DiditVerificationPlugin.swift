import Foundation
import Capacitor

@objc(DiditVerificationPlugin)
public class DiditVerificationPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "DiditVerificationPlugin"
    public let jsName = "DiditVerification"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "startVerification", returnType: CAPPluginReturnPromise)
    ]

    private var isVerificationActive = false

    @objc func startVerification(_ call: CAPPluginCall) {
        guard let sessionToken = call.getString("sessionToken"), !sessionToken.isEmpty else {
            call.reject("sessionToken is required", "MISSING_TOKEN")
            return
        }

        // Keep the call alive across the async gap so the bridge doesn't
        // release it before the SDK callback fires.
        call.keepAlive = true

        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }

            let finish: (CAPPluginCall) -> Void = { [weak self] call in
                self?.bridge?.releaseCall(call)
            }

            guard !self.isVerificationActive else {
                call.reject("A verification is already in progress", "BUSY")
                finish(call)
                return
            }

            guard let rootVC = self.bridge?.viewController else {
                call.reject("No view controller available", "UNAVAILABLE")
                finish(call)
                return
            }

            self.isVerificationActive = true

            DiditVerificationBridge.present(from: rootVC, sessionToken: sessionToken) { [weak self] result in
                self?.isVerificationActive = false

                switch result {
                case .success(let verification):
                    call.resolve([
                        "status": verification.status,
                        "sessionId": verification.sessionId
                    ])
                case .failure(let error):
                    let code = (error as? DiditNativeError)?.code ?? "FAILED"
                    call.reject(error.localizedDescription, code)
                }
                finish(call)
            }
        }
    }
}
