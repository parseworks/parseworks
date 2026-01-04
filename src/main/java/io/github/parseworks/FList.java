package io.github.parseworks;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * An immutable functional list implementation.
 * This class preserves immutability by wrapping a List and throwing
 * UnsupportedOperationException for all mutating operations.
 *
 * @param <T> the type of elements in this list
 * @author jason bailey
 */
public final class FList<T> extends AbstractList<T> implements Iterable<T> {

    private final List<T> delegate;

    /**
     * Constructs an empty {@code FList}.
     */
    public FList() {
        this.delegate = Collections.emptyList();
    }

    /**
     * Internal constructor that takes a list directly.
     * It is assumed the list passed here is either already immutable or will not be modified.
     */
    private FList(List<T> list) {
        this.delegate = Collections.unmodifiableList(list);
    }

    /**
     * Constructs an {@code FList} with a head element and a tail list.
     *
     * @param head the first element of the list
     * @param tail the rest of the list (another FList)
     */
    public FList(T head, FList<T> tail) {
        List<T> list = new ArrayList<>(tail.size() + 1);
        list.add(head);
        list.addAll(tail);
        this.delegate = Collections.unmodifiableList(list);
    }

    /**
     * Constructs an {@code FList} from an existing {@code Collection}.
     * The elements are copied to ensure immutability.
     *
     * @param collection the collection to copy elements from
     */
    public FList(Collection<? extends T> collection) {
        if (collection == null || collection.isEmpty()) {
            this.delegate = Collections.emptyList();
        } else {
            this.delegate = Collections.unmodifiableList(new ArrayList<>(collection));
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
        if (elements == null || elements.length == 0) {
            return new FList<>();
        }
        return new FList<>(Arrays.asList(elements));
    }

    @Override
    public T get(int index) {
        return delegate.get(index);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    /**
     * Returns a new {@code FList} with the given element added to the front.
     *
     * @param head the element to add
     * @return a new {@code FList} with the new element at the beginning
     */
    public FList<T> prepend(T head) {
        List<T> list = new ArrayList<>(size() + 1);
        list.add(head);
        list.addAll(delegate);
        return new FList<>(list);
    }

    /**
     * Returns a new {@code FList} with the elements in reverse order.
     *
     * @return a new {@code FList} with the elements in reverse order
     */
    public FList<T> reverse() {
        List<T> list = new ArrayList<>(delegate);
        Collections.reverse(list);
        return new FList<>(list);
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
        return new FList<>(delegate.subList(1, size()));
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
        return delegate.get(0);
    }

    // --- Functional "modification" methods returning new FList instances ---

    public FList<T> append(T element) {
        List<T> list = new ArrayList<>(size() + 1);
        list.addAll(delegate);
        list.add(element);
        return new FList<>(list);
    }

    public FList<T> appendAll(Collection<? extends T> collection) {
        if (collection == null || collection.isEmpty()) {
            return this;
        }
        List<T> list = new ArrayList<>(size() + collection.size());
        list.addAll(delegate);
        list.addAll(collection);
        return new FList<>(list);
    }

    public FList<T> replace(int index, T element) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        }
        List<T> list = new ArrayList<>(delegate);
        list.set(index, element);
        return new FList<>(list);
    }

    public FList<T> removeElement(T element) {
        int index = indexOf(element);
        if (index == -1) {
            return this;
        }
        List<T> list = new ArrayList<>(delegate);
        list.remove(index);
        return new FList<>(list);
    }

    public FList<T> removeAt(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());
        }
        List<T> list = new ArrayList<>(delegate);
        list.remove(index);
        return new FList<>(list);
    }

    public <R> FList<R> map(Function<? super T, ? extends R> mapper) {
        if (isEmpty()) {
            return new FList<>();
        }
        List<R> list = new ArrayList<>(size());
        for (T item : this) {
            list.add(mapper.apply(item));
        }
        return new FList<>(list);
    }

    public FList<T> filter(Predicate<? super T> predicate) {
        if (isEmpty()) {
            return this;
        }
        List<T> list = new ArrayList<>(size());
        for (T item : this) {
            if (predicate.test(item)) {
                list.add(item);
            }
        }
        if (list.size() == size()) return this; // Optimization
        return new FList<>(list);
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
        T result = delegate.get(0);
        for (int i = 1; i < size(); i++) {
            result = reducer.apply(result, delegate.get(i));
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
        T result = delegate.get(size() - 1);
        for (int i = size() - 2; i >= 0; i--) {
            result = reducer.apply(delegate.get(i), result);
        }
        return Optional.of(result);
    }

    @Override
    public FList<T> subList(int fromIndex, int toIndex) {
        return new FList<>(delegate.subList(fromIndex, toIndex));
    }

    public static String joinChars(FList<Character> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return list.foldLeft(new StringBuilder(), StringBuilder::append).toString();
    }

    public static String joinStrings(FList<?> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return list.foldLeft(new StringBuilder(), StringBuilder::append).toString();
    }
}