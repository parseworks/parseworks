package io.github.parseworks.impl.inputs;

import io.github.parseworks.Input;

public class LowercaseInput implements Input<Character> {

    private final Input<Character> delegate;

    public LowercaseInput(Input<Character> delegate) {
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
                return Character.toLowerCase(delegate.data().charAt(index));
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return delegate.data().subSequence(start, end).toString().toLowerCase();
            }

            @Override
            public String toString() {
                return delegate.data().toString().toLowerCase();
            }
        };
    }

    @Override
    public boolean isEof() {
        return delegate.isEof();
    }

    @Override
    public Character current() {
        return Character.toLowerCase(delegate.current());
    }

    @Override
    public Input<Character> next() {
        return new LowercaseInput(delegate.next());
    }

    @Override
    public int position() {
        return delegate.position();
    }

    @Override
    public Input<Character> skip(int offset) {
        return new LowercaseInput(delegate.skip(offset));
    }

    @Override
    public boolean hasMore() {
        return delegate.hasMore();
    }

    @Override
    public String toString() {
        return "LowercaseTextInput{delegate=" + delegate + "}";
    }
}
