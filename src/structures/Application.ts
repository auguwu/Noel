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
import type { ComponentImpl as Component, Service } from '.';
import ListenerService from './services/ListenerService';
import { Collection } from '@augu/collections';
import * as utils from '@augu/utils';
import { join } from 'path';
import Discord from './components/Discord';
import Logger from './Logger';

/**
 * Represents the application for the bot, this can't be injectable using the `@Component` decorator
 */
@NotInjectable
export default class Application {
  public components: Collection<string, Component>;
  public services: Collection<string, Service>;

  private references = new Collection<any, string>();
  private logger = Logger.get();
  public priority = 3621; // fuck you
  public name = 'Noel';

  constructor() {
    this.components = new Collection();
    this.services = new Collection();
  }

  async load() {
    this.logger.debug('Bootstrapping Application component...');

    // Initialize all services
    await this._initServices();

    // Initialize all components
    await this._initComponents();

    // Inject all subscriptions, components, and services and load them
    for (const service of this.services.values()) {
      this._inject(service);
      await service.load?.();
    }

    // Sort by load priority (0 = highest, 3 = lowest)
    const components = this.components.sort((a, b) => {
      if (a.priority > b.priority) return 1;
      else if (a.priority < b.priority) return -1;
      else return 0;
    });

    for (let i = 0; i < components.length; i++) {
      this._inject(components[i]);
      components[i].load?.();
    }

    // Since the listener service needs the Discord component and it's not
    // initialized, we'll have to create it
    const listeners = new ListenerService();

    // Since the discord component needs to be launched on itself
    // We'll create a new instance of it and add subscriptions from components and services
    const discord = new Discord();

    this._inject(discord);
    discord.init();

    const services = this.services.toArray();
    const serviceAndComponents = [...services, ...components];

    for (let i = 0; i < serviceAndComponents.length; i++) {
      const subscriptions = getSubscriptions(serviceAndComponents[i]);
      subscriptions.forEach(sub => discord.subscribe(sub));
    }

    this.services.set('listeners', listeners);
    this.components.set('discord', discord);

    this._inject(listeners);
    getSubscriptions(listeners).forEach(sub => discord.subscribe(sub));
    await discord.load();
  }

  dispose() {
    for (const service of this.services.values())
      service.dispose?.();

    for (const component of this.components.values())
      component.dispose?.();

    this.logger.warn('Disposed all services and components');
  }

  $ref(reference: Component | Service) {
    const ref = this.references.get(reference);
    if (ref === undefined)
      throw new TypeError(`Component or service ${reference.name} was not found in collection`);

    if (this.services.has(ref))
      return this.services.get(ref)!;
    else if (this.components.has(ref))
      return this.components.get(ref)!;
    else
      throw new TypeError(`Reference "${ref}" was not implemented?`);
  }

  private async _initServices() {
    this.logger.debug('Now initializing services...');

    const files = (await utils.readdir(join(__dirname, 'services'))).filter(uwu => !uwu.includes('ListenerService'));
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const ctor: utils.Ctor<Service> = await import(file);
      const service = ctor.default ? new ctor.default() : new ctor();

      this.logger.log(`Found and initialized service ${service.name}`);
      this.services.set(service.name, service);
      this.references.set(service.constructor, service.name);
    }
  }

  private async _initComponents() {
    this.logger.debug('Now initializing components...');

    const files = (await utils.readdir(join(__dirname, 'components'))).filter(uwu => !uwu.includes('Discord'));
    for (let i = 0; i < files.length; i++) {
      const file = files[i];
      const ctor: utils.Ctor<Component> = await import(file);
      const component = ctor.default ? new ctor.default() : new ctor();

      this.logger.log(`Found and initialized component ${component.name}`);
      this.components.set(component.name, component);
      this.references.set(component.constructor, component.name);
    }
  }

  private _inject(type: Service | Component) {
    const components = getComponentsIn(type);
    const services = getServicesIn(type);

    components.forEach(component => {
      Object.defineProperty(type, component.key, {
        get: () => this.$ref(component.reference),
        set: () => {
          throw new SyntaxError('Cannot write new component to this property.');
        },

        enumerable: true,
        configurable: true
      });
    });

    services.forEach(s => {
      Object.defineProperty(type, s.key, {
        get: () => this.$ref(s.reference),
        set: () => {
          throw new SyntaxError('Cannot write new service to this property.');
        },

        enumerable: true,
        configurable: true
      });
    });

    this.logger.debug(`[${type.name}] Populated ${utils.pluralize('component', components.length)} and ${utils.pluralize('service', services.length)}`);
  }
}
