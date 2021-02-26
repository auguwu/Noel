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

import type { WebSocketClientEvents } from 'wumpcord/build/gateway/WebSocketClient';
import type Subscription from '../interfaces/Subscription';
import { MetadataKeys } from '../interfaces/MetaKeys';

export function getSubscriptions(target: any): Subscription[] {
  return Reflect.getMetadata(MetadataKeys.Subscription, target) ?? [];
}

/**
 * Decorator to mark a method as a subscription, currently
 * this is supported for the [Discord] component but might be supported
 * in more components soon™️
 *
 * @param event The event to listen for
 */
export default function Subscribe(event: keyof WebSocketClientEvents): MethodDecorator {
  return (target: any, _, descriptor: TypedPropertyDescriptor<any>) => {
    const subscriptions: Subscription[] = Reflect.getMetadata(MetadataKeys.Subscription, target) ?? [];
    subscriptions.push({
      run: descriptor.value!,
      event
    });

    Reflect.defineMetadata(MetadataKeys.Subscription, subscriptions, target);
  };
}
