package io.github.parseworks.impl.inputs;

import io.github.parseworks.Input;
import io.github.parseworks.TextInput;

/**
 * An {@link Input} decorator that always returns lowercase characters.
 * If the wrapped input is a {@link TextInput}, this also implements {@link TextInput}.
 */
public class LowercaseTextInput extends LowercaseInput implements TextInput {

    TextInput delegate;

    public LowercaseTextInput(TextInput delegate){
        super(delegate);
        this.delegate = delegate;
    }

    @Override
    public Input next() {
        return new LowercaseTextInput((TextInput) delegate.next());
    }

    @Override
    public int line() {
        return delegate.line();
    }

    @Override
    public int column() {
        return delegate.column();
    }

    @Override
    public String getLine(int lineNumber) {
        return delegate.getLine(lineNumber);
    }

    @Override
    public String getSnippet(int before, int after) {
        return delegate.getSnippet(before, after);
    }

    @Override
    public String getFormattedSnippet(int linesBefore, int linesAfter) {
        return delegate.getFormattedSnippet(linesBefore, linesAfter);
    }

    @Override
    public String toString() {
        return "LowercaseTextInput{delegate=" + delegate + "}";
    }
}
