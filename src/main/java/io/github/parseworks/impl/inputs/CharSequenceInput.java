package io.github.parseworks.impl.inputs;

import io.github.parseworks.Input;
import io.github.parseworks.TextInput;

/**
 * An implementation of the {@link TextInput} interface that uses a {@link CharSequence} as the input source.
 * This class is immutable and now stores only position and data. Line, column, and line content
 * are computed lazily on demand to reduce memory and copying.
 */
public record CharSequenceInput(int position, CharSequence data) implements TextInput {

    /**
     * Constructs a new {@code CharSequenceInput} starting at the beginning of the given {@code CharSequence}.
     *
     * @param data the {@code CharSequence} to be used as the input source
     */
    public CharSequenceInput(CharSequence data) {
        this(0, data);
    }

    // --- Derived properties (computed lazily) ---
    @Override
    public int line() {
        return computeLineAndColumn(this.data, this.position)[0];
    }

    @Override
    public int column() {
        return computeLineAndColumn(this.data, this.position)[1];
    }

    private static int[] computeLineAndColumn(CharSequence data, int position) {
        int line = 1;
        int column = 1;
        for (int i = 0; i < position && i < data.length(); i++) {
            if (data.charAt(i) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new int[] { line, column };
    }

    private static int totalLines(CharSequence data) {
        if (data.isEmpty()) return 1; // treat empty as single empty line
        int lines = 1;
        for (int i = 0; i < data.length(); i++) {
            if (data.charAt(i) == '\n') lines++;
        }
        return lines;
    }

    /**
     * Checks if the end of the input has been reached.
     */
    @Override
    public boolean isEof() {
        return position >= data.length();
    }

    /**
     * Returns the current character at the current position in the input.
     */
    @Override
    public Character current() {
        return data.charAt(position);
    }

    /**
     * Move to next position.
     */
    @Override
    public Input<Character> next() {
        if (isEof()) {
            throw new IllegalStateException("End of input");
        }
        return new CharSequenceInput(position + 1, data);
    }

    /**
     * Skip by an offset with bounds checking.
     */
    @Override
    public Input<Character> skip(int offset) {
        if (offset == 0) {
            return this;
        }
        int newPosition = position + offset;
        if (newPosition > data.length()) newPosition = data.length();
        if (newPosition < 0) newPosition = 0;
        return new CharSequenceInput(newPosition, data);
    }

    /**
     * Returns the line of text at the specified line number (1-based), computed on demand.
     */
    @Override
    public String getLine(int lineNumber) {
        if (lineNumber < 1) return null;
        int currentLine = 1;
        int lineStart = 0;
        for (int i = 0; i <= data.length(); i++) {
            boolean atEnd = (i == data.length());
            char c = atEnd ? '\n' : data.charAt(i);
            if (c == '\n') {
                if (currentLine == lineNumber) {
                    return data.subSequence(lineStart, i).toString();
                }
                currentLine++;
                lineStart = i + 1;
            }
        }
        // If requesting last line and no trailing newline, handled in loop via atEnd
        return null;
    }

    /**
     * Returns a snippet of the input around the current position.
     */
    @Override
    public String getSnippet(int before, int after) {
        if (isEof()) {
            return "EOF";
        }
        // Keep previous behavior
        int start = Math.max(0, position - before - 2);
        int end = Math.min(data.length(), Math.max(0, position + after - 1));
        if (end < start) {
            end = start;
        }
        return data.subSequence(start, end).toString();
    }

    /**
     * Returns a formatted snippet with line numbers and a caret at the current column.
     */
    @Override
    public String getFormattedSnippet(int linesBefore, int linesAfter) {
        // Even at EOF, render a caret-based snippet to provide consistent context
        int line = line();
        int column = column();
        int total = totalLines(data);
        int startLine = Math.max(1, line - linesBefore);
        int endLine = Math.min(total, line + linesAfter);

        StringBuilder snippet = new StringBuilder();
        int lineNumberWidth = String.valueOf(endLine).length();

        for (int i = startLine; i <= endLine; i++) {
            String lineText = getLine(i);
            if (lineText == null) lineText = "";
            snippet.append(String.format("%" + lineNumberWidth + "d | %s%n", i, lineText));
            if (i == line) {
                int totalSpaces = lineNumberWidth + 3 + Math.max(0, column - 1);
                snippet.append(" ".repeat(totalSpaces)).append("^").append(System.lineSeparator());
            }
        }
        return snippet.toString();
    }

    @Override
    public String toString() {
        final String dataStr = isEof() ? "EOF" : String.valueOf(data.charAt(position));
        return "CharSequenceInput{position=" + position + ", line=" + line() + ", column=" + column() + ", data=\"" + dataStr + "\"}";
    }
}
