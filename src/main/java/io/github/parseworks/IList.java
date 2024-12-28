package io.github.parseworks;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class IList<T> implements Iterator<T> {
    private final List<T> list;
    private int currentIndex = 0;

    private IList(List<T> list) {
        this.list = list;
    }

    public static <T> IList<T> empty() {
        return new IList<>(new ArrayList<>());
    }

    public IList<T> add(T element) {
        List<T> newList = new ArrayList<>(list);
        newList.add(element);
        return new IList<>(newList);
    }

    public IList<T> reverse() {
        List<T> newList = new ArrayList<>(list);
        java.util.Collections.reverse(newList);
        return new IList<>(newList);
    }

    public <U> U foldRight(U identity, java.util.function.BiFunction<T, U, U> accumulator) {
        U result = identity;
        for (int i = list.size() - 1; i >= 0; i--) {
            result = accumulator.apply(list.get(i), result);
        }
        return result;
    }

    public <U> U foldLeft(U identity, java.util.function.BiFunction<U, T, U> accumulator) {
        U result = identity;
        for (T element : list) {
            result = accumulator.apply(result, element);
        }
        return result;
    }

    @Override
    public boolean hasNext() {
        return currentIndex < list.size();
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return list.get(currentIndex++);
    }
}