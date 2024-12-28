package io.github.parseworks;

public class Tuple<A, B> {
    private final A first;
    private final B second;

    public Tuple(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public A getFirst() {
        return first;
    }

    public B getSecond() {
        return second;
    }

    public static <A, B> Tuple<A, B> of(A first, B second) {
        return new Tuple<>(first, second);
    }
}