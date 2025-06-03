package io.github.parseworks.impl;

import io.github.parseworks.Input;
import io.github.parseworks.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Represents a failure result in a parser combinator.
 *
 * @param <I> the type of the input symbols
 * @param <A> the type of the parsed value
 */
public record Failure<I, A>(
        Input<I> input,
        String expected,
        String found,
        Failure<?, ?> cause
) implements Result<I, A> {

    public Failure(Input<I> input, String expected, String found) {
        this(input, expected, found, null);
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public boolean isError() {
        return true;
    }

    @Override
    public A get() {
        throw new RuntimeException(fullErrorMessage());
    }

    @Override
    public Input<I> next() {
        return input;
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
    public String error() {
        String foundInstead = this.found != null ? this.found : input.hasMore() ? String.valueOf(input.current()) : "end of input";
        String message = "Position " + input.position() + ": Expected ";

        message += Objects.requireNonNullElse(expected, "correct input");
        if (input.hasMore()) {
            message += " but found " + foundInstead;
        } else {
            message += " but reached end of input";
        }
        return message;
    }

    public String fullErrorMessage() {
        List<String> messages = new ArrayList<>();
        Failure<?, ?> current = this;
        while (current != null) {
            messages.add(current.error());
            current = current.cause();
        }
        return String.join(" -> ", messages);
    }

    public Failure<?, ?> cause() {
        return cause;
    }

    @Override
    public <B> B handle(Function<Result<I, A>, B> success, Function<Result<I, A>, B> failure) {
        return failure.apply(this);
    }
}