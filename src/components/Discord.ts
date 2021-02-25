
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

import { Component, ComponentAPI, Inject, Subscribe } from '@ayanaware/bento';
import Logger from '@ayanaware/logger';
import { Client } from 'wumpcord';
import Config from './Config';

const logger = Logger.get();

export default class Discord implements Component {
  public api!: ComponentAPI;
  public name = 'Discord';

  @Inject(Config)
  private config!: Config;
  public client!: Client;

  async onLoad() {
    const token = this.config.getProperty('DISCORD_TOKEN');
    this.client = new Client({
      getAllUsers: true,
      token,
      ws: {
        intents: ['guilds', 'guildMessages', 'guildMembers', 'guildMessageReactions']
      }
    });

    this.api.forwardEvents(this.client, [
      'ready',
      //'guildMemberAdd',
      //'guildMemberRemove',
      //'guildMemberUpdate',
      'message',
      //'interactionReceive'
    ]);

    return this.client.connect();
  }

  async onUnload() {
    this.client.disconnect(false);
  }

  @Subscribe(Discord, 'ready')
  async onReady() {
    logger.info(`Ready as ${this.client.user.tag}!`);
    this.client.setStatus('online', {
      name: 'who\'s cuter!',
      type: 5
    });
  }
}
