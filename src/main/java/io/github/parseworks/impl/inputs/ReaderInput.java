package io.github.parseworks.impl.inputs;


import io.github.parseworks.Input;

import java.io.IOException;
import java.io.Reader;

/**
 * An implementation of the {@link Input} interface that uses a {@link Reader} as the input source.
 * This class provides methods to navigate through the characters of the input.
 */
public record ReaderInput(Reader reader, int position, int current, boolean isEof) implements Input<Character> {


    /**
     * Constructs a new {@code ReaderInput} starting at the beginning of the given {@code Reader}.
     *
     * @param reader the {@code Reader} to be used as the input source
     */
    public ReaderInput(Reader reader) {
        this(reader, 0, readChar(reader), false);
    }


    private static int readChar(Reader reader) {
        try {
            return reader.read();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read from reader", e);
        }
    }


    /**
     * Checks if the end of the input has been reached.
     *
     * @return {@code true} if the current position is at or beyond the end of the input, {@code false} otherwise
     */
    @Override
    public boolean isEof() {
        return isEof;
    }

    /**
     * Returns the current character at the current position in the input.
     *
     * @return the current character
     * @throws IllegalStateException if the current position is beyond the end of the input
     */
    @Override
    public Character get() {
        if (isEof) {
            throw new IllegalStateException("End of input");
        }
        return (char) current;
    }

    /**
     * Returns a new {@code ReaderInput} instance representing the next position in the input.
     *
     * @return a new {@code ReaderInput} with the position incremented by one
     */
    @Override
    public Input<Character> next() {
        if (isEof) {
            throw new IllegalStateException("End of input");
        }
        int nextChar = readChar(reader);
        return new ReaderInput(reader, position + 1, nextChar, nextChar == -1);
    }


    @Override
    public String toString() {
        return "ReaderInput{position=" + position + ", current=" + (char) current + ", isEof=" + isEof + "}";
    }
}