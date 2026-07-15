import { registerPlugin } from '@capacitor/core';

import type { DiditVerificationPlugin } from './definitions';

const DiditVerification = registerPlugin<DiditVerificationPlugin>('DiditVerification', {
  web: () => import('./web').then((m) => new m.DiditVerificationWeb()),
});

export * from './definitions';
export { DiditVerification };
