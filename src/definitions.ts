/**
 * The final status of a completed Didit verification session.
 */
export type DiditVerificationStatus = 'Approved' | 'Pending' | 'Declined';

export interface DiditVerificationResult {
  /**
   * The session status reported by the Didit SDK when the flow completed.
   *
   * `Pending` means the session finished but is still being processed or
   * needs manual review — poll the Didit API from your backend for the
   * final decision.
   */
  status: DiditVerificationStatus;
  /**
   * The Didit verification session id, for reconciliation with your backend.
   */
  sessionId: string;
}

export interface StartVerificationOptions {
  /**
   * The session token created for this user via the Didit API
   * (`POST /v2/session/` from your backend).
   */
  sessionToken: string;
}

export interface DiditVerificationPlugin {
  /**
   * Launch the native Didit verification flow (document capture, liveness,
   * NFC where available) and resolve when the user finishes.
   *
   * Rejects with `code`:
   * - `MISSING_TOKEN` — `sessionToken` was absent or empty.
   * - `BUSY` — a verification flow is already running.
   * - `UNAVAILABLE` — no activity / view controller to present from,
   *   or the platform is web.
   * - `CANCELLED` — the user exited the flow before completing it.
   * - `FAILED` — the SDK reported an error (message has details).
   */
  startVerification(options: StartVerificationOptions): Promise<DiditVerificationResult>;
}
