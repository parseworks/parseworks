package io.github.parseworks.impl;

/**
 * The `Pair` class represents a tuple of two elements.
 *
 * @param <L> the type of the left element
 * @param <R> the type of the right element
 */
public record Pair<L, R>(L left, R right) {
}