package io.github.parseworks.impl.inputs;

import io.github.parseworks.ContextMap;
import io.github.parseworks.Input;
import java.util.List;
import java.util.Map;

/**
 * An implementation of the {@link Input} interface that uses a {@link List} as the input source.
 * This class is immutable and provides methods to navigate through the elements of the input.
 *
 * @param <I> the type of the input elements
 */
public record ListInput<I>(int position, List<I> data, Map<Object,Object> context) implements Input<I> {

    /**
     * Constructs a new {@code ListInput} starting at the beginning of the given {@code List}.
     *
     * @param data the {@code List} to be used as the input source
     */
    public ListInput(List<I> data) {
        this(0, data,new ContextMap<>());
    }

    /**
     * Checks if the end of the input has been reached.
     *
     * @return {@code true} if the current position is at or beyond the end of the input, {@code false} otherwise
     */
    @Override
    public boolean isEof() {
        return position >= data.size();
    }

    /**
     * Returns the current element at the current position in the input.
     *
     * @return the current element
     * @throws IndexOutOfBoundsException if the current position is beyond the end of the input
     */
    @Override
    public I current() {
        return data.get(position);
    }

    /**
     * Returns a new {@code ListInput} instance representing the next position in the input.
     *
     * @return a new {@code ListInput} with the position incremented by one
     */
    @Override
    public Input<I> next() {
        return new ListInput<>(position + 1, data, new ContextMap<>(context));
    }

    @Override
    public Input<I> skip(int offset) {
        return new ListInput<>(position + offset, data, new ContextMap<>(context));
    }

    /**
     * Returns a string representation of the {@code ListInput}.
     *
     * @return a string representation of the {@code ListInput}
     */
    @Override
    public String toString() {
        final String dataStr = isEof() ? "EOF" : String.valueOf(current());
        return "ListInput{" + position + ",data=\"" + dataStr + "\"}";
    }
}