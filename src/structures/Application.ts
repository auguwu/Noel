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

import { NotInjectable, getSubscriptions, getServicesIn, getComponentsIn } from './decorators';
import type { Component, Service } from '.';
import { Collection } from '@augu/collections';
import * as utils from '@augu/utils';
import { join } from 'path';
import Logger from './Logger';

// Components
import type Telegram from './components/Telegram';
import type Discord from './components/Discord';
import type Config from './components/Config';

// Services
import type CommandService from './services/CommandService';
import type JobService from './services/JobService';

interface ComponentCollection extends Collection<string, Component> {
  get(key: 'telegram'): Telegram;
  get(key: 'discord'): Discord;
  get(key: 'config'): Config;
  get(key: string): Component | undefined;
}

interface ServiceCollection extends Collection<string, Service> {
  get(key: 'commands'): CommandService;
  get(key: 'jobs'): JobService;
  get(key: string): Service | undefined;
}

/**
 * Represents the application for the bot, this can't be injectable using the `@Component` decorator
 */
@NotInjectable
export default class Application implements Component {
  public components: ComponentCollection;
  public services: ServiceCollection;

  private logger = Logger.get();
  public name = 'Noel';

  constructor() {
    this.components = new Collection();
    this.services = new Collection();
  }

  async load() {
    this.logger.debug('Bootstrapping Application component...');

    // Add this application for no reason :^)
    this.components.set(this.name, this);

    // Initialize all services
    await this._initServices();

    // Initialize all components
    await this._initComponents();

    // Inject all subscriptions, components, and services and load them
    for (const service of this.services.values()) {
      this._inject(service);
      await service.load?.();
    }

    for (const component of this.components.values()) {
      this._inject(component);
      await component.load?.();
    }
  }

  async dispose() {
    for (const service of this.services.values())
      service.dispose?.();

    for (const component of this.components.values())
      component.dispose?.();

    this.logger.warn('Disposed all services and components');
  }

  getService(reference: Service) {
    const ref = this.services.find(ref => ref === reference);
    if (ref === null)
      throw new TypeError(`Service ${reference.name} was not found in collection`);

    return ref!;
  }

  getComponent(reference: Component) {
    const ref = this.components.find(ref => ref === reference);
    if (ref === null)
      throw new TypeError(`Component ${reference.name} was not found in collection`);

    return ref!;
  }

  private async _initServices() {
    this.logger.debug('Now initializing services...');

    const files = await utils.readdir(join(__dirname, 'services'));
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const ctor: utils.Ctor<Service> = await import(file);
      const service = ctor.default ? new ctor.default() : new ctor();

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

      this.logger.log(`Found and initialized component ${component.name}`);
      this.components.set(component.name, component);
    }
  }

  private _inject(type: Service | Component) {
    const subscriptions = getSubscriptions(type);
    const components = getComponentsIn(type);
    const services = getServicesIn(type);

    subscriptions.forEach(subscription => {
      const discord = this.components.get('discord');
      discord.subscribe(subscription);
    });

    components.forEach(component => {
      Object.defineProperty(type, component.key, {
        get: () => this.getComponent(component.reference),
        set: () => {
          throw new SyntaxError('Cannot write new component to this property.');
        },

        enumerable: true,
        configurable: true
      });
    });

    services.forEach(s => {
      Object.defineProperty(type, s.key, {
        get: () => this.getService(s.reference),
        set: () => {
          throw new SyntaxError('Cannot write new service to this property.');
        },

        enumerable: true,
        configurable: true
      });
    });

    this.logger.debug(`[${type.name}] Populated ${subscriptions.length} ${utils.pluralize('subscription', subscriptions.length)}, ${components.length} ${utils.pluralize('component', components.length)}, and ${services.length} ${utils.pluralize('service', services.length)}`);
  }
}
