package io.github.parseworks.impl.inputs;

import io.github.parseworks.Input;

/**
 * An implementation of the {@link Input} interface that uses a {@link CharSequence} as the input source.
 * This class is immutable and provides methods to navigate through the characters of the input.
 */
public record CharSequenceInput(int position, CharSequence data) implements Input<Character> {

    /**
     * Constructs a new {@code CharSequenceInput} starting at the beginning of the given {@code CharSequence}.
     *
     * @param data the {@code CharSequence} to be used as the input source
     */
    public CharSequenceInput(CharSequence data) {
        this(0, data);
    }

    /**
     * Checks if the end of the input has been reached.
     *
     * @return {@code true} if the current position is at or beyond the end of the input, {@code false} otherwise
     */
    @Override
    public boolean isEof() {
        return position >= data.length();
    }

    /**
     * Returns the current character at the current position in the input.
     *
     * @return the current character
     * @throws IndexOutOfBoundsException if the current position is beyond the end of the input
     */
    @Override
    public Character current() {
        return data.charAt(position);
    }

    /**
     * Returns a new {@code CharSequenceInput} instance representing the next position in the input.
     *
     * @return a new {@code CharSequenceInput} with the position incremented by one
     */
    @Override
    public Input<Character> next() {
        return new CharSequenceInput(position + 1, data);
    }

    /**
     * Returns a new {@code CharSequenceInput} instance representing the position offset by the given value.
     *
     * @param offset the offset to add to the current position
     * @return a new {@code CharSequenceInput} with the position incremented by the given offset
     */
    @Override
    public Input<Character> skip(int offset) {
        return new CharSequenceInput(position + offset, data);
    }

    /**
     * Returns a string representation of the {@code CharSequenceInput}.
     *
     * @return a string representation of the {@code CharSequenceInput}
     */
    @Override
    public String toString() {
        return "ReaderInput{position=" + position + ", current=" + current() + ", isEof=" + isEof() + "}";
    }
}
