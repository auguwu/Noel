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

import { ScheduledTask, schedule, validate } from 'node-cron';
import { Collection } from '@augu/collections';
import { Component } from '@ayanaware/bento';
import Loggaby from 'loggaby';

const logger = new Loggaby({
  format: `{grey}[CronJob / ${process.pid} / {level.name}]{grey} `
});

export default class CronJobComponent implements Component {
  private jobs: Collection<string, ScheduledTask> = new Collection();
  public name: string = 'jobs';

  async onChildLoad(job: any) {
    if (!validate(job.expression)) throw new TypeError(`Expression '${job.expression}' is invalid.`);

    const task = schedule(job.expression, async () => {
      try {
        await job.run();
      } catch(ex) {
        logger.error(`Unable to run cron job '${job.name}':\n`, ex);
      }
    });

    logger.log(`Initialized job "${job.name}"`);
    this.jobs.set(job.name, task);
  }

  async onChildUnload(job: any) {
    logger.warn(`Expected to unload job "${job.name}"`);

    const task = this.jobs.get(job.name);
    if (!task) throw new TypeError(`Job "${job.name}" doesn't exist, was it unloaded already?`);

    task.destroy();
    this.jobs.delete(job.name);

    logger.warn(`Job "${job.name}" has been unloaded successfully.`);
  }
}
