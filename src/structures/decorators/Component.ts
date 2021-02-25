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
import type { Component } from '..';
import { MetadataKeys } from '../interfaces/MetaKeys';

interface ComponentReflectReference {
  reference: Component;
  key: string;
}

/**
 * Component decorator to inject into a property, use `@Service` to inject
 * a service into a property.
 */
const _Component: PropertyDecorator = (target: any, property) => {
  const reference = Reflect.getMetadata('design:type', target, property);

  if (reference === undefined)
    throw new TypeError('Unable to infer type for reference');

  if (!isNotInjectable(reference))
    throw new TypeError('Inferred reference is not injectable');

  const references: ComponentReflectReference[] = Reflect.getMetadata(MetadataKeys.Component, target) ?? [];
  references.push({ key: String(property), reference });

  Reflect.defineMetadata(MetadataKeys.Component, target, references);
};

/**
 * Returns the referenced components in the specified [target]
 */
export function getComponentsIn(target: any): ComponentReflectReference[] {
  return Reflect.getMetadata(MetadataKeys.Component, target) ?? [];
}

export default _Component;
