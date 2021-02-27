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

import type { ArgumentMeta } from './arguments/Argument';
import type CommandMessage from './CommandMessage';

/** definition of a subcommand */
export interface SubcommandDefinition {
  /** description of the subcommand */
  description: string;

  /** metadata of arguments */
  args?: ArgumentMeta[];

  /** the name of the subcommand */
  name: string;

  /** the subcommand's execution function */
  run: SubcommandRunner;
}

type SubcommandRunner = <T extends object = {}>(msg: CommandMessage, args?: T) => Promise<any>;

export default class Subcommand {
  public description: string;
  public name: string;
  #runner: SubcommandRunner;

  constructor(def: SubcommandDefinition) {
    this.description = def.description;
    this.#runner = def.run;
    this.name = def.description;
  }

  async run<T extends object = {}>(msg: CommandMessage, args?: T) {
    return this.#runner<T>(msg, args);
  }
}
