package io.github.parseworks.parsers;

import io.github.parseworks.Parser;
import io.github.parseworks.impl.result.Match;
import io.github.parseworks.impl.result.NoMatch;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static io.github.parseworks.parsers.Lexical.escapedString;
import static org.junit.jupiter.api.Assertions.*;

public class EscapedStringTest {

    @Test
    public void testQuoteAsEscape() {
        // Case where ' is both quote and escape, and '' is used to escape '
        Parser<Character, String> parser = escapedString('\'', '\'', Map.of('\'', '\''));
        
        // This should match 'It''s' as "It's"
        var result = parser.parse("'It''s'");
        assertTrue(result.matches(), "Should match");
        assertEquals("It's", result.value());
    }

    @Test
    public void testQuoteAsEscapeWithNoMatchRollback() {
        // Case where ' is both quote and escape, but it's not followed by a character in the escape map.
        // If it's not a match in the escape map, it should be treated as the closing quote.
        Parser<Character, String> parser = escapedString('\'', '\'', Collections.emptyMap());
        
        var result = parser.parse("'abc'");
        assertTrue(result.matches(), "Should match 'abc'");
        assertEquals("abc", result.value());
    }

    @Test
    public void testQuote2(){
        var parser = escapedString('"','"', Map.of( '"', '"'));
        var string = parser.parse("\"abc\"");
        assertTrue(string.matches(), "Should match \"abc\"");
        assertEquals("abc", string.value());

        var result = parser.parse("\"v1,v2,3\nand 4\"");
        assertTrue(result.matches(), "Should match v1,v2,\"v,3\nand\"\" 4\"");
        System.out.println(result.value());
        //assertEquals("\v1,v2,v,3\nand\"\" 4\"", result.value());

    }

    @Test
    public void testStandardEscape() {
        // Test that standard escape still works
        Parser<Character, String> parser = escapedString('"', '\\', Map.of('n', '\n'));
        var result = parser.parse("\"a\\nb\"");
        assertTrue(result.matches());
        assertEquals("a\nb", result.value());
    }

    @Test
    public void testStandardEscapeInvalidSequence() {
        // Test that standard escape with invalid sequence still works (it literalizes the char)
        Parser<Character, String> parser = escapedString('"', '\\', Collections.emptyMap());
        var result = parser.parse("\"a\\xb\"");
        assertTrue(result.matches());
        assertEquals("axb", result.value());
    }
}
