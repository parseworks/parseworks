package io.github.parseworks;

import io.github.parseworks.impl.inputs.CharArrayInput;
import io.github.parseworks.impl.inputs.CharSequenceInput;
import io.github.parseworks.impl.inputs.ReaderInput;

import java.io.Reader;

/**
 * {@code Input} represents a position in a stream of input symbols,
 * that {@link Parser}s operate on.
 *
 * @param <I> the input stream symbol type
 */
public interface Input<I> {
    /**
     * Construct an {@code Input} from a {@code char} array.
     *
     * @param data the input data
     * @return the input stream
     */
    static Input<Character> of(char[] data) {
        return new CharArrayInput(data);
    }

    /**
     * Construct an {@code Input} from a {@link java.lang.String}.
     *
     * @param s the input data
     * @return the input stream
     */
    static Input<Character> of(String s) {
        return new CharSequenceInput(s);
    }

    /**
     * Construct an {@code Input} from a {@link java.io.Reader}.
     *
     * @param rdr the input data
     * @return the input stream
     */
    static Input<Character> of(Reader rdr) {
        return new ReaderInput(rdr);
    }

    /**
     * Returns true if and only if this input is at the end of the input stream.
     *
     * @return true if this input is at the end of the input stream
     */
    boolean isEof();

    /**
     * Returns the symbol from the stream indicated by this input.
     * Will throw if {@code isEof} is true.
     *
     * @return the next symbol
     */
    I get();

    /**
     * Get the next position in the input stream.
     * Will throw if {@code isEof} is true.
     *
     * @return the next position in the input stream
     */
    Input<I> next();

    /**
     * Returns the current position in the input stream.
     *
     * @return the current position in the input stream
     */
    int position();

}

