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

import { schedule, ScheduledTask } from 'node-cron';
import { NotInjectable } from '../decorators';
import { Ctor, readdir } from '@augu/utils';
import { Logger } from '..';
import { Queue } from '@augu/collections';
import { join } from 'path';
import Job from '../jobs/Job';

interface IJob extends Pick<Job, 'name' | 'expression'> {
  task: ScheduledTask;
}

@NotInjectable
export default class JobService {
  private jobs: Queue<IJob> = new Queue();
  private logger = Logger.get();

  async load() {
    this.logger.log('Loading jobs...');

    const files = await readdir(join(__dirname, '..', '..', 'jobs'));
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const ctor: Ctor<Job> = await import(file);
      const job = ctor.default ? new ctor.default() : new ctor();

      const scheduler = schedule(job.expression, async() => {
        try {
          await job.run();
        } catch(ex) {
          this.logger.error(`Unable to run job ${job.name}:`, ex);
        }
      }); // because yes uwu

      scheduler.start();

      this.logger.log(`Initialized job ${job.name}!`);
      this.jobs.push({
        expression: job.expression,
        task: scheduler,
        name: job.name
      });
    }
  }

  dispose() {
    // @ts-ignore i am bad yes
    this.jobs.items.forEach(job => job.task.destroy());
  }
}
