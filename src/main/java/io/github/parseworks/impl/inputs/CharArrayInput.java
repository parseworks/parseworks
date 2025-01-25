package io.github.parseworks.impl.inputs;


import io.github.parseworks.Input;

public record CharArrayInput(int position, char[] data) implements Input<Character> {

    public CharArrayInput(char[] data) {
        this(0, data);
    }

    @Override
    public String toString() {
        final String dataStr = isEof() ? "EOF" : String.valueOf(data[position]);
        return "CharArrayInput{" + position + ",data=\"" + dataStr + "\"";
    }

    @Override
    public boolean isEof() {
        return position >= data.length;
    }

    @Override
    public Character current() {
        return data[position];
    }

    @Override
    public Input<Character> next() {
        return new CharArrayInput(position + 1, data);
    }

}
