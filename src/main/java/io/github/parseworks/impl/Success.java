package io.github.parseworks.impl;

import io.github.parseworks.Input;
import io.github.parseworks.Result;

import java.util.function.Function;

/**
 * Represents a successful result in a parser combinator.
 *
 * @param <I> the type of the input symbols
 * @param <A> the type of the parsed value
 */
public record Success<I, A>(
        A value,
        Input<I> next
) implements Result<I, A> {

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public boolean isError() {
        return false;
    }

    @Override
    public A get() {
        return value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B> Result<I, B> cast() {
        return (Result<I, B>) this;
    }

    @Override
    public <B> Result<I, B> map(Function<A, B> mapper) {
        return new Success<>(mapper.apply(value), next);
    }

    @Override
    public String error() {
        return "";
    }

    @Override
    public <B> B handle(Function<Result<I, A>, B> success, Function<Result<I, A>, B> failure) {
        return success.apply(this);
    }
}