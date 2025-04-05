package io.github.parseworks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The `FList` class is a functional list implementation that extends `ArrayList`.
 * It provides additional methods for functional programming, such as `map`, `filter`, and `fold`.
 *
 * @param <T> the type of elements in this list
 * @author jason bailey
 * @version $Id: $Id
 */
public class FList<T> extends ArrayList<T> {

    /**
     * Constructs an empty `FList`.
     */
    public FList() {
        super();
    }

    /**
     * Constructs an `FList` with a head element and a tail list.
     *
     * @param head the left element of the list
     * @param tail the rest of the list
     */
    public FList(T head, FList<T> tail) {
        super();
        add(head);
        addAll(tail);
    }

    /**
     * Constructs an `FList` from an existing list.
     *
     * @param tail the list to copy elements from
     */
    public FList(List<T> tail) {
        super();
        addAll(tail);
    }

    /**
     * Adds an element to the front of the list.
     *
     * @param head the element to add
     * @return this list with the new element added
     */
    public FList<T> push(T head) {
        add(0, head);
        return this;
    }

    /**
     * Reverses the order of the elements in this list.
     *
     * @return a new `FList` with the elements in reverse order
     */
    public FList<T> reverse() {
        FList<T> reversed = new FList<>();
        for (int i = size() - 1; i >= 0; i--) {
            reversed.add(get(i));
        }
        return reversed;
    }

    /**
     * Returns a new `FList` that is a sublist of this list, starting from the right element.
     *
     * @return a new `FList` that is a sublist of this list
     */
    public FList<T> tail() {
        return new FList<>(super.subList(1, this.size()));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns a new `FList` that is a sublist of this list, from the specified index to the end index.
     */
    public FList<T> subList(int index, int endIndex) {
        return new FList<>(super.subList(index, endIndex));
    }

    /**
     * Returns the left element of the list.
     *
     * @return the left element of the list
     */
    public T head() {
        return get(0);
    }

    /**
     * Returns true if the list is empty.
     *
     * @return true if the list is empty
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the element at the specified position in this list.
     */
    public T get(int index) {
        return super.get(index);
    }

    /**
     * Returns a new `FList` consisting of the results of applying the given function to the elements of this list.
     *
     * @param mapper the function to apply to each element
     * @param <R>    a R class
     * @return a new `FList` with the mapped elements
     */
    public <R> FList<R> map(Function<T, R> mapper) {
        FList<R> mapped = new FList<>();
        for (T t : this) {
            mapped.add(mapper.apply(t));
        }
        return mapped;
    }

    /**
     * Returns a new `FList` consisting of the elements of this list that match the given predicate.
     *
     * @param predicate the predicate to apply to each element
     * @return a new `FList` with the filtered elements
     */
    public FList<T> filter(Predicate<T> predicate) {
        FList<T> filtered = new FList<>();
        for (T t : this) {
            if (predicate.test(t)) {
                filtered.add(t);
            }
        }
        return filtered;
    }

    /**
     * Folds the elements of this list from the left using the given identity value and folding function.
     *
     * @param identity the initial value
     * @param folder   the folding function
     * @param <B>      the type of the result
     * @return the result of folding the elements
     */
    public <B> B foldLeft(B identity, BiFunction<B, T, B> folder) {
        B result = identity;
        for (T t : this) {
            result = folder.apply(result, t);
        }
        return result;
    }

    /**
     * Folds the elements of this list from the left using the given initial value and folding function.
     *
     * @param initial the initial value
     * @param folder  the folding function
     * @return the result of folding the elements
     */
    public T foldLeft(T initial, BinaryOperator<T> folder) {
        T result = initial;
        for (T t : this) {
            result = folder.apply(result, t);
        }
        return result;
    }

    /**
     * Folds the elements of this list from the right using the given initial value and folding function.
     *
     * @param identity the initial value
     * @param folder   the folding function
     * @param <B>      the type of the result
     * @return the result of folding the elements
     */
    public <B> B foldRight(B identity, BiFunction<T, B, B> folder) {
        return reverse().foldLeft(identity, (b, a) -> folder.apply(a, b));
    }

    /**
     * Folds the elements of this list from the right using the given initial value and folding function.
     *
     * @param identity the initial value
     * @param folder   the folding function
     * @param <B>      the type of the result
     * @return the result of folding the elements
     */
    public <B> B foldRight1(B identity, BiFunction<T, B, B> folder) {
        return reverse().foldLeft(identity, (b, a) -> folder.apply(a, b));
    }

    /**
     * <p>of.</p>
     *
     * @param elements a T object
     * @param <T>      a T class
     * @return a {@link io.github.parseworks.FList} object
     */
    @SafeVarargs
    public static <T> FList<T> of(T... elements) {
        FList<T> list = new FList<>();
        list.addAll(Arrays.asList(elements));
        return list;
    }

    /**
     * Converts a list of characters to a string.
     *
     * @param list the list of characters
     * @return the string representation of the list
     */
    public static String toString(FList<Character> list) {
        return list.foldLeft(new StringBuilder(), StringBuilder::append).toString();
    }

}
