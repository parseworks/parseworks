package io.github.parseworks.impl.result;

import io.github.parseworks.Failure;
import io.github.parseworks.Input;
import io.github.parseworks.Result;
import io.github.parseworks.ResultType;

import java.util.List;
import java.util.function.Function;

/**
 * Represents a failure that occurred during repetition parsing.
 * @param <I> the type of input being parsed
 * @param <A> the type of result produced by the parser
 */
public record RepetitionFailure<I, A> (Input<I> input) implements Failure<I, A> {


    /**
     * Apply one of two functions to this value.
     *
     * @param success the function to be applied to a Match result
     * @param failure the function to be applied to a NoMatch result
     * @return the result of applying either function
     */
    @Override
    public <B> B handle(Function<Result<I, A>, B> success, Function<Result<I, A>, B> failure) {
        return null;
    }

    /**
     * Returns the underlying failure that caused this failure.
     *
     * @return the underlying failure, or null if there is no cause
     */
    @Override
    public Failure<?, ?> cause() {
        return null;
    }

    /**
     * Returns what was expected by the parser that failed.
     *
     * @return the expected input description
     */
    @Override
    public String expected() {
        return "";
    }

    /**
     * Returns a list of failures that were combined to form this failure.
     * This is used when multiple alternative parsers fail at the same position.
     *
     * @return the list of combined failures, or null if not an aggregated failure
     */
    @Override
    public List<Failure<I, A>> combinedFailures() {
        return List.of();
    }

    /**
     * Returns the type of this result.
     *
     * @return the result type
     */
    @Override
    public ResultType type() {
        return null;
    }

    /**
     * Returns true if this result is a Match.
     *
     * @return true if this result is a Match
     */
    @Override
    public boolean matches() {
        return false;
    }

    /**
     * Returns the parsed value if this result is a Match.
     * Throws an exception if this result is a NoMatch.
     *
     * @return the parsed value
     * @throws RuntimeException if this result is a NoMatch
     */
    @Override
    public A value() {
        return null;
    }

    /**
     * Returns the remaining input after parsing.
     *
     * @return the remaining input
     */
    @Override
    public Input<I> input() {
        return null;
    }

    /**
     * Casts this result to a result of a different type.
     *
     * @return this result cast to the new type
     */
    @Override
    public <B> Result<I, B> cast() {
        return null;
    }

    /**
     * Maps the parsed value to a new value using the given function.
     *
     * @param mapper the function to apply to the parsed value
     * @return a new result with the mapped value
     */
    @Override
    public <B> Result<I, B> map(Function<A, B> mapper) {
        return null;
    }
}
