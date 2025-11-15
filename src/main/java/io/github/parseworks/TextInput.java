package io.github.parseworks;

/**
 * {@code TextInput} extends the basic {@link Input} interface for character-based inputs
 * with methods for line/column information and formatted error reporting.
 * <p>
 * This interface provides additional context for error messages, making them more
 * informative and user-friendly.
 */
public interface TextInput extends Input<Character> {
    /**
     * Returns the current line number (1-based).
     *
     * @return the current line number
     */
    int line();
    
    /**
     * Returns the current column number (1-based).
     *
     * @return the current column number
     */
    int column();
    
    /**
     * Returns the line of text at the specified line number.
     *
     * @param lineNumber the line number (1-based)
     * @return the line of text, or null if the line number is out of range
     */
    String getLine(int lineNumber);
    
    /**
     * Returns a snippet of the input around the current position.
     *
     * @param before Number of characters to include before the current position
     * @param after Number of characters to include after the current position
     * @return A string representation of the input snippet
     */
    String getSnippet(int before, int after);
    
    /**
     * Returns a snippet of the input around the current position,
     * including line numbers and a caret marker.
     *
     * @param linesBefore Number of lines to include before the error line
     * @param linesAfter Number of lines to include after the error line
     * @return A formatted string with the input snippet
     */
    String getFormattedSnippet(int linesBefore, int linesAfter);
}