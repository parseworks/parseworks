package io.github.parseworks;

public class Failure<I, A> extends Result<I, A> {
    private final Input<I> input;
    private final String message;

    public Failure(Input<I> input, String message) {
        this.input = input;
        this.message = message;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public A getOrThrow() {
        throw new RuntimeException(message);
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
    public <B> Result<I, B> cast() {
        return (Result<I, B>) this;
    }

    @Override
    public <B> Result<I, B> map(java.util.function.Function<A, B> mapper) {
        return (Result<I, B>) this;
    }
}