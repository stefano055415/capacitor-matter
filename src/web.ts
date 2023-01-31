import { WebPlugin } from '@capacitor/core';

import type { MatterPlugin } from './definitions';

export class MatterWeb extends WebPlugin implements MatterPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
