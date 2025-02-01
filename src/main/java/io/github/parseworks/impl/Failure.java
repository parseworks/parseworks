package io.github.parseworks.impl;

import io.github.parseworks.Input;
import io.github.parseworks.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a failure result in a parser combinator.
 *
 * @param <I> the type of the input symbols
 * @param <A> the type of the parsed value
 */
public class Failure<I, A> extends Result<I, A> {
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
     * Returns false as this represents a failure.
     *
     * @return false
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
     * Throws a RuntimeException with the full error message.
     *
     * @return never returns normally
     * @throws RuntimeException always thrown with the full error message
     */
    @Override
    public A getOrThrow() {
        throw new RuntimeException(getFullErrorMessage());
    }

    /**
     * Returns the input at the point of failure.
     *
     * @return the input at the point of failure
     */
    @Override
    public Input<I> next() {
        return input;
    }

    /**
     * Casts the result to another type.
     *
     * @param <B> the new type of the result
     * @return this failure cast to the new type
     */
    @Override
    @SuppressWarnings("unchecked")
    public <B> Result<I, B> cast() {
        return (Result<I, B>) this;
    }

    /**
     * Maps the result to another type.
     *
     * @param <B>    the new type of the result
     * @param mapper the function to apply to the result
     * @return this failure cast to the new type
     */
    @Override
    @SuppressWarnings("unchecked")
    public <B> Result<I, B> map(java.util.function.Function<A, B> mapper) {
        return (Result<I, B>) this;
    }

    /**
     * Returns the error message.
     *
     * @return the error message, or "No error message" if none is provided
     */
    @Override
    public String getError() {
        return message == null ? "No error message" : message;
    }

    /**
     * Returns the full error message by iterating through the chain of failures.
     *
     * @return the full error message
     */
    public String getFullErrorMessage() {
        List<String> messages = new ArrayList<>();
        Result<?, ?> current = this;
        while (current != null) {
            if (current.getError() != null) {
                messages.add(current.getError());
            }
            current = current.cause();
        }
        return String.join(" -> ", messages);
    }

    /**
     * Returns the cause of this failure.
     *
     * @return the cause of this failure
     */
    @Override
    public Result<?, ?> cause() {
        return cause;
    }

    /**
     * Handles the result by calling the appropriate consumer.
     *
     * @param success the consumer to call if this is a success
     * @param failure the consumer to call if this is a failure
     */
    @Override
    public void handle(Consumer<Success<I, A>> success, Consumer<Failure<I, A>> failure) {
        failure.accept(this);
    }
}