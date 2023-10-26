import { registerPlugin } from '@capacitor/core';
const Matter = registerPlugin('Matter', {
    web: () => import('./web').then(m => new m.MatterWeb()),
});
export * from './definitions';
export { Matter };
//# sourceMappingURL=index.js.map