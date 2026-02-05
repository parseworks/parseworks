package io.github.parseworks;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Utility functions for working with {@link List} in a functional style.
 */
public final class Lists {

    private Lists() {
    }

    /** Prepends an element to a list. Returns a new unmodifiable list. */
    public static <T> List<T> prepend(T head, List<T> tail) {
        List<T> list = new ArrayList<>(tail.size() + 1);
        list.add(head);
        list.addAll(tail);
        return Collections.unmodifiableList(list);
    }

    /** Appends an element to a list. Returns a new unmodifiable list. */
    public static <T> List<T> append(List<T> list, T element) {
        List<T> result = new ArrayList<>(list.size() + 1);
        result.addAll(list);
        result.add(element);
        return Collections.unmodifiableList(result);
    }

    /** Appends a collection of elements to a list. Returns a new unmodifiable list. */
    public static <T> List<T> appendAll(List<T> list, Collection<? extends T> collection) {
        if (collection == null || collection.isEmpty()) {
            return Collections.unmodifiableList(new ArrayList<>(list));
        }
        List<T> result = new ArrayList<>(list.size() + collection.size());
        result.addAll(list);
        result.addAll(collection);
        return Collections.unmodifiableList(result);
    }

    /**
     * Reverses the order of elements in a list.
     *
     * @param list the list to reverse
     * @param <T>  the type of elements in the list
     * @return a new unmodifiable list with elements in reverse order
     */
    private static <T> List<T> reverse(List<T> list) {
        List<T> result = new ArrayList<>(list);
        Collections.reverse(result);
        return Collections.unmodifiableList(result);
    }

    /** Transforms a list using a mapper function. Returns a new unmodifiable list. */
    public static <T, R> List<R> map(List<T> list, Function<? super T, ? extends R> mapper) {
        List<R> result = new ArrayList<>(list.size());
        for (T item : list) {
            result.add(mapper.apply(item));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Filters elements of a list based on a predicate.
     *
     * @param list      the list to filter
     * @param predicate the predicate to apply to each element
     * @param <T>       the type of elements in the list
     * @return a new unmodifiable list containing only elements that satisfy the predicate
     */
    private static <T> List<T> filter(List<T> list, Predicate<? super T> predicate) {
        List<T> result = new ArrayList<>(list.size());
        for (T item : list) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** Folds a list from left to right. */
    public static <T, B> B foldLeft(List<T> list, B identity, BiFunction<B, ? super T, B> folder) {
        B result = identity;
        for (T t : list) {
            result = folder.apply(result, t);
        }
        return result;
    }

    /** Folds a list from right to left. */
    public static <T, B> B foldRight(List<T> list, B identity, BiFunction<? super T, B, B> folder) {
        B result = identity;
        ListIterator<T> it = list.listIterator(list.size());
        while (it.hasPrevious()) {
            result = folder.apply(it.previous(), result);
        }
        return result;
    }

    /**
     * Joins the string representation of all elements in a list.
     *
     * @param list the list to join
     * @return a string containing all elements concatenated
     */
    public static String join(List<?> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object o : list) {
            sb.append(o);
        }
        return sb.toString();
    }

}
