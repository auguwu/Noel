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

import { Component, PluginReference, Subscribe } from '@ayanaware/bento';
import { Collection } from '@augu/collections';
import Discord from '../components/DiscordComponent';
import { Command } from '../Command';
import Noel from '../Noel';
import type { MessageCreateEvent } from 'wumpcord';

export default class CommandManager implements Component {
  private commands: Collection<string, Command> = new Collection();
  public parent: PluginReference = Noel;
  public name: string = 'commands';

  async onChildLoad(command: Command) {
    // yes
  }

  async onChildUnload(command: Command) {
    // yes
  }

  @Subscribe(Discord, 'message')
  private async onMessageCreate(event: MessageCreateEvent) {
    // yes
  }
}
