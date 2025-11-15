package io.github.parseworks.impl.result;

import io.github.parseworks.Input;
import io.github.parseworks.Result;

import java.util.function.Function;

/**
 * Represents a successful result in a parser combinator.
 *
 * @param <I> the type of the input symbols
 * @param <A> the type of the parsed value
 */
public record Match<I, A>(
        A value,
        Input<I> input
) implements Result<I, A> {

    @Override
    public boolean matches() {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B> Result<I, B> cast() {
        return (Result<I, B>) this;
    }

    @Override
    public <B> Result<I, B> map(Function<A, B> mapper) {
        return new Match<>(mapper.apply(value), input);
    }

    @Override
    public String error() {
        return "";
    }

    @Override
    public <B> B handle(Function<Result<I, A>, B> success, Function<Result<I, A>, B> failure) {
        return success.apply(this);
    }

    public String toString() {
        return "Match(" + value + ")";
    }
}