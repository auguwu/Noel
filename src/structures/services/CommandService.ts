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

import type { MessageCreateEvent, TextChannel } from 'wumpcord';
import { Component, Subscribe } from '../decorators';
import { Collection } from '@augu/collections';
import { HttpClient } from '@augu/orchid';
import T2DParser from '../parsers/DiscordToTelegram';
import Database from '../components/Database';
import Telegram from '../components/Telegram';
import Discord from '../components/Discord';
import Config from '../components/Config';
import Logger from '../Logger';

const { version } = require('../../../package.json');

interface CommandModule {
  commands: Collection<string, any>;
  name: string;
}

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
  @Component private config!: Config;

  async load() {
    this.logger.log('Loading commands...');

    // noop :^)
  }

  @Subscribe('message')
  private async onMessage(event: MessageCreateEvent<TextChannel>) {
    if (event.message.author.bot) return; // ignore bots
    if (!event.guild) return; // don't do anything without a guild

    // Get the user's metadata
    let user = await this.database.getUser(event.message.author.id);
    if (user === null) {
      await this.database.createUser(event.message.author.id);
      user = await this.database.getUser(event.message.author.id);
    }

    const channels = this.config.getProperty('TELEGRAM_RELAY_CHANNELS') ?? [];
    const tgGroupID = this.config.getProperty('TELEGRAM_GROUP_CHANNEL_ID');
    if (channels.includes(event.channel.id)) {
      const text = T2DParser.parse(event.message.embeds.length > 0 ? event.message.embeds[0] : `**${event.message.author.tag} | #${event.channel.name}**: ${event.message.content}`);
      if (tgGroupID !== undefined)
        return this.telegram.client.telegram.sendMessage(tgGroupID, text, { parse_mode: 'MarkdownV2' });
    }
  }
}
