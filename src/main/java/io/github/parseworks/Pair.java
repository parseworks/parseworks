package io.github.parseworks;

/**
 * The `Pair` class represents a tuple of two elements.
 *
 * @param <F> the type of the first element
 * @param <S> the type of the second element
 */
public record Pair<F, S> (F first, S second) {
}