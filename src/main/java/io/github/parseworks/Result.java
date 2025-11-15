package io.github.parseworks;

import io.github.parseworks.impl.result.NoMatch;
import io.github.parseworks.impl.result.Match;

import java.util.List;
import java.util.Optional;
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
        return new Match<>(value, next);
    }

    /**
     * Creates a failure result that wraps another failure as its cause, preserving the chain.
     * Uses the error type from the cause.
     *
     * @param input    the input at which the failure occurred
     * @param expected the expected input
     * @param cause    the underlying failure to chain as the cause
     * @param <I>      the type of the input symbols
     * @param <A>      the type of the parsed value
     * @return a failure result that chains the given cause
     */
    static <I, A> Result<I, A> failure(Input<I> input, String expected, NoMatch<?, ?> cause) {
        return new NoMatch<>(input, expected, cause);
    }

    /**
     * Creates a failure result with the given expected value.
     * Uses the GENERIC error type by default.
     *
     * @param input    the input at which the failure occurred
     * @param expected the expected input
     * @param <I>      the type of the input symbols
     * @param <A>      the type of the parsed value
     * @return a failure result
     */

    static <I, A> Result<I, A> failure(Input<I> input, String expected) {
        return new NoMatch<>(input, expected);
    }


    /**
     * Creates an unexpected EOF error failure result.
     * Use this when the input ended prematurely.
     *
     * @param input    the input at which the failure occurred
     * @param expected what was expected before EOF
     * @param <I>      the type of the input symbols
     * @param <A>      the type of the parsed value
     * @return a failure result with UNEXPECTED_EOF error type
     */
    static <I, A> Result<I, A> unexpectedEofError(Input<I> input, String expected) {
        return failure(input, expected);
    }

    /**
     * Creates an expected EOF error failure result.
     * Use this when there is trailing content when EOF was expected.
     *
     * @param input the input at which the failure occurred
     * @param <I>   the type of the input symbols
     * @param <A>   the type of the parsed value
     * @return a failure result with EXPECTED_EOF error type
     */
    static <I, A> Result<I, A> expectedEofError(Input<I> input) {
        return failure(input, "end of input");
    }

    /**
     * Creates a recursion error failure result.
     * Use this when infinite recursion is detected.
     *
     * @param input the input at which the failure occurred
     * @param <I>   the type of the input symbols
     * @param <A>   the type of the parsed value
     * @return a failure result with RECURSION error type
     */
    static <I, A> Result<I, A> recursionError(Input<I> input) {
        return failure(input, "no infinite recursion");
    }


    /**
     * Creates a validation error failure result.
     * Use this when input parsed but failed validation.
     *
     * @param input       the input at which the failure occurred
     * @param <I>         the type of the input symbols
     * @param <A>         the type of the parsed value
     * @return a failure result with VALIDATION error type
     */
    static <I, A> Result<I, A> validationError(Input<I> input, String expected) {
        return failure(input, expected);
    }

    /**
     * Creates an internal error failure result.
     * Use this for unexpected errors in parser logic.
     *
     * @param input   the input at which the failure occurred
     * @param <I>     the type of the input symbols
     * @param <A>     the type of the parsed value
     * @return a failure result with INTERNAL error type
     */
    static <I, A> Result<I, A> internalError(Input<I> input) {
        return failure(input, "parser to function correctly");
    }

    static <A, I> Result<I,A> failure(List<NoMatch<I,A>> failures) {
        return new NoMatch<>(null, null, null, failures);
    }

    /**
     * Returns true if this result is a success.
     *
     * @return true if this result is a success
     */
    boolean matches();

    /**
     * Returns the parsed value if this result is a success.
     * Throws an exception if this result is a failure.
     *
     * @return the parsed value
     * @throws java.lang.RuntimeException if this result is a failure
     */
     A value();

    /**
     * Returns the remaining input after parsing.
     *
     * @return the remaining input
     */
     Input<I> input();

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
     * Returns an Optional containing the parsed value if this result is a success.
     * If this result is a failure, returns an empty Optional.
     *
     * @return an Optional containing the parsed value, or an empty Optional if this result is a failure
     */
    default Optional<A> toOptional() {
        return matches() ? Optional.of(value()) : Optional.empty();
    }

    /**
     * Returns an Optional containing the error message if this result is a failure.
     * If this result is a success, returns an empty Optional.
     *
     * @return an Optional containing the error message, or an empty Optional if this result is a success
     */
    default Optional<String> errorOptional() {
        return !matches() ? Optional.of(error()) : Optional.empty();
    }

    /**
     * Apply one of two functions to this value.
     * @param success   the function to be applied to a successful value
     * @param failure   the function to be applied to a failure value
     * @param <B>       the function return type
     * @return          the result of applying either function
     */
    <B> B handle(Function<Result<I, A>, B> success, Function<Result<I, A>, B> failure);
}
