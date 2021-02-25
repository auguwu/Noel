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

import { parse, SchemaOptions } from '@augu/dotenv';
import { Component } from '@ayanaware/bento';
import { join } from 'path';

interface Configuration {
  BIRTHDAY_CHANNEL_ID: string;
  POLLS_CHANNEL_ID: string;
  TELEGRAM_TOKEN: string;
  WAH_CHANNEL_ID: string;
  DISCORD_TOKEN: string;
  PREFIX: string;
  OWNERS: string[];
}

const schema: { [P in keyof Configuration]: string | SchemaOptions } = {
  BIRTHDAY_CHANNEL_ID: 'string',
  POLLS_CHANNEL_ID: 'string',
  TELEGRAM_TOKEN: 'string',
  WAH_CHANNEL_ID: 'string',
  OWNERS: {
    type: 'array',
    default: []
  },
  PREFIX: {
    type: 'string',
    default: 'noel '
  },
  DISCORD_TOKEN: {
    type: 'string',
    default: undefined
  }
};

export default class ConfigurationComponent implements Component {
  private config!: Configuration;
  public name: string = 'Configuration';

  async onLoad() {
    this.config = parse<Configuration>({
      file: join(__dirname, '..', '..', '..', '.env'),
      populate: false,
      delimiter: ',',
      schema
    });
  }

  getProperty<Key extends keyof Configuration>(key: Key): Configuration[Key] {
    if (!this.config.hasOwnProperty(key)) throw new TypeError(`Key '${key}' was not found in configuration`);

    return this.config[key];
  }
}
