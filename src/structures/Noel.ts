/**
 * Copyright (c) 2020-2021 August
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import { FSComponentLoader, Plugin, PluginAPI } from '@ayanaware/bento';
import Loggaby from 'loggaby';
import DiscordComponent from './components/DiscordComponent';
import ConfigurationComponent from './components/ConfigurationComponent';
import { sleep } from '@augu/utils';
import { resolve } from 'path';

const logger = new Loggaby({
  format: `{cyan}[Noel ~ ${process.pid} ~ {level.color}{level.name}{cyan}]{cyan} `
});

export default class Noel implements Plugin {
  private fsLoader!: FSComponentLoader;

  public version: string;
  public api!: PluginAPI;
  public name: string = 'Noel';

  constructor() {
    this.version = (require('../../package.json')).version;
  }

  async onLoad() {
    logger.log('Initializing main runner component...');

    this.fsLoader = new FSComponentLoader();
    this.fsLoader.name = 'NoelFilesystemComponentLoader';

    try {
      await this.api.bento.addPlugin(this.fsLoader);

      const config = await (this.fsLoader as any).createInstance(resolve(__dirname, 'components', 'ConfigurationComponent'));
      const discord = await (this.fsLoader as any).createInstance(resolve(__dirname, 'components', 'DiscordComponent'));

      await this.api.bento.addComponent(config);
      await this.api.bento.addComponent(discord);
    } catch(ex) {
      logger.error('Unable to initialize Noel component:\n', ex);
      throw ex;
    }
  }
}
