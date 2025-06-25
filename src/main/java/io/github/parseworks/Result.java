package io.github.parseworks;

import io.github.parseworks.impl.Failure;
import io.github.parseworks.impl.Failure.ErrorType;
import io.github.parseworks.impl.Success;

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
        return new Success<>(value, next);
    }

    /**
     * Creates a failure result with the given expected and found values.
     * Uses the GENERIC error type by default.
     *
     * @param input    the input at which the failure occurred
     * @param expected the expected input
     * @param found    what was actually found
     * @param <I>      the type of the input symbols
     * @param <A>      the type of the parsed value
     * @return a failure result
     */
    static <I, A> Result<I, A> failure(Input<I> input, String expected, String found) {
        return new Failure<>(input, expected, found);
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
        return new Failure<>(input, expected, null);
    }

    /**
     * Creates a failure result with the given expected and found values and a specific error type.
     *
     * @param input     the input at which the failure occurred
     * @param expected  the expected input
     * @param found     what was actually found
     * @param errorType the type of error
     * @param <I>       the type of the input symbols
     * @param <A>       the type of the parsed value
     * @return a failure result
     */
    static <I, A> Result<I, A> failure(Input<I> input, String expected, String found, ErrorType errorType) {
        return new Failure<>(input, expected, found, errorType);
    }

    /**
     * Creates a syntax error failure result.
     * Use this for errors where the input doesn't match the expected syntax.
     *
     * @param input    the input at which the failure occurred
     * @param expected the expected syntax
     * @param found    what was actually found
     * @param <I>      the type of the input symbols
     * @param <A>      the type of the parsed value
     * @return a failure result with SYNTAX error type
     */
    static <I, A> Result<I, A> syntaxError(Input<I> input, String expected, String found) {
        return failure(input, expected, found, ErrorType.SYNTAX);
    }

    /**
     * Creates a type error failure result.
     * Use this for errors where the input has correct syntax but wrong type.
     *
     * @param input    the input at which the failure occurred
     * @param expected the expected type
     * @param found    what was actually found
     * @param <I>      the type of the input symbols
     * @param <A>      the type of the parsed value
     * @return a failure result with TYPE error type
     */
    static <I, A> Result<I, A> typeError(Input<I> input, String expected, String found) {
        return failure(input, expected, found, ErrorType.TYPE);
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
        return failure(input, expected, "end of input", ErrorType.UNEXPECTED_EOF);
    }

    /**
     * Creates an expected EOF error failure result.
     * Use this when there is trailing content when EOF was expected.
     *
     * @param input the input at which the failure occurred
     * @param found the trailing content that was found
     * @param <I>   the type of the input symbols
     * @param <A>   the type of the parsed value
     * @return a failure result with EXPECTED_EOF error type
     */
    static <I, A> Result<I, A> expectedEofError(Input<I> input, String found) {
        return failure(input, "end of input", found, ErrorType.EXPECTED_EOF);
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
        return failure(input, "parser to make progress", "infinite recursion detected", ErrorType.RECURSION);
    }


    /**
     * Creates a validation error failure result.
     * Use this when input parsed but failed validation.
     *
     * @param input       the input at which the failure occurred
     * @param constraint  the validation constraint
     * @param actualValue the actual value that failed validation
     * @param <I>         the type of the input symbols
     * @param <A>         the type of the parsed value
     * @return a failure result with VALIDATION error type
     */
    static <I, A> Result<I, A> validationError(Input<I> input, String constraint, String actualValue) {
        return failure(input, constraint, actualValue, ErrorType.VALIDATION);
    }

    /**
     * Creates an internal error failure result.
     * Use this for unexpected errors in parser logic.
     *
     * @param input   the input at which the failure occurred
     * @param message the error message
     * @param <I>     the type of the input symbols
     * @param <A>     the type of the parsed value
     * @return a failure result with INTERNAL error type
     */
    static <I, A> Result<I, A> internalError(Input<I> input, String message) {
        return failure(input, "parser to function correctly", message, ErrorType.INTERNAL);
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
    <B> B handle(Function<Result<I, A>, B> success, Function<Result<I, A>, B> failure);
}
