package io.github.parseworks.impl;

import io.github.parseworks.Input;
import io.github.parseworks.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Represents a failure result in a parser combinator.
 *
 * @param <I> the type of the input symbols
 * @param <A> the type of the parsed value
 * @author jason bailey
 * @version $Id: $Id
 */
public class Failure<I, A> implements Result<I, A> {
    private final Input<I> input;
    private final String message;
    private final Result<?, ?> cause;

    /**
     * Constructs a new Failure with the specified input and message.
     *
     * @param input   the input at the point of failure
     * @param message the error message
     */
    public Failure(Input<I> input, String message) {
        this(input, message, null);
    }

    /**
     * Constructs a new Failure with the specified input, message, and cause.
     *
     * @param input   the input at the point of failure
     * @param message the error message
     * @param cause   the previous Failure that caused this failure
     */
    public Failure(Input<I> input, String message, Result<?, ?> cause) {
        this.input = input;
        this.message = message;
        this.cause = cause;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns false as this represents a failure.
     */
    @Override
    public boolean isSuccess() {
        return false;
    }

    /**
     * Returns true indicating this is an error result.
     *
     * @return true
     */
    public boolean isError() {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Throws a RuntimeException with the full error message.
     */
    @Override
    public A get() {
        throw new RuntimeException(fullErrorMessage());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the input at the point of failure.
     */
    @Override
    public Input<I> next() {
        return input;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Casts the result to another type.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <B> Result<I, B> cast() {
        return (Result<I, B>) this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Maps the result to another type.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <B> Result<I, B> map(java.util.function.Function<A, B> mapper) {
        return (Result<I, B>) this;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the error message.
     */
    @Override
    public String error() {
        return message == null ? "No error message" : message;
    }

    /**
     * Returns the full error message by iterating through the chain of failures.
     *
     * @return the full error message
     */
    public String fullErrorMessage() {
        List<String> messages = new ArrayList<>();
        Result<?, ?> current = this;
        while (current != null) {
            if (current.error() != null) {
                messages.add(current.error());
            }
            current = current.cause();
        }
        return String.join(" -> ", messages);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the cause of this failure.
     */
    @Override
    public Result<?, ?> cause() {
        return cause;
    }

    @Override
    public <B> B handle(Function<Success<I, A>, B> success, Function<Failure<I, A>, B> failure) {
        return failure.apply(this);
    }
}
