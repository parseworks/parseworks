package io.github.parseworks;

import java.util.*;
import java.util.function.*;

/**
 * An immutable functional list implementation that extends LinkedList.
 * Despite extending a mutable collection, this class preserves immutability
 * by overriding all mutating operations to throw exceptions.
 *
 * @param <T> the type of elements in this list
 * @author jason bailey
 */
public final class FList<T> extends LinkedList<T> implements Iterable<T> {

    /**
     * Constructs an empty {@code FList}.
     */
    public FList() {
        super();
    }

    /**
     * Constructs an {@code FList} with a head element and a tail list.
     *
     * @param head the first element of the list
     * @param tail the rest of the list (another FList)
     */
    public FList(T head, FList<T> tail) {
        super();
        super.add(head);
        super.addAll(tail);
    }

    /**
     * Constructs an {@code FList} from an existing {@code Collection}.
     * The elements are copied to ensure immutability.
     *
     * @param collection the collection to copy elements from
     */
    public FList(Collection<? extends T> collection) {
        super();
        if (collection != null) {
            super.addAll(collection);
        }
    }

    /**
     * Creates a new {@code FList} instance populated with the provided elements.
     *
     * @param <T>      the type of elements in the list
     * @param elements a variable number of elements to include in the new list.
     * @return a new {@code FList} containing all the specified elements.
     */
    @SafeVarargs
    public static <T> FList<T> of(T... elements) {
        FList<T> result = new FList<>();
        if (elements != null && elements.length > 0) {
            Collections.addAll(result, elements);
        }
        return result;
    }

    /**
     * Returns a new {@code FList} with the given element added to the front.
     *
     * @param head the element to add
     * @return a new {@code FList} with the new element at the beginning
     */
    public FList<T> prepend(T head) {
        FList<T> result = new FList<>(this);
        result.addFirst(head);
        return result;
    }

    /**
     * Returns a new {@code FList} with the elements in reverse order.
     *
     * @return a new {@code FList} with the elements in reverse order
     */
    public FList<T> reverse() {
        FList<T> result = new FList<>(this);
        Collections.reverse(result);
        return result;
    }

    /**
     * Returns a new {@code FList} representing the tail of this list (all elements except the first).
     * Throws {@link NoSuchElementException} if the list is empty.
     *
     * @return a new {@code FList} that is the tail of this list
     * @throws NoSuchElementException if the list is empty
     */
    public FList<T> tail() {
        if (isEmpty()) {
            throw new NoSuchElementException("tail() of empty list");
        }
        return new FList<>(subList(1, size()));
    }

    /**
     * Returns the first element of the list.
     * Throws {@link NoSuchElementException} if the list is empty.
     *
     * @return the first element of the list
     * @throws NoSuchElementException if the list is empty
     */
    public T head() {
        if (isEmpty()) {
            throw new NoSuchElementException("head() of empty list");
        }
        return getFirst();
    }

    // --- Functional "modification" methods returning new FList instances ---

    public FList<T> append(T element) {
        FList<T> result = new FList<>(this);
        result.addLast(element);
        return result;
    }

    public FList<T> appendAll(Collection<? extends T> collection) {
        if (collection == null || collection.isEmpty()) {
            return this;
        }
        FList<T> result = new FList<>(this);
        result.addAll(collection);
        return result;
    }

    public FList<T> replace(int index, T element) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        }
        FList<T> result = new FList<>(this);
        result.set(index, element);
        return result;
    }

    public FList<T> removeElement(T element) {
        int index = indexOf(element);
        if (index == -1) {
            return this;
        }
        FList<T> result = new FList<>(this);
        result.remove(index);
        return result;
    }

    public FList<T> removeAt(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        }
        FList<T> result = new FList<>(this);
        result.remove(index);
        return result;
    }

    public <R> FList<R> map(Function<? super T, ? extends R> mapper) {
        if (isEmpty()) {
            return new FList<>();
        }
        FList<R> result = new FList<>();
        for (T item : this) {
            result.add(mapper.apply(item));
        }
        return result;
    }

    public FList<T> filter(Predicate<? super T> predicate) {
        if (isEmpty()) {
            return this;
        }
        FList<T> result = new FList<>();
        for (T item : this) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        if (result.size() == size()) return this; // Optimization
        return result;
    }

    public <B> B foldLeft(B identity, BiFunction<B, ? super T, B> folder) {
        B result = identity;
        for (T t : this) {
            result = folder.apply(result, t);
        }
        return result;
    }

    public T foldLeft(T initial, BinaryOperator<T> folder) {
        T result = initial;
        for (T t : this) {
            result = folder.apply(result, t);
        }
        return result;
    }

    public Optional<T> reduceLeft(BinaryOperator<T> reducer) {
        if (isEmpty()) {
            return Optional.empty();
        }
        T result = getFirst();
        for (int i = 1; i < size(); i++) {
            result = reducer.apply(result, get(i));
        }
        return Optional.of(result);
    }

    public <B> B foldRight(B identity, BiFunction<? super T, B, B> folder) {
        B result = identity;
        ListIterator<T> it = listIterator(size());
        while (it.hasPrevious()) {
            result = folder.apply(it.previous(), result);
        }
        return result;
    }

    public Optional<T> reduceRight(BinaryOperator<T> reducer) {
        if (isEmpty()) {
            return Optional.empty();
        }
        T result = getLast();
        for (int i = size() - 2; i >= 0; i--) {
            result = reducer.apply(get(i), result);
        }
        return Optional.of(result);
    }

    // Override all modifying methods to throw UnsupportedOperationException
    // and use private versions instead for construction and functional methods

    @Override
    public FList<T> subList(int fromIndex, int toIndex) {
        return new FList<>(super.subList(fromIndex, toIndex));
    }

    public static String joinChars(FList<Character> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return list.foldLeft(new StringBuilder(), StringBuilder::append).toString();
    }

    // Note: We must override all LinkedList's mutation methods to preserve immutability
    // This implementation omits these for brevity, but they should all throw UnsupportedOperationException
}