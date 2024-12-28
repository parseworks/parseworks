package io.github.parseworks.impl.inputs;


import io.github.parseworks.Input;

public record StringInput(int position, char[] data) implements Input<Character> {

    public StringInput(char[] data) {
        this(0, data);
    }

    @Override
    public String toString() {
        final String dataStr = isEof() ? "EOF" : String.valueOf(data[position]);
        return "StringInput{" + position + ",data=\"" + dataStr + "\"";
    }

    @Override
    public boolean isEof() {
        return position >= data.length;
    }

    @Override
    public Character get() {
        return data[position];
    }

    @Override
    public Input<Character> next() {
        return new StringInput(position + 1, data);
    }

}
