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

import { parse as dotenv, SchemaOptions } from '@augu/dotenv';
import type { ComponentImpl } from '..';
import { join } from 'path';

interface Configuration {
  BIRTHDAY_CHANNEL_ID: string;
  WELCOMER_CHANNEL_ID: string;
  POLLS_CHANNEL_ID: string;
  TELEGRAM_TOKEN: string;
  WAH_CHANNEL_ID: string;
  DISCORD_TOKEN: string;
  AUTOROLES: string[];
  NODE_ENV: 'development' | 'production';
  PREFIXES: string[];
  OWNERS: string[];
}

const schema: { [P in keyof Configuration]: string | SchemaOptions } = {
  BIRTHDAY_CHANNEL_ID: 'string',
  WELCOMER_CHANNEL_ID: 'string',
  POLLS_CHANNEL_ID: 'string',
  TELEGRAM_TOKEN: 'string',
  WAH_CHANNEL_ID: 'string',
  AUTOROLES: {
    type: 'array',
    default: []
  },
  OWNERS: {
    type: 'array',
    default: []
  },
  NODE_ENV: {
    type: 'string',
    default: 'development',
    oneOf: ['development', 'production']
  },
  PREFIXES: {
    type: 'array',
    default: ['noel ', 'noel!', 'n;']
  },
  DISCORD_TOKEN: {
    type: 'string',
    default: undefined
  }
};

export default class ConfigurationComponent implements ComponentImpl {
  public priority = 0;
  public name = 'config';

  #config!: Configuration;

  async load() {
    this.#config = dotenv<Configuration>({
      populate: false,
      delimiter: ',',
      schema,
      file: join(__dirname, '..', '..', '..', '.env')
    });

    process.env.NODE_ENV = this.#config.NODE_ENV;
  }

  getProperty<K extends keyof Configuration>(key: K): Configuration[K] | undefined {
    return this.#config[key];
  }
}
