package io.github.parseworks.impl.inputs;


import io.github.parseworks.ContextMap;
import io.github.parseworks.Input;

import java.util.Map;

/**
 * An implementation of the {@link Input} interface that uses a {@code char} array as the input source.
 * This class is immutable and provides methods to navigate through the characters of the input.
 */
public record CharArrayInput(int position, char[] data, Map<Object,Object> context) implements Input<Character> {

    /**
     * An implementation of the {@link Input} interface that uses a {@code char} array as the input source.
     * This class is immutable and provides methods to navigate through the characters of the input.
     */
    public CharArrayInput(char[] data) {
        this(0, data, new ContextMap<>());
    }

    /**
     * Checks if the end of the input has been reached.
     *
     * @return {@code true} if the current position is at or beyond the end of the input, {@code false} otherwise
     */
    @Override
    public boolean isEof() {
        return position >= data.length;
    }

    /**
     * Returns the current character at the current position in the input.
     *
     * @return the current character
     * @throws IndexOutOfBoundsException if the current position is beyond the end of the input
     */
    @Override
    public Character current() {
        return data[position];
    }

    /**
     * Returns a new {@code CharArrayInput} instance representing the next position in the input.
     *
     * @return a new {@code CharArrayInput} with the position incremented by one
     */
    @Override
    public Input<Character> next() {
        return new CharArrayInput(position + 1, data, new ContextMap<>(context));
    }

    /**
     * Returns a new {@code CharArrayInput} instance representing the position offset by the given value.
     *
     * @param offset the offset to add to the current position
     * @return a new {@code CharArrayInput} with the position incremented by the given offset
     */
    @Override
    public Input<Character> skip(int offset) {
        return new CharArrayInput(position + offset,data, new ContextMap<>(context));
    }


    /**
     * Returns a string representation of the {@code CharArrayInput}.
     *
     * @return a string representation of the {@code CharArrayInput}
     */
    @Override
    public String toString() {
        final String dataStr = isEof() ? "EOF" : String.valueOf(data[position]);
        return "CharArrayInput{" + position + ",data=\"" + dataStr + "\"";
    }

}
