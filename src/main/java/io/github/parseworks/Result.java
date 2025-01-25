package io.github.parseworks;

import io.github.parseworks.impl.Failure;
import io.github.parseworks.impl.Success;

/**
 * The `Result` class represents the outcome of applying a parser to an input.
 * It can either be a success, containing the parsed value and the remaining input,
 * or a failure, containing an error message and the input at which the failure occurred.
 *
 * @param <I> the type of the input symbols
 * @param <A> the type of the parsed value
 */
public abstract class Result<I, A> {

    /**
     * Creates a successful result with the given value and remaining input.
     *
     * @param next  the remaining input
     * @param value the parsed value
     * @param <I>   the type of the input symbols
     * @param <A>   the type of the parsed value
     * @return a successful result
     */
    public static <I, A> Result<I, A> success(Input<I> next, A value) {
        return new Success<>(value, next);
    }

    /**
     * Creates a failure result with the given input and error message.
     *
     * @param input   the input at which the failure occurred
     * @param message the error message
     * @param <I>     the type of the input symbols
     * @param <A>     the type of the parsed value
     * @return a failure result
     */
    public static <I, A> Result<I, A> failure(Input<I> input, String message) {
        return new Failure<>(input, message);
    }

    /**
     * Creates a failure result due to an unexpected end of input.
     *
     * @param input   the input at which the failure occurred
     * @param message the error message
     * @param <I>     the type of the input symbols
     * @param <A>     the type of the parsed value
     * @return a failure result
     */
    public static <I, A> Result<I, A> failureEof(Input<I> input, String message) {
        return new Failure<>(input, message);
    }

    /**
     * Returns true if this result is a success.
     *
     * @return true if this result is a success
     */
    public abstract boolean isSuccess();

    /**
     * Returns true if this result is an error.
     *
     * @return true if this result is an error
     */
    public abstract boolean isError();

    /**
     * Returns the parsed value if this result is a success.
     * Throws an exception if this result is a failure.
     *
     * @return the parsed value
     * @throws RuntimeException if this result is a failure
     */
    public abstract A getOrThrow();

    /**
     * Returns the remaining input after parsing.
     *
     * @return the remaining input
     */
    public abstract Input<I> next();

    /**
     * Casts this result to a result of a different type.
     *
     * @param <B> the new type of the parsed value
     * @return this result cast to the new type
     */
    public abstract <B> Result<I, B> cast();

    /**
     * Maps the parsed value to a new value using the given function.
     *
     * @param mapper the function to apply to the parsed value
     * @param <B>    the new type of the parsed value
     * @return a new result with the mapped value
     */
    public abstract <B> Result<I, B> map(java.util.function.Function<A, B> mapper);

    /**
     * Returns the error message if this result is a failure.
     *
     * @return the error message, or an empty string if this result is a success
     */
    public abstract String getError();
}