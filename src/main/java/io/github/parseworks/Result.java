package io.github.parseworks;

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
     * Returns the type of this result.
     *
     * @return the result type
     */
    ResultType type();

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
