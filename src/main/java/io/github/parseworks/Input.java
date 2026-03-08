package io.github.parseworks;

import io.github.parseworks.impl.inputs.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;
import java.util.stream.Collectors;

/**
 * Represents a position in a stream of input symbols.
 *e
 */
public interface Input<I> {
    /** Creates an {@code Input} from a {@code char} array. */
    static Input<Character> of(char[] data) {
        return new CharSequenceInput(CharBuffer.wrap(data));
    }

    /** Creates an {@code Input} from a {@link CharSequence}. */
    static Input<Character> of(CharSequence s) {
        return new CharSequenceInput(s);
    }

    /** Creates an {@code Input} from a {@link Reader}. */
    static Input<Character> of(Reader rdr) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(rdr)) {
            String result = bufferedReader.lines().collect(Collectors.joining("\n"));
            return new CharSequenceInput(result); // String is a CharSequence
        }
    }

    CharSequence data();

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
