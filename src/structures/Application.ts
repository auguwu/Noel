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

import type { Component, Service } from '.';
import { NotInjectable } from './decorators';
import { Collection } from '@augu/collections';
import * as utils from '@augu/utils';
import { join } from 'path';
import Logger from './Logger';

/**
 * Represents the application for the bot, this can't be injectable using the `@Component` decorator
 */
@NotInjectable
export default class Application implements Component {
  public components: Collection<string, Component>;
  public services: Collection<string, Service>;

  private static instance: Application;
  private logger = Logger.get();
  public name = 'Noel';

  constructor() {
    this.components = new Collection();
    this.services = new Collection();

    if (!Application.instance)
      Application.instance = this;
  }

  static get() {
    return this.instance;
  }

  async load() {
    this.logger.debug('Bootstrapping Application component...');

    // Initialize all services
    await this._initServices();

    // Initialize all components
    await this._initComponents();
  }

  async dispose() {
    for (const service of this.services.values())
      service.dispose?.();

    for (const component of this.components.values())
      component.dispose?.();

    this.logger.warn('Disposed all services and components');
  }

  private async _initServices() {
    this.logger.debug('Now initializing services...');

    const files = await utils.readdir(join(__dirname, 'services'));
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const ctor: utils.Ctor<Service> = await import(file);
      const service = ctor.default ? new ctor.default() : new ctor();

      await service.load?.();

      this.logger.log(`Found and initialized service ${service.name}`);
      this.services.set(service.name, service);
    }
  }

  private async _initComponents() {
    this.logger.debug('Now initializing components...');

    const files = await utils.readdir(join(__dirname, 'components'));
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const ctor: utils.Ctor<Component> = await import(file);
      const component = ctor.default ? new ctor.default() : new ctor();

      await component.load?.();
      this.logger.log(`Found and initialized component ${component.name}`);
      this.components.set(component.name, component);
    }
  }
}
