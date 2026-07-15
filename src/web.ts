import { WebPlugin } from '@capacitor/core';

import type { DiditVerificationPlugin, DiditVerificationResult } from './definitions';

export class DiditVerificationWeb extends WebPlugin implements DiditVerificationPlugin {
  async startVerification(): Promise<DiditVerificationResult> {
    throw this.unavailable(
      'The Didit native SDK is not available on web. Use the Didit web verification URL instead.',
    );
  }
}
