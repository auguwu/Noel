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
import { Component, Inject } from '@ayanaware/bento';
import { Ctor, readdir } from '@augu/utils';
import { Collection } from '@augu/collections';
import type CronJob from '../Job';
import { join } from 'path';
import Logger from '@ayanaware/logger';
import Noel from '../Noel';

const logger = Logger.get();

export default class JobManager implements Component {
  private jobs: Collection<string, ScheduledTask> = new Collection();
  public name = 'JobManager';

  @Inject(Noel)
  private bot!: Noel;

  async onLoad() {
    const files = await readdir(join(__dirname, '..', '..', 'jobs'));
    logger.info(`Found ${files.length} jobs to initialize`);

    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const ctor: Ctor<CronJob> = await import(file);
      const job = ctor.default ? new ctor.default() : new ctor();

      job.init(this.bot);
      logger.info(`Found & initialized job "${job.name}"`);

      schedule(job.expression, async() => {
        try {
          await job.run();
        } catch(ex) {
          logger.error(ex);
        }
      }, { timezone: 'America/Phoenix' });
    }
  }

  async onUnload() {
    logger.warn('Unloading jobs...');
    this.jobs.forEach(job => job.destroy());
    this.jobs.clear();

    logger.info('Jobs has been cleared.');
  }
}
