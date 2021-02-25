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
import type { Service } from '..';
import { MetadataKeys } from '../interfaces/MetaKeys';

interface ServiceReflectReference {
  reference: Service;
  key: string;
}

/**
 * Service decorator to inject into a property, use `@Component` to inject
 * a component into a property.
 */
const _Service: PropertyDecorator = (target: any, property) => {
  const reference = Reflect.getMetadata('design:type', target, property);

  if (reference === undefined)
    throw new TypeError('Unable to infer type for reference');

  if (!isNotInjectable(reference))
    throw new TypeError('Inferred reference is not injectable');

  const references: ServiceReflectReference[] = Reflect.getMetadata(MetadataKeys.Service, target) ?? [];
  references.push({ key: String(property), reference });

  Reflect.defineMetadata(MetadataKeys.Service, target, references);
};

/**
* Returns the referenced services in the specified [target]
*/
export function getServicesIn(target: any): ServiceReflectReference[] {
  return Reflect.getMetadata(MetadataKeys.Service, target) ?? [];
}

export default _Service;
