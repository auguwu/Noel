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

/* eslint-disable camelcase */

import type { MessageCreateEvent } from 'wumpcord';
import { Component, Subscribe } from '../decorators';
import { Collection } from '@augu/collections';
import { HttpClient } from '@augu/orchid';
import Database from '../components/Database';
import Telegram from '../components/Telegram';
import Discord from '../components/Discord';
import Logger from '../Logger';

const { version } = require('../../../package.json');

interface CommandModule {
  commands: Collection<string, any>;
  name: string;
}

const replaceEw = (text: string) =>
  text
    .replaceAll('#', '\\#')
    .replaceAll('.', '\\.')
    .replaceAll(/<a?/gi, '')
    .replaceAll(/([0-9]+)>/gi, '');

export default class CommandService {
  private references: Collection<any, string> = new Collection();
  private ratelimits: Collection<string, Collection<string, number>> = new Collection();
  public commands: Collection<string, any> = new Collection();
  public modules: Collection<string, CommandModule> = new Collection();
  private logger = Logger.get();
  private http = new HttpClient({
    userAgent: `Noel (https://github.com/auguwu/Noel, v${version})`
  });

  @Component private telegram!: Telegram;
  @Component private database!: Database;
  @Component private discord!: Discord;

  async load() {
    this.logger.log('Loading commands...');

    // noop :^)
  }

  @Subscribe('message')
  private async onMessage(event: MessageCreateEvent) {
    if (event.message.author.bot) return; // if the author is a bot
    if (!event.guild) return; // don't do anything without a guild

    // Get the user's metadata
    let user = await this.database.getUser(event.message.author.id);
    if (user === null) {
      await this.database.createUser(event.message.author.id);
      user = await this.database.getUser(event.message.author.id);
    }

    if (event.channel.id === '814971471151104010' || event.channel.id === '794101954158526474')
      await this.telegram.client.telegram.sendMessage('-1001451058226', replaceEw(`**${event.message.author.tag} in [#${(event.channel as any).name}]**: ${event.message.content}`), { parse_mode: 'MarkdownV2' });
  }
}
