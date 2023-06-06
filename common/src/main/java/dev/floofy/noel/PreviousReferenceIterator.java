/*
 * 🐾✨ Noel: Discord bot made to manage my servers, made in Java.
 * Copyright 2021-2023 Noel <cutie@floofy.dev>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.floofy.noel;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class PreviousReferenceIterator<T> implements Iterator<Tuple<T>> {
    private final List<T> items;
    private Tuple<T> currentRef;
    private int index = 0;

    public PreviousReferenceIterator(List<T> items) {
        this.items = Objects.requireNonNull(items);
    }

    @Override
    public boolean hasNext() {
        return items.isEmpty() || index > items.size();
    }

    @Override
    public Tuple<T> next() {
        if (items.isEmpty()) {
            return null;
        }

        if (index > items.size()) {
            return null;
        }

        if (index == 0) {
            currentRef = new Tuple<>(null, items.get(index));
        } else {
            currentRef = new Tuple<>(currentRef.second(), items.get(index));
        }

        index += 1;
        return currentRef;
    }
}
