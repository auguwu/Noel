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

import { isNotInjectable } from './NotInjectable';
import { MetadataKeys } from '../interfaces/MetaKeys';

interface InjectReference {
  reference: any;
  key: string;
}

const _Injectable: ClassDecorator = (target) => {
  if (isNotInjectable(target))
    throw new TypeError('target class is marked not injectable');

  Reflect.defineMetadata(MetadataKeys.Injectable, true, target);
};

export const Inject: PropertyDecorator = (target: any, prop) => {
  const reference = Reflect.getMetadata('design:type', target, prop);

  if (reference === undefined)
    throw new TypeError('Unable to infer type for reference');

  if (isNotInjectable(reference))
    throw new TypeError('Inferred reference is not injectable');

  const $refs: InjectReference[] = Reflect.getMetadata(MetadataKeys.Injections, target) ?? [];
  $refs.push({ reference, key: String(prop) });

  Reflect.defineMetadata(MetadataKeys.Injections, $refs, target);
};

export const isInjectable = (target: any): boolean => Reflect.getMetadata(MetadataKeys.Injectable, target) ?? false;
export const getInjections = (target: any): InjectReference[] => {
  const isInject = Reflect.getMetadata(MetadataKeys.Injectable, target) ?? false;
  if (!isInject) return [];

  return Reflect.getMetadata(MetadataKeys.Injections, target) ?? [];
};

export default _Injectable;
