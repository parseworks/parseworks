package io.github.parseworks;

/**
 * Extends {@link Input} with line, column, and error reporting information.
 */
public interface TextInput extends Input<Character> {
    /** Returns the 1-based line number. */
    int line();
    
    /** Returns the 1-based column number. */
    int column();
    
    /** Returns the text at the specified line number. */
    String getLine(int lineNumber);
    
    /** Returns a snippet around the current position. */
    String getSnippet(int before, int after);
    
    /** Returns a formatted snippet with line numbers and a caret marker. */
    String getFormattedSnippet(int linesBefore, int linesAfter);
}