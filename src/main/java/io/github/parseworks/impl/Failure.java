package io.github.parseworks.impl;

import io.github.parseworks.Input;
import io.github.parseworks.Result;

import java.util.ArrayList;
import java.util.List;
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
        StringBuilder message = new StringBuilder();

        // Add position information
        message.append("Position ").append(input.position()).append(": ");

        // Add parser context if available
        if (expected != null && !expected.isEmpty()) {
            // Make the expected message more descriptive
            if (expected.equals("eof")) {
                message.append("Expected end of input");
            } else if (expected.equals("one of")) {
                message.append("Expected one of the specified options");
            } else if (expected.equals("inequality")) {
                message.append("Expected a different value");
            } else if (expected.equals("equivalence")) {
                message.append("Expected an equivalent value");
            } else if (expected.equals("progress")) {
                message.append("Parser failed to make progress");
            } else {
                message.append("Expected ").append(expected);
            }
        } else {
            message.append("Expected correct input");
        }

        // Add what was found
        if (input.hasMore()) {
            message.append(" but found ").append(foundInstead);
        } else {
            message.append(" but reached end of input");
        }

        return message.toString();
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
