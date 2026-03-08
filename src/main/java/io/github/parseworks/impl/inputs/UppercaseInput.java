package io.github.parseworks.impl.inputs;

import io.github.parseworks.Input;
import io.github.parseworks.TextInput;

/**
 * An {@link Input} decorator that always returns uppercase characters.
 * If the wrapped input is a {@link TextInput}, this also implements {@link TextInput}.
 */
public class UppercaseInput implements Input<Character> {

    private final Input<Character> delegate;

    public UppercaseInput(Input<Character> delegate) {
        this.delegate = delegate;
    }

    public Input<Character> delegate() {
        return delegate;
    }

    @Override
    public CharSequence data() {
        return new CharSequence() {
            @Override
            public int length() {
                return delegate.data().length();
            }

            @Override
            public char charAt(int index) {
                return Character.toUpperCase(delegate.data().charAt(index));
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return delegate.data().subSequence(start, end).toString().toUpperCase();
            }

            @Override
            public String toString() {
                return delegate.data().toString().toUpperCase();
            }
        };
    }

    @Override
    public boolean isEof() {
        return delegate.isEof();
    }

    @Override
    public Character current() {
        return Character.toUpperCase(delegate.current());
    }

    @Override
    public Input<Character> next() {
        return new UppercaseInput(delegate.next());
    }

    @Override
    public int position() {
        return delegate.position();
    }

    @Override
    public Input<Character> skip(int offset) {
        return new UppercaseInput(delegate.skip(offset));
    }

    @Override
    public boolean hasMore() {
        return delegate.hasMore();
    }

    @Override
    public String toString() {
        return "UppercaseTextInput{delegate=" + delegate + "}";
    }
}
