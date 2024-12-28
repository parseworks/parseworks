package io.github.parseworks;

public abstract class Result<I, A> {
    public abstract boolean isSuccess();

    public abstract A getOrThrow();

    public abstract Input<I> next();

    public abstract int getPosition();

    public static <I, A> Result<I, A> success(A value, Input<I> next) {
        return new Success<>(value, next);
    }

    public static <I, A> Result<I, A> failure(Input<I> input, String message) {
        return new Failure<>(input, message);
    }

    public static <I, A> Result<I, A> failureEof(Input<I> input, String message) {
        return new Failure<>(input, message);
    }

    public abstract <B> Result<I, B> cast();

    public abstract <B> Result<I, B> map(java.util.function.Function<A, B> mapper);
}