import SwiftUI
import UIKit
import DiditSDK

// MARK: - Result types passed back to the Capacitor plugin

struct DiditNativeResult {
    let status: String   // "Approved" | "Pending" | "Declined"
    let sessionId: String
}

enum DiditNativeError: LocalizedError {
    case cancelled
    case sdkError(String)

    var code: String {
        switch self {
        case .cancelled: return "CANCELLED"
        case .sdkError: return "FAILED"
        }
    }

    var errorDescription: String? {
        switch self {
        case .cancelled: return "Verification cancelled"
        case .sdkError(let msg): return msg
        }
    }
}

// MARK: - Transparent SwiftUI view that registers the DiditSDK callback

private struct DiditBridgeView: View {
    let onResult: (Result<DiditNativeResult, Error>) -> Void

    var body: some View {
        Color.clear
            .ignoresSafeArea()
            .diditVerification { sdkResult in
                switch sdkResult {
                case .completed(let session):
                    let status: String
                    switch session.status {
                    case .approved: status = "Approved"
                    case .declined: status = "Declined"
                    default: status = "Pending"
                    }
                    onResult(.success(DiditNativeResult(
                        status: status,
                        sessionId: session.sessionId
                    )))

                case .cancelled:
                    onResult(.failure(DiditNativeError.cancelled))

                case .failed(let error, _):
                    onResult(.failure(DiditNativeError.sdkError(error.localizedDescription)))
                }
            }
    }
}

// MARK: - Bridge that presents the SwiftUI host and launches the SDK

final class DiditVerificationBridge {

    static func present(
        from presentingVC: UIViewController,
        sessionToken: String,
        completion: @escaping (Result<DiditNativeResult, Error>) -> Void
    ) {
        // Present from the top-most controller — presenting from a controller
        // that already has a modal up fails silently in UIKit.
        var topVC = presentingVC
        while let presented = topVC.presentedViewController {
            topVC = presented
        }

        var hosting: UIHostingController<DiditBridgeView>?

        // Guard against the SDK firing the callback more than once (e.g. a
        // completed event followed by a cleanup event). The second invocation
        // would call resolve/reject on an already-settled CAPPluginCall, which
        // crashes the bridge on iOS 17+.
        var didComplete = false
        let onceCompletion: (Result<DiditNativeResult, Error>) -> Void = { result in
            guard !didComplete else { return }
            didComplete = true
            DispatchQueue.main.async {
                hosting?.dismiss(animated: false)
                hosting = nil
                completion(result)
            }
        }

        let bridgeView = DiditBridgeView(onResult: onceCompletion)

        let hc = UIHostingController(rootView: bridgeView)
        hc.view.backgroundColor = .clear
        // overFullScreen keeps the underlying UI visible while DiditSDK
        // presents its own screens on top.
        hc.modalPresentationStyle = .overFullScreen
        hosting = hc

        topVC.present(hc, animated: false) {
            // The SwiftUI environment with .diditVerification is now active;
            // the SDK finds the top view controller and the registered
            // callback through the environment.
            DiditSdk.shared.startVerification(token: sessionToken)
        }
    }
}
