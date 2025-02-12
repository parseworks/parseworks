package io.github.parseworks.impl;

import io.github.parseworks.Input;
import io.github.parseworks.Result;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * <p>Success class.</p>
 *
 * @author jason bailey
 * @version $Id: $Id
 */
public class Success<I, A> implements Result<I, A> {
    private final A value;
    private final Input<I> next;

    /**
     * <p>Constructor for Success.</p>
     *
     * @param value a A object
     * @param next  a {@link io.github.parseworks.Input} object
     */
    public Success(A value, Input<I> next) {
        this.value = value;
        this.next = next;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSuccess() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isError() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public A get() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Input<I> next() {
        return next;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <B> Result<I, B> cast() {
        return (Result<I, B>) this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <B> Result<I, B> map(java.util.function.Function<A, B> mapper) {
        return new Success<>(mapper.apply(value), next);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String error() {
        return "No error";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String fullErrorMessage() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Result<?, ?> cause() {
        return null;
    }

    @Override
    public <B> B handle(Function<Success<I, A>, B> success, Function<Failure<I, A>, B> failure) {
        return success.apply(this);
    }

}
