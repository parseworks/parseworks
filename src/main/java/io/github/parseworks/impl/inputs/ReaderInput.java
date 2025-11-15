package io.github.parseworks.impl.inputs;

import io.github.parseworks.Input;

import java.io.IOException;
import java.io.Reader;

/**
 * An {@link io.github.parseworks.Input} backed by a {@link java.io.Reader}.
 * <p>
 * This input is a forward-only, single-pass cursor over a character stream. Each instance
 * represents a position in the stream and provides {@link #current()}, {@link #next()},
 * and {@link #skip(int)} to advance.
 * </p>
 *
 * <h2>Why this does not implement {@link io.github.parseworks.TextInput}</h2>
 * <p>
 * {@code ReaderInput} intentionally does not implement {@code TextInput}. Readers can be
 * non-repeatable and provide only sequential access. Accurate line/column reporting and
 * methods like {@code getLine(int)} or formatted snippets require random access to the
 * original text or full buffering. {@code ReaderInput} avoids buffering and therefore
 * cannot reliably compute that information.
 * </p>
 *
 * <h2>Semantics and limitations</h2>
 * <ul>
 *   <li><b>Single pass:</b> advancing consumes the underlying reader; previous positions
 *       cannot be revisited.</li>
 *   <li><b>EOF handling:</b> When the underlying reader is exhausted, {@link #isEof()}
 *       becomes {@code true} and {@link #current()} will throw.</li>
 *   <li><b>Skip behavior:</b> {@link #skip(int)} attempts to advance by {@code offset}
 *       characters. Note that {@link java.io.Reader#skip(long)} may skip fewer characters
 *       than requested; implementations should not assume full skipping is guaranteed.</li>
 *   <li><b>Resource management:</b> This class does not close the supplied reader.
 *       Callers are responsible for closing it when appropriate.</li>
 * </ul>
 *
 * <h2>Need line/column or formatted snippets?</h2>
 * <p>
 * If you need {@code TextInput} features (line/column, line extraction, caret diagnostics),
 * buffer the data first and use a random-access implementation such as
 * {@link io.github.parseworks.impl.inputs.CharSequenceInput} via
 * {@link io.github.parseworks.Input#of(CharSequence)}.
 * </p>
 */
public record ReaderInput(Reader reader, int position, int chr, boolean isEof) implements Input<Character> {

    // Normalize EOF flag to align with chr value across all constructors
    public ReaderInput {
        isEof = (chr == -1);
    }

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
    public Character current() {
        if (isEof) {
            throw new IllegalStateException("End of input");
        }
        return (char) chr;
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
    public Input<Character> skip(int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be non-negative");
        }
        if (offset == 0) {
            return this;
        }
        if (isEof) {
            throw new IllegalStateException("End of input");
        }

        int remaining = offset;
        int advanced = 0;
        try {
            while (remaining > 0) {
                long skipped;
                try {
                    skipped = reader.skip(remaining);
                } catch (IOException ioe) {
                    // On IO error during skip, consider stream unusable; mark EOF at current progress
                    return new ReaderInput(reader, position + advanced, -1, true);
                }
                if (skipped > 0) {
                    remaining -= (int) skipped;
                    advanced += (int) skipped;
                    continue;
                }
                // If skip made no progress, fall back to reading one char
                int c = readChar(reader);
                if (c == -1) {
                    // Reached EOF before skipping requested amount
                    return new ReaderInput(reader, position + advanced, -1, true);
                }
                remaining--;
                advanced++;
            }
            // Now at the new position; read current char for this position
            int nextChar = readChar(reader);
            return new ReaderInput(reader, position + offset, nextChar, nextChar == -1);
        } catch (RuntimeException ex) {
            // readChar throws RuntimeException on IO error; preserve position advanced so far
            return new ReaderInput(reader, position + advanced, -1, true);
        }
    }

    /**
     * Returns a string representation of the {@code ReaderInput}.
     *
     * @return a string representation of the {@code ReaderInput}
     */
    @Override
    public String toString() {
        String currentStr = (isEof || chr == -1) ? "EOF" : String.valueOf((char) chr);
        return "ReaderInput{position=" + position + ", current=" + currentStr + ", isEof=" + isEof + "}";
    }
}