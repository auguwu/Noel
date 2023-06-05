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
