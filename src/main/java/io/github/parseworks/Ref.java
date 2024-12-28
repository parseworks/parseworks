package io.github.parseworks;

public class Ref<I, A> extends Parser<I, A> {
    private Parser<I, A> parser;

    public Ref() {
        super(() -> false, in -> {
            throw new IllegalStateException("Parser not initialized");
        });
    }

    public boolean isInitialised() {
        return parser != null;
    }

    public void set(Parser<I, A> parser) {
        this.parser = parser;
    }

    public Parser<I, A> get() {
        return parser;
    }
}