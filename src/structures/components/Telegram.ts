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

import { EmbedBuilder, TextChannel } from 'wumpcord';
import { Telegraf, Context } from 'telegraf';
import { Color, Emojis } from '../../Constants';
import ComponentImpl from '../Component';
import { Component } from '../decorators';
import Discord from './Discord';
import Config from './Config';
import Logger from '../Logger';

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

      // Apply ctx to all middlewares
      this.#telegram.use((ctx, next) => {
        ctx.discord = this.discord;
        return next();
      });

      this.#telegram.on('new_chat_members', this.onNewChatMembers.bind(this));
      this.#telegram.on('left_chat_member', this.onLeftChatMember.bind(this));
      this.#telegram.on('poll', this.onNewPoll.bind(this));
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

  private async onNewPoll(context: NoelContext) {
    const update: any = context.update;

    const tgRelayChannelId = this.config.getProperty('TELEGRAM_RELAY_CHANNEL_ID');
    if (!tgRelayChannelId)
      return;

    const channel = context.discord.client.channels.get<TextChannel>(tgRelayChannelId);
    if (!channel)
      return;

    if (channel.type !== 'text')
      return;

    const from = update.message.from;
    const data = update.message.poll;

    const embed = new EmbedBuilder()
      .setTitle(`${data.question}${data.is_anoymous ? ' (Anonymous)' : data.is_closed ? ' (CLOSED)' : ''}`)
      .setDescription(data.options.map((r: any) => `• **${r.text}**`).join('\n'))
      .setColor(Color);

    return channel.send({
      content: `**${from.first_name}${from.last_name ? ` ${from.last_name}` : ''}** (@${from.username}) has created a poll!`,
      embed: embed.build()
    });
  }

  private onNewChatMembers(context: NoelContext) {
    const update: any = context.update;
    const tgRelayChannelId = this.config.getProperty('TELEGRAM_RELAY_CHANNEL_ID');
    if (!tgRelayChannelId)
      return;

    const channel = context.discord.client.channels.get<TextChannel>(tgRelayChannelId);
    if (!channel)
      return;

    if (channel.type !== 'text')
      return;

    // only one new person joined
    if (update.message.new_chat_members.length === 1) {
      const member = update.message.new_chat_members[0];
      return channel.send(`${Emojis.TransHeart} **${member.first_name}${member.last_name ? ` ${member.last_name}` : ''}** (@${member.username}) has joined from the Telegram side!`);
    } else {
      const members = update
        .message
        .new_chat_members
        .map((r: any) => `${r.first_name}${r.last_name ? ` ${r.last_name}` : ''} (@${r.username})`);

      return channel.send(`${Emojis.TransHeart} ${update.message.new_chat_members.length} members has joined!\n\`\`\`prolog\n${members.join('\n')}\`\`\``);
    }
  }

  private onLeftChatMember(context: NoelContext) {
    const update: any = context.update;
    const tgRelayChannelId = this.config.getProperty('TELEGRAM_RELAY_CHANNEL_ID');
    if (!tgRelayChannelId)
      return;

    const channel = context.discord.client.channels.get<TextChannel>(tgRelayChannelId);
    if (!channel)
      return;

    if (channel.type !== 'text')
      return;

    const member = update.message.left_chat_member;
    return channel.send(`:sob: **${member.first_name}${member.last_name ? ` ${member.last_name}` : ''}** (${member.username ? `@${member.username}` : '<anonymous>'}) has left from the Telegram side.`);
  }

  private async onMessageRelay(context: NoelContext) {
    const update: any = context.update;

    // Handling editing messages isn't available
    if (update.edited_message !== undefined)
      return;

    const botIcon = update.message?.from?.is_bot ? ' <:botIcon:814566565604360272>' : '';
    let text = '';

    if (update.message.reply_to_message !== undefined) {
      const message = update.message.reply_to_message;
      text = `> Replying to **${message.from.first_name}${message.from.last_name ? ` ${message.from.last_name}` : ''}**\n> ${message.caption ? message.caption : message.text}\n\n${update.message.text}`;
    } else {
      text = update.message.text;
    }

    const embed = new EmbedBuilder()
      .setColor(Color)
      .setAuthor(`${update.message.from.first_name}${update.message.from.last_name ? ` ${update.message.from.last_name}` : ''} (${update.message.from.username ? `@${update.message.from.username}` : '<anonymous>'})${botIcon}`)
      .setDescription(text);

    const tgRelayChannelId = this.config.getProperty('TELEGRAM_RELAY_CHANNEL_ID');
    if (!tgRelayChannelId)
      return;

    const channel = context.discord.client.channels.get<TextChannel>(tgRelayChannelId);
    if (!channel)
      return;

    if (channel.type !== 'text')
      return;

    await channel.send({
      embed: embed.build()
    });
  }
}
