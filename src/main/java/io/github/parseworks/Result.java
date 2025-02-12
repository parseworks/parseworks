package io.github.parseworks;

import io.github.parseworks.impl.Failure;
import io.github.parseworks.impl.Success;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The `Result` interface represents the outcome of applying a parser to an input.
 * It can either be a success, containing the parsed value and the remaining input,
 * or a failure, containing an error message and the input at which the failure occurred.
 *
 * @param <I> the type of the input symbols
 * @param <A> the type of the parsed value
 * @author jason bailey
 * @version $Id: $Id
 */
public interface Result<I, A> {

    /**
     * Creates a successful result with the given value and remaining input.
     *
     * @param next  the remaining input
     * @param value the parsed value
     * @param <I>   the type of the input symbols
     * @param <A>   the type of the parsed value
     * @return a successful result
     */
    static <I, A> Result<I, A> success(Input<I> next, A value) {
        return new Success<>(value, next);
    }

    /**
     * Creates a failure result with the given input and error message.
     * This method is used to indicate that the parser has failed at a specific point in the input.
     *
     * @param input   the input at which the failure occurred
     * @param message the error message describing the failure
     * @param <I>     the type of the input symbols
     * @param <A>     the type of the parsed value
     * @return a failure result containing the input and error message
     */
    static <I, A> Result<I, A> failure(Input<I> input, String message) {
        return new Failure<>(input, message);
    }

    /**
     * Creates a failure result with the given input, error message, and cause.
     * This method is used to indicate that the parser has failed at a specific point in the input,
     * and provides additional context about the cause of the failure.
     *
     * @param input   the input at which the failure occurred
     * @param message the error message describing the failure
     * @param cause   the underlying cause of the failure
     * @param <I>     the type of the input symbols
     * @param <A>     the type of the parsed value
     * @return a failure result containing the input, error message, and cause
     */
    static <I, A> Result<I, A> failure(Input<I> input, String message, Result<I, ?> cause) {
        return new Failure<>(input, message, cause);
    }

    /**
     * Creates a failure result due to an unexpected end of input.
     *
     * @param input   the input at which the failure occurred
     * @param expectedType the error message
     * @param <I>     the type of the input symbols
     * @param <A>     the type of the parsed value
     * @return a failure result
     */
    static <I, A> Result<I, A> failureEof(Input<I> input, String expectedType) {
        String message = "Failure at position %s, saw <eof>, expected %s".formatted(input.position(), expectedType);
        return new Failure<>(input, message);
    }

    /**
     * Returns true if this result is a success.
     *
     * @return true if this result is a success
     */
    boolean isSuccess();

    /**
     * Returns true if this result is an error.
     *
     * @return true if this result is an error
     */
     boolean isError();

    /**
     * Returns the parsed value if this result is a success.
     * Throws an exception if this result is a failure.
     *
     * @return the parsed value
     * @throws java.lang.RuntimeException if this result is a failure
     */
     A get();

    /**
     * Returns the remaining input after parsing.
     *
     * @return the remaining input
     */
     Input<I> next();

    /**
     * Casts this result to a result of a different type.
     *
     * @param <B> the new type of the parsed value
     * @return this result cast to the new type
     */
     <B> Result<I, B> cast();

    /**
     * Maps the parsed value to a new value using the given function.
     *
     * @param mapper the function to apply to the parsed value
     * @param <B>    the new type of the parsed value
     * @return a new result with the mapped value
     */
     <B> Result<I, B> map(java.util.function.Function<A, B> mapper);

    /**
     * Returns the error message if this result is a failure.
     *
     * @return the error message, or an empty string if this result is a success
     */
     String error();

    /**
     * Returns the full error message if this result is a failure.
     *
     * @return the full error message, or an empty string if this result is a success
     */
     String fullErrorMessage();

    /**
     * Returns the input at which the failure occurred.
     *
     * @return a {@link io.github.parseworks.Result} object
     */
     Result<?, ?> cause();

    /**
     * Returns an Optional containing the parsed value if this result is a success.
     * If this result is a failure, returns an empty Optional.
     *
     * @return an Optional containing the parsed value, or an empty Optional if this result is a failure
     */
    default Optional<A> toOptional() {
        return isSuccess() ? Optional.of(get()) : Optional.empty();
    }

    /**
     * Returns an Optional containing the error message if this result is a failure.
     * If this result is a success, returns an empty Optional.
     *
     * @return an Optional containing the error message, or an empty Optional if this result is a success
     */
    default Optional<String> errorOptional() {
        return isError() ? Optional.of(error()) : Optional.empty();
    }

    /**
     * Apply one of two functions to this value.
     * @param success   the function to be applied to a successful value
     * @param failure   the function to be applied to a failure value
     * @param <B>       the function return type
     * @return          the result of applying either function
     */
    <B> B handle(Function<Success<I, A>, B> success, Function<Failure<I, A>, B> failure);
}
