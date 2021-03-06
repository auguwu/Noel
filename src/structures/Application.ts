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

import { NotInjectable, getSubscriptions, getComponentsIn, getInjections, isInjectable } from './decorators';
import type { ComponentImpl as Component } from '.';
import ListenerService from './services/ListenerService';
import { Collection } from '@augu/collections';
import * as utils from '@augu/utils';
import { join } from 'path';
import Discord from './components/Discord';
import Logger from './Logger';
import JobService from './services/JobService';
import CommandService from './services/CommandService';

/**
 * Represents the application for the bot, this can't be injectable using the `@Component` decorator
 */
@NotInjectable
export default class Application {
  public injectables: Collection<string, any>;
  public components: Collection<string, Component>;
  public listeners: ListenerService;
  public commands: CommandService;
  public jobs: JobService;

  private references = new Collection<any, string>();
  private logger = Logger.get();
  public priority = 3621; // fuck you
  public name = 'Noel';

  constructor() {
    this.injectables = new Collection();
    this.components = new Collection();
    this.listeners = new ListenerService();
    this.commands = new CommandService();
    this.jobs = new JobService();
  }

  async load() {
    this.logger.debug('Bootstrapping Application component...');

    // Initialize all components
    await this._initComponents();

    // Sort by load priority (0 = highest, 3 = lowest)
    const components = this.components.sort((a, b) => {
      if (a.priority > b.priority) return 1;
      else if (a.priority < b.priority) return -1;
      else return 0;
    });

    this._inject(components);
    for (let i = 0; i < components.length; i++)
      await components[i].load?.();

    await this.jobs.load();
    await this.commands.load();

    const refs: any[] = [
      isInjectable(this.commands) ? this.commands : null,
      isInjectable(this.listeners) ? this.listeners : null,
      isInjectable(this.jobs) ? this.jobs : null
    ].filter(value => value !== null);

    for (let i = 0; i < refs.length; i++) {
      this.injectables.set(refs[i].name, refs[i]);
      this.references.set(refs[i], refs[i].name);
    }

    this._inject([
      this.listeners,
      this.commands,
      this.jobs
    ]);
  }

  dispose() {
    for (const component of this.components.values())
      component.dispose?.();

    this.logger.warn('Disposed all services and components');
  }

  $ref(reference: Component) {
    const ref = this.references.get(reference);
    if (ref === undefined)
      throw new TypeError(`Component or injectable ${reference.name} was not found in collection`);

    if (this.components.has(ref))
      return this.components.get(ref)!;
    else if (this.injectables.has(ref))
      return this.injectables.get(ref)!;
    else
      throw new TypeError(`Reference "${ref}" was not implemented?`);
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
      this.references.set(component.constructor, component.name);
    }
  }

  private _inject(types: any[]) {
    for (let i = 0; i < types.length; i++) {
      const type = types[i];

      const subscriptions = getSubscriptions(type);
      const components = getComponentsIn(type);
      const injectables = getInjections(type);

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

      injectables.forEach(inject => {
        Object.defineProperty(type, inject.key, {
          get: () => this.$ref(inject.reference),
          set: () => {
            throw new TypeError('Cannot write new injectable to this property.');
          },

          enumerable: true,
          configurable: true
        });
      });

      const discord = this.components.get('discord') as Discord;
      subscriptions.forEach(sub => {
        discord.subscribe({
          event: sub.event,
          run: sub.run.bind(type)
        });
      });
    }
  }
}
