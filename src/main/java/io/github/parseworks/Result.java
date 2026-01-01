package io.github.parseworks;

import io.github.parseworks.impl.result.NoMatch;
import io.github.parseworks.impl.result.Match;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * The `Result` interface represents the outcome of applying a parser to an input.
 * It can either be a Match result, containing the parsed value and the remaining input,
 * or a NoMatch result, containing an error message and the input at which the failure occurred.
 *
 * @param <I> the type of the input symbols
 * @param <A> the type of the parsed value
 * @author jason bailey
 * @version $Id: $Id
 */
public interface Result<I, A> {

    /**
     * Creates a Match result with the given value and remaining input.
     *
     * @param next  the remaining input
     * @param value the parsed value
     * @param <I>   the type of the input symbols
     * @param <A>   the type of the parsed value
     * @return a Match result
     */
    static <I, A> Result<I, A> success(Input<I> next, A value) {
        return new Match<>(value, next);
    }

    /**
     * Creates a NoMatch result that wraps another NoMatch as its cause, preserving the chain.
     * Uses the error type from the cause.
     *
     * @param input    the input at which the failure occurred
     * @param expected the expected input
     * @param cause    the underlying NoMatch to chain as the cause
     * @param <I>      the type of the input symbols
     * @param <A>      the type of the parsed value
     * @return a NoMatch result that chains the given cause
     */
    static <I, A> Result<I, A> failure(Input<I> input, String expected, NoMatch<?, ?> cause) {
        return new NoMatch<>(input, expected, cause);
    }

    /**
     * Creates a NoMatch result with the given expected value.
     * Uses the GENERIC error type by default.
     *
     * @param input    the input at which the failure occurred
     * @param expected the expected input
     * @param <I>      the type of the input symbols
     * @param <A>      the type of the parsed value
     * @return a NoMatch result
     */

    static <I, A> Result<I, A> failure(Input<I> input, String expected) {
        return new NoMatch<>(input, expected);
    }


    /**
     * Creates an unexpected EOF error NoMatch result.
     * Use this when the input ended prematurely.
     *
     * @param input    the input at which the failure occurred
     * @param expected what was expected before EOF
     * @param <I>      the type of the input symbols
     * @param <A>      the type of the parsed value
     * @return a NoMatch result with UNEXPECTED_EOF error type
     */
    static <I, A> Result<I, A> unexpectedEofError(Input<I> input, String expected) {
        return failure(input, expected);
    }

    /**
     * Creates an expected EOF error NoMatch result.
     * Use this when there is trailing content when EOF was expected.
     *
     * @param input the input at which the failure occurred
     * @param <I>   the type of the input symbols
     * @param <A>   the type of the parsed value
     * @return a NoMatch result with EXPECTED_EOF error type
     */
    static <I, A> Result<I, A> expectedEofError(Input<I> input) {
        return failure(input, "end of input");
    }

    /**
     * Creates a recursion error NoMatch result.
     * Use this when infinite recursion is detected.
     *
     * @param input the input at which the failure occurred
     * @param <I>   the type of the input symbols
     * @param <A>   the type of the parsed value
     * @return a NoMatch result with RECURSION error type
     */
    static <I, A> Result<I, A> recursionError(Input<I> input) {
        return failure(input, "no infinite recursion");
    }


    /**
     * Creates a validation error NoMatch result.
     * Use this when input parsed but failed validation.
     *
     * @param input       the input at which the failure occurred
     * @param <I>         the type of the input symbols
     * @param <A>         the type of the parsed value
     * @return a NoMatch result with VALIDATION error type
     */
    static <I, A> Result<I, A> validationError(Input<I> input, String expected) {
        return failure(input, expected);
    }

    /**
     * Creates an internal error NoMatch result.
     * Use this for unexpected errors in parser logic.
     *
     * @param input   the input at which the failure occurred
     * @param <I>     the type of the input symbols
     * @param <A>     the type of the parsed value
     * @return a NoMatch result with INTERNAL error type
     */
    static <I, A> Result<I, A> internalError(Input<I> input) {
        return failure(input, "parser to function correctly");
    }

    static <A, I> Result<I,A> failure(List<NoMatch<I,A>> failures) {
        return new NoMatch<>(null, null, null, failures);
    }

    /**
     * Returns true if this result is a Match.
     *
     * @return true if this result is a Match
     */
    boolean matches();

    /**
     * Returns the parsed value if this result is a Match.
     * Throws an exception if this result is a NoMatch.
     *
     * @return the parsed value
     * @throws java.lang.RuntimeException if this result is a NoMatch
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
     * Returns the error message if this result is a NoMatch.
     *
     * @return the error message, or an empty string if this result is a Match
     */
     String error();

    /**
     * Returns an Optional containing the parsed value if this result is a Match.
     * If this result is a NoMatch, returns an empty Optional.
     *
     * @return an Optional containing the parsed value, or an empty Optional if this result is a NoMatch
     */
    default Optional<A> toOptional() {
        return matches() ? Optional.of(value()) : Optional.empty();
    }

    /**
     * Returns an Optional containing the error message if this result is a NoMatch.
     * If this result is a Match, returns an empty Optional.
     *
     * @return an Optional containing the error message, or an empty Optional if this result is a Match
     */
    default Optional<String> errorOptional() {
        return !matches() ? Optional.of(error()) : Optional.empty();
    }

    /**
     * Apply one of two functions to this value.
     * @param success   the function to be applied to a Match result
     * @param failure   the function to be applied to a NoMatch result
     * @param <B>       the function return type
     * @return          the result of applying either function
     */
    <B> B handle(Function<Result<I, A>, B> success, Function<Result<I, A>, B> failure);
}
