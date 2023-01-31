import { registerPlugin } from '@capacitor/core';

import type { MatterPlugin } from './definitions';

const Matter = registerPlugin<MatterPlugin>('Matter', {
  web: () => import('./web').then(m => new m.MatterWeb()),
});

export * from './definitions';
export { Matter };
