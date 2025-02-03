package io.github.parseworks.impl;

import io.github.parseworks.Input;
import io.github.parseworks.Result;

import java.util.function.Consumer;

/**
 * <p>Success class.</p>
 *
 * @author jason bailey
 * @version $Id: $Id
 */
public class Success<I, A> extends Result<I, A> {
    private final A value;
    private final Input<I> next;

    /**
     * <p>Constructor for Success.</p>
     *
     * @param value a A object
     * @param next a {@link io.github.parseworks.Input} object
     */
    public Success(A value, Input<I> next) {
        this.value = value;
        this.next = next;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSuccess() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isError() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public A getOrThrow() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public Input<I> next() {
        return next;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public <B> Result<I, B> cast() {
        return (Result<I, B>) this;
    }

    /** {@inheritDoc} */
    @Override
    public <B> Result<I, B> map(java.util.function.Function<A, B> mapper) {
        return new Success<>(mapper.apply(value), next);
    }

    /** {@inheritDoc} */
    @Override
    public String getError() {
        return "No error";
    }

    /** {@inheritDoc} */
    @Override
    public String getFullErrorMessage() {
        return "";
    }

    /** {@inheritDoc} */
    @Override
    public Result<?, ?> cause() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void handle(Consumer<Success<I, A>> success, Consumer<Failure<I, A>> failure) {
        success.accept(this);
    }
}
