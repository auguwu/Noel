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

import ConfigurationComponent from './Config';
import type ComponentImpl from '../Component';
import type Subscription from '../interfaces/Subscription';
import { Component } from '../decorators';
import { Client } from 'wumpcord';
import Logger from '../Logger';

export default class DiscordComponent implements ComponentImpl {
  public priority = 1;
  private logger = Logger.get();
  public name = 'discord';

  @Component private config!: ConfigurationComponent;

  #client!: Client;

  init() {
    const token = this.config.getProperty('DISCORD_TOKEN')!;
    this.#client = new Client({
      interactions: true,
      getAllUsers: true,
      token,
      ws: {
        intents: ['guilds', 'guildMessages', 'guildMessageReactions', 'guildMembers']
      }
    });

    this.#client.on('ready', () => {
      this.logger.log(`[Discord] Connected as ${this.#client.user.tag}`);
      this.#client.setStatus('online', {
        name: 'who\'s cuter!',
        type: 5
      }); // "Competing in who's cuter!"
    });
  }

  get client() {
    return this.#client;
  }

  async load() {
    this.init();
    return this.#client.connect();
  }

  dispose() {
    return this.#client.disconnect(false);
  }

  subscribe(subscription: Subscription) {
    this.logger.debug(`Added subscription ${subscription.event}!`);
    this.#client.on(subscription.event, async (...args: any[]) => {
      try {
        await subscription.run(...args);
      } catch(ex) {
        this.logger.error(`Unable to execute event "${subscription.event}":\n`, ex);
      }
    });
  }
}
