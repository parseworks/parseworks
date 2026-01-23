package io.github.parseworks;

import io.github.parseworks.impl.inputs.CharSequenceInput;
import io.github.parseworks.impl.result.Match;
import org.junit.jupiter.api.Test;

import static io.github.parseworks.parsers.Lexical.string;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the enhanced error messaging features in the ParseWorks library.
 */
public class EnhancedErrorMessagingTest {

    @Test
    public void testTextInputLineAndColumn() {
        TextInput input = new CharSequenceInput("line 1\nline 2\nline 3");
        
        // Skip to position 8 (line 2, column 2)
        TextInput skipped = (TextInput) input.skip(8);
        
        assertEquals(2, skipped.line());
        assertEquals(2, skipped.column());
        assertEquals("line 2", skipped.getLine(2));
    }
    
    @Test
    public void testTextInputSnippets() {
        TextInput input = new CharSequenceInput("line 1\nline 2\nline 3");
        
        // Skip to position 8 (line 2, column 2)
        TextInput skipped = (TextInput) input.skip(8);
        
        // Get a snippet around the current position
        String snippet = skipped.getSnippet(3, 3);
        assertEquals("e 1\nlin", snippet);
        
        // Get a formatted snippet
        String formatted = skipped.getFormattedSnippet(1, 1);
        assertTrue(formatted.contains("1 | line 1"));
        assertTrue(formatted.contains("2 | line 2"));
        assertTrue(formatted.contains("3 | line 3"));
        assertTrue(formatted.contains("     ^"));  // Caret marker at column 2
    }
    
    // Removed severity-level tests as Severity has been eliminated

    
    // Removed suggestion-related tests as suggestions feature has been removed
    
    @Test
    public void testErrorRecovery() {
        // Create a parser that recovers from errors
        Parser<Character, String> parser = string("hello")
            .recover(string("world"));
            
        // Test successful parsing with the primary parser
        Result<Character, String> result1 = parser.parse("hello");
        assertTrue(result1.matches());
        assertEquals("hello", result1.value());
        
        // Test recovery with the alternative parser
        Result<Character, String> result2 = parser.parse("world");
        assertTrue(result2.matches());
        assertEquals("world", result2.value());
        
        // Test failure when neither parser matches
        Result<Character, String> result3 = parser.parse("other");
        assertTrue(!result3.matches());
    }
    
    @Test
    public void testErrorRecoveryWithFunction() {
        // Create a parser that recovers from errors with a function
        Parser<Character, String> parser = string("hello")
            .recoverWith(failure -> 
                new Match<>("default", failure.input()));
                
        // Test successful parsing with the primary parser
        Result<Character, String> result1 = parser.parse("hello");
        assertTrue(result1.matches());
        assertEquals("hello", result1.value());
        
        // Test recovery with the function
        Result<Character, String> result2 = parser.parse("other");
        assertTrue(result2.matches());
        assertEquals("default", result2.value());
    }

}