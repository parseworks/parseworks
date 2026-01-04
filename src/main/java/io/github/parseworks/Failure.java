package io.github.parseworks;

import java.util.List;

/**
 * Represents a failure result in a parser combinator.
 * Both NoMatch and PartialMatch are types of Failure.
 *
 * @param <I> the type of the input symbols
 * @param <A> the type of the parsed value
 */
public interface Failure<I, A> extends Result<I, A> {
    /**
     * Returns the underlying failure that caused this failure.
     *
     * @return the underlying failure, or null if there is no cause
     */
    Failure<?, ?> cause();

    /**
     * Returns what was expected by the parser that failed.
     *
     * @return the expected input description
     */
    String expected();

    /**
     * Returns a list of failures that were combined to form this failure.
     * This is used when multiple alternative parsers fail at the same position.
     *
     * @return the list of combined failures, or null if not an aggregated failure
     */
    List<Failure<I, A>> combinedFailures();

    /**
     * Returns a human-friendly error message for this failure.
     *
     * @return the error message
     */
    String error();

    /**
     * Returns a human-friendly error message for this failure with indentation based on depth.
     *
     * @param depth the depth of the failure in the chain
     * @return the indented error message
     */
    String error(int depth);
}
