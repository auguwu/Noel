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

import type { Message, TextChannel } from 'wumpcord';
import type Noel from './Noel';

export enum CommandCategory {
  Cutie = 'Cutie',
  Staff = 'Staff',
  Utils = 'Utilities',
  Core = 'Core'
}

interface CommandInfo {
  description: string;
  cooldown?: number;
  category: CommandCategory;
  aliases?: string[];
  usage?: string;
  name: string;
}

export default abstract class Command {
  public description: string;
  public cooldown: number;
  public category: CommandCategory;
  public aliases: string[];
  public bot!: Noel;
  public usage: string;
  public name: string;

  constructor(info: CommandInfo) {
    this.description = info.description;
    this.cooldown = info.cooldown ?? 5;
    this.category = info.category;
    this.aliases = info.aliases ?? [];
    this.usage = info.usage ?? '';
    this.name = info.name;
  }

  init(noel: Noel) {
    this.bot = noel;
    return this;
  }

  abstract run(msg: Message<TextChannel>, args: string[]): Promise<any>;
}
