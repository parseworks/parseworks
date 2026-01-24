package io.github.parseworks.impl.result;

import io.github.parseworks.*;

import java.util.List;
import java.util.function.Function;

/**
 * Represents a failure result in a parser combinator.
 * <p>
 * This class provides detailed error information including:
 * <ul>
 *   <li>The input position where the error occurred</li>
 *   <li>What was expected vs. what was found</li>
 *   <li>The type of the error</li>
 *   <li>A custom error message (if provided)</li>
 *   <li>The cause of the error (for nested errors)</li>
 * </ul>
 *
 * @param <I> the type of the input symbols
 * @param <A> the type of the parsed value
 */
public record NoMatch<I, A>(
        Input<I> input,
        String expected,
        Failure<?, ?> cause,
        List<Failure<I, A>> combinedFailures
) implements Failure<I, A> {

    /**
     * Constructs a new NoMatch with no custom message.
     */
    public NoMatch(Input<I> input, String expected) {
        this(input, expected, null, null);
    }

    /**
     * Constructs a new NoMatch with a cause, inheriting the cause's error type,
     * with no custom message.
     */
    public NoMatch(Input<I> input, String expected, Failure<?, ?> cause) {
        this(input, expected, cause, null);
    }

    /**
     * Constructs a new NoMatch with a cause, inheriting the cause's error type,
     * with no custom message.
     */
    public NoMatch(List<Failure<I, A>> failures) {
        this(failures.isEmpty() ? null : failures.get(0).input(), null, null, failures);
    }



    // No explicit canonical constructor override is needed; the record-generated
    // canonical constructor is used.

    @Override
    public ResultType type() {
        return ResultType.NO_MATCH;
    }

    @Override
    public boolean matches() {
        return false;
    }

    @Override
    public A value() {
        throw new RuntimeException(error());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B> Result<I, B> cast() {
        return (Result<I, B>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B> Result<I, B> map(Function<A, B> mapper) {
        return (Result<I, B>) this;
    }

    @Override
    public <B> B handle(Function<Result<I, A>, B> success, Function<Result<I, A>, B> failure) {
        return failure.apply(this);
    }

}
