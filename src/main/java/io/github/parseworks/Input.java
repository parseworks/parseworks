package io.github.parseworks;

import io.github.parseworks.impl.inputs.CharArrayInput;
import io.github.parseworks.impl.inputs.CharSequenceInput;
import io.github.parseworks.impl.inputs.ReaderInput;

import java.io.Reader;

/**
 * Represents a position in a stream of input symbols.
 *
 * @param <I> input symbol type
 */
public interface Input<I> {
    /** Creates an {@code Input} from a {@code char} array. */
    static Input<Character> of(char[] data) {
        return new CharArrayInput(data);
    }

    /** Creates an {@code Input} from a {@link CharSequence}. */
    static Input<Character> of(CharSequence s) {
        return new CharSequenceInput(s);
    }

    /** Creates an {@code Input} from a {@link Reader}. */
    static Input<Character> of(Reader rdr) {
        return new ReaderInput(rdr);
    }

    /** Returns true if at the end of input. */
    boolean isEof();

    /** Returns the current symbol. Throws if {@code isEof} is true. */
    I current();

    /** Returns the next position. Throws if {@code isEof} is true. */
    Input<I> next();

    /** Returns the current position. */
    int position();

    /** Returns a new input advanced by the given offset. */
    Input<I> skip(int offset);

    default boolean hasMore(){
        return !isEof();
    }
}

