package io.github.parseworks;

public class Success<I, A> extends Result<I, A> {
    private final A value;
    private final Input<I> next;

    public Success(A value, Input<I> next) {
        this.value = value;
        this.next = next;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public A getOrThrow() {
        return value;
    }

    @Override
    public Input<I> next() {
        return next;
    }

    @Override
    public int getPosition() {
        return next.position();
    }

    @Override
    public <B> Result<I, B> cast() {
        return (Result<I, B>) this;
    }

    @Override
    public <B> Result<I, B> map(java.util.function.Function<A, B> mapper) {
        return new Success<>(mapper.apply(value), next);
    }
}