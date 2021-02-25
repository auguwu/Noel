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

import { Telegraf, Context, Composer, MiddlewareFn } from 'telegraf';
import ComponentImpl from '../Component';
import { Component } from '../decorators';
import Discord from './Discord';
import Config from './Config';
import Logger from '../Logger';
import { EmbedBuilder, TextChannel } from 'wumpcord';
import { Color } from '../../Constants';

type InferContext<T> = T extends MiddlewareFn<infer P> ? P : never;
type MatchedContext = InferContext<Parameters<Composer<Context>['on']>[1]> & { update: any };

interface NoelContext extends Context {
  discord: Discord;
}

export default class TelegramComponent implements ComponentImpl {
  public priority = 2;
  private logger = Logger.get();
  public name = 'telegram';

  @Component private discord!: Discord;
  @Component private config!: Config;

  #telegram!: Telegraf<NoelContext>;

  get client() {
    return this.#telegram;
  }

  async load() {
    this.logger.log('Creating new Telegram instance');
    const token = this.config.getProperty('TELEGRAM_TOKEN');
    if (token === undefined) {
      this.logger.warn('Missing `TELEGRAM_TOKEN` env variable, this is optional though!');
      return Promise.resolve();
    }

    if (!this.#telegram) {
      this.#telegram = new Telegraf(token);

      this.#telegram.start(Telegraf.reply('This is a private instance for @uwunoel (Telegram group), please refrain from using this instance.'));
      this.#telegram.help(Telegraf.reply('Go help yourself, cutie~ <3'));
      this.#telegram.on('new_chat_members', this.onNewChatMembers.bind(this));
      this.#telegram.on('left_chat_member', this.onLeftChatMember.bind(this));
      this.#telegram.use((ctx, next) => {
        ctx.discord = this.discord;
        return next();
      });

      this.#telegram.use((ctx, next) => {
        this.onMessageRelay(ctx);
        return next();
      });

      const me = await this.#telegram.telegram.getMe();
      this.logger.log(`Hello @${me.username} (${me.first_name})!`);

      await this.#telegram.launch();
    }
  }

  dispose() {
    this.#telegram.stop('noel:disposed');
  }

  private onNewChatMembers(context: MatchedContext) {
    console.trace('new member(s)', context);
  }

  private onLeftChatMember(context: MatchedContext) {
    console.trace('left member', context);
  }

  private async onMessageRelay(context: NoelContext) {
    const update: any = context.update;
    console.log(update);

    const botIcon = update.message.from.is_bot ? ' <:botIcon:814566565604360272>' : '';
    let text = '';

    if (update.message.reply_to_message !== undefined) {
      const message = update.message.reply_to_message;
      text = `> Replying to **${message.from.first_name}${message.from.last_name ? ` ${message.from.last_name}` : ''}**\n> ${message.text}\n\n${update.message.text}`;
    } else {
      text = update.message.text;
    }

    const embed = new EmbedBuilder()
      .setColor(Color)
      .setAuthor(`${update.message.from.first_name}${update.message.from.last_name ? ` ${update.message.from.last_name}` : ''} (@${update.message.from.username})${botIcon}`)
      .setDescription(text)
      .build();

    const tgRelayChannelId = this.config.getProperty('TELEGRAM_RELAY_CHANNEL_ID');
    if (!tgRelayChannelId)
      return;

    const channel = context.discord.client.channels.get<TextChannel>(tgRelayChannelId);
    if (!channel)
      return;

    if (channel.type !== 'text')
      return;

    await channel.send({
      embed
    });
  }
}
