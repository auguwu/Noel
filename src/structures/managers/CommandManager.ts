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

import { EmbedBuilder, MessageCreateEvent } from 'wumpcord';
import { Component, Inject, Subscribe } from '@ayanaware/bento';
import { Collection } from '@augu/collections';
import { Ctor, readdir } from '@augu/utils';
import Discord from '../../components/Discord';
import Config from '../../components/Config';
import { Color } from '../../Constants';
import { join } from 'path';
import Logger from '@ayanaware/logger';
import Command, { CommandCategory } from '../Command';
import Noel from '../Noel';

const logger = Logger.get();

export default class CommandManager implements Component {
  private commands: Collection<string, Command> = new Collection();
  public name: string = 'Commands';

  @Inject(Noel)
  private bot!: Noel;

  @Inject(Discord)
  private discord!: Discord;

  @Inject(Config)
  private config!: Config;

  async onLoad() {
    const files = await readdir(join(__dirname, '..', '..', 'commands'));
    logger.info(`Found ${files.length} commands to initialize.`);

    for (let i = 0; i < files.length; i++) {
      const ctor: Ctor<Command> = await import(files[i]);
      const command = ctor.default ? new ctor.default() : new ctor();

      command.init(this.bot);
      logger.info(`Found & initialized command ${command.name}`);

      this.commands.set(command.name, command);
    }
  }

  findCommand(name: string) {
    return this.commands.filter(cmd =>
      cmd.name === name || cmd.aliases.includes(name)
    )[0];
  }

  @Subscribe(Discord, 'message')
  async onMessage(event: MessageCreateEvent) {
    // Ignore bots
    if (event.message.author.bot) return;

    // Check if we were just mentioned
    if ((new RegExp(`^<@!?${this.discord.client.user.id}>$`)).test(event.message.content))
      return event.channel.send(`Hello, **${event.message.author.id}**! Use \`${this.config.getProperty('PREFIX')}help\` for a list of commands.`);

    // Check for prefixes
    const mention = new RegExp(`^<@!?${this.discord.client.user.id}> `).exec(event.message.content);
    const _prefix = this.config.getProperty('PREFIX');
    const prefixes = [_prefix].filter(Boolean);

    if (mention !== null)
      prefixes.push(`${mention}`);

    let prefix: string | null = null;
    for (let i = 0; i < prefixes.length; i++) {
      const p = prefixes[i];
      if (event.message.content.startsWith(p)) {
        prefix = p;
        break;
      }
    }

    if (prefix === null)
      return;

    const args = event.message.content.slice(prefix.length).trim().split(/ +/g);
    const name = args.shift()!;
    const command = this.findCommand(name);

    if (!command) return;

    const owners = this.config.getProperty('OWNERS');
    if (command.category === CommandCategory.Cutie && !owners.includes(event.message.author.id))
      return event.channel.send('Well, you found a command that the owners can execute.');

    const staff = event.guild?.members.cache.filter(r => r.roles.find(r => r.name === 'Staff Members') !== undefined).map(r => r.id) ?? [];
    if (command.category === CommandCategory.Staff && !staff.includes(event.message.author.id))
      return event.channel.send('Well, only Staff Members can execute this.');

    try {
      await command.run(event.message as any, args);

      logger.info(`Command "${command.name} ${args.join(' ')}" was executed by ${event.message.author.tag}!`);
    } catch(ex) {
      const embed = new EmbedBuilder()
        .setColor(Color)
        .setTitle('Uh, oh... something broke... ;^;')
        .setDescription([
          `Looks like command **${command.name}** has failed to execute...`,
          `Report this to ${owners.map(owner => this.discord.client.users.get(owner)?.tag ?? 'Unknown User#0000').join(', ')}!`,
          '```js',
          `${ex.name}: ${ex.message}`,
          ...(ex.stack?.split('\n').slice(0) ?? []),
          '```js'
        ]);

      return event.channel.send({
        content: 'Uh oh... something broke... ;~;',
        embed: embed.build()
      });
    }
  }
}
