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
import Config from './Config';
import Logger from '../Logger';

type InferContext<T> = T extends MiddlewareFn<infer P> ? P : never;
type MatchedContext = InferContext<Parameters<Composer<Context>['on']>[1]>;

export default class TelegramComponent implements ComponentImpl {
  public priority = 3;
  private logger = Logger.get();
  public name = 'telegram';

  @Component private config!: Config;

  #telegram!: Telegraf;

  async load() {
    this.logger.log('Creating new Telegram instance');
    const token = this.config.getProperty('TELEGRAM_TOKEN');
    if (token === undefined) {
      this.logger.warn('Missing `TELEGRAM_TOKEN` env variable, this is optional though!');
      return Promise.resolve();
    }

    if (!this.#telegram) {
      this.#telegram = new Telegraf(token);
      this.#telegram.on('new_chat_members', this.onNewChatMembers.bind(this));
      this.#telegram.on('message', this.onMessage.bind(this));

      await this.#telegram.launch();
    }
  }

  dispose() {
    this.#telegram.stop('noel:disposed');
  }

  private onMessage(context: MatchedContext) {
    console.trace('message', context);
  }

  private onNewChatMembers(context: MatchedContext) {
    console.trace('new member(s)', context);
  }
}
