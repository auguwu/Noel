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

import { Bento, FSComponentLoader } from '@ayanaware/bento';
import Loggaby from 'loggaby';
import { join } from 'path';
import { version as wumpVer } from 'wumpcord';
import DiscordComponent from './structures/components/DiscordComponent';
import Noel from './structures/Noel';

const { version } = require('../package.json');
const bento = new Bento();
const logger = new Loggaby({
  format: `{cyan}[main ~ ${process.pid} ~ {level.color}{level.name}{cyan}]{cyan} `
});

async function main() {
  logger.log('Initializing Noel...');
  logger.log(`Bento: ${bento.version} | Wumpcord: ${wumpVer} | Bot: ${version}`);

  const bot = new Noel();
  const fsloader = new FSComponentLoader();

  await fsloader.addDirectory(join(__dirname, 'structures', 'components'));
  await bento.addPlugins([fsloader, bot]);
  await bento.verify();

  process.on('SIGINT', () => {
    logger.warn('Received `SIGINT` call, disposing');

    const discord = bot.api.getComponent(DiscordComponent);
    discord.disconnect();

    process.exit(0);
  });
}

main();
