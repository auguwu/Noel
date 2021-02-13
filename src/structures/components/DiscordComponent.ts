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

import { Component, Subscribe, PluginReference, ComponentAPI, Inject } from '@ayanaware/bento';
import type ConfigurationComponent from './ConfigurationComponent';
import { Client } from 'wumpcord';
import Loggaby from 'loggaby';

const logger = new Loggaby({
  format: `{grey}[Discord / ${process.pid} / {level.name}]{grey} `
});

const EVENTS = [
  'ready',
  'guildMemberAdd',
  'guildMemberRemove',
  'guildMemberUpdate',
  'shardDisconnect',
  'shardReady',
  'shardResume'
];

export default class DiscordComponent implements Component {
  public client!: Client;
  public parent: PluginReference = null as any;
  public name: string = 'discord';
  public api!: ComponentAPI;

  @Inject()
  private config!: ConfigurationComponent;

  onLoad() {
    return this.connect();
  }

  disconnect() {
    return this.client.disconnect(false);
  }

  private connect() {
    logger.log('Now connecting to Discord...');

    const token = this.config.getProperty('DISCORD_TOKEN');
    this.client = new Client({
      interactions: true,
      getAllUsers: true,
      token,
      ws: {
        intents: [
          'guilds',
          'guildMessages',
          'guildMembers'
        ]
      }
    });

    this.api.forwardEvents(this.client as any, EVENTS);
    return this.client.connect()
      .then(() => logger.log('Successfully established a connection with Discord.'))
      .catch(error =>
        logger.error('Unable to establish a connection with Discord', error)
      );
  }

  @Subscribe(DiscordComponent, 'ready')
  private async onReady() {
    logger.log(`Ready as ${this.client.user.tag}!`);

    this.client.setStatus('online', {
      name: 'who\'s cuter',
      type: 5
    });
  }
}
