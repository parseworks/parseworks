package io.github.parseworks;

/**
 * The `Ref` class is a parser that allows for the creation of recursive parsers.
 * It acts as a placeholder that can be set to a specific parser later.
 *
 * @param <I> the type of the input symbols
 * @param <A> the type of the parsed value
 */
public class Ref<I, A> extends Parser<I, A> {
    /**
     * Constructs a `Ref` parser that is initially uninitialized.
     */
    public Ref() {
        super(in -> {
            throw new IllegalStateException("Parser not initialized");
        });
    }

    /**
     * Constructs a `Ref` parser that is initialized with the given parser.
     *
     * @param parser the parser to initialize this `Ref` with
     */
    public Ref(Parser<I, A> parser) {
        super(in -> {
            throw new IllegalStateException("Parser not initialized");
        });
        set(parser);
    }

    /**
     * Sets the parser that this `Ref` refers to.
     *
     * @param parser the parser to set
     */
    public void set(Parser<I, A> parser) {
        this.applyHandler = parser.applyHandler;
    }

}