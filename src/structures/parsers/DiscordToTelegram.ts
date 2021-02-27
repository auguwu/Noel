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

import type { APIEmbed } from 'discord-api-types';
import * as utils from '@augu/utils';

export default class DiscordToTelegramParser {
  constructor() {
    throw new SyntaxError('Refrain from using the `new` keyword in [DiscordToTelegramParser], use the static methods');
  }

  static parse(text: string | APIEmbed) {
    if (typeof text === 'string')
      return text
        .replaceAll('#', '\\#') // can't use #
        .replaceAll('.', '\\.') // can't use .
        .replaceAll(/<a?/gi, '') // make the emoji is just ":name:"
        .replaceAll(/([0-9]+)>/gi, '')
        .replaceAll('<', '\\<') // can't use <
        .replaceAll('>', '\\>') // can't use >
        .replaceAll('|', '\\|'); // can't use |

    let parsedText = '';
    if (text.author !== undefined && text.author.name !== undefined)
      parsedText += `> **${text.author.name}**\n`;

    if (text.title !== undefined && text.title !== '')
      parsedText += `> **${text.title}**\n`;

    if (text.description !== undefined && text.description.length < 2048)
      parsedText += `\n**${text.description}**\n`;

    if (text.image !== undefined && text.image.url !== undefined)
      parsedText += `\n${text.image.url}\n`;

    if (text.fields !== undefined) {
      for (let i = 0; i < text.fields.length; i++) {
        const field = text.fields[i];
        parsedText += `- **${field.name}**: ${field.value}\n`;
      }
    }

    if (text.footer !== undefined) {
      parsedText += `**${text.footer.text}**`;
      if (text.timestamp !== undefined) {
        const date = new Date(text.timestamp);
        parsedText += `| ${utils.omitZero(date.getDate())}/${utils.omitZero(date.getMonth() + 1)}/${date.getFullYear()} at ${utils.omitZero(date.getHours())}:${utils.omitZero(date.getMinutes())}:${utils.omitZero(date.getSeconds())}`;
      }
    }

    return parsedText;
  }
}
