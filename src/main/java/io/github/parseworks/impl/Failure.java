package io.github.parseworks.impl;

import io.github.parseworks.Input;
import io.github.parseworks.Result;

import java.util.Optional;

public class Failure<I, A> extends Result<I, A> {
    private final Input<I> input;
    private final Optional<String> message;

    public Failure(Input<I> input, String message) {
        this.input = input;
        this.message = Optional.of(message);
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public A getOrThrow() {
        throw new RuntimeException(message.orElse("Parsing failed"));
    }

    @Override
    public Input<I> next() {
        return input;
    }

    @Override
    public int getPosition() {
        return input.position();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B> Result<I, B> cast() {
        return (Result<I, B>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B> Result<I, B> map(java.util.function.Function<A, B> mapper) {
        return (Result<I, B>) this;
    }

    @Override
    public String getError() {
        return message.orElse("No error message");
    }
}