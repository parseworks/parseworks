package io.github.parseworks;

import io.github.parseworks.parsers.Lexical;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OnlyIfTest {

    @Test
    public void testOnlyIfWithCharPredicate() {
        Parser<Character, Character> parser = Lexical.chr('a').onlyIf(CharPredicate.is('a'));
        Result<Character, Character> result = parser.parse("a");
        assertTrue(result.matches(), "Should match 'a'");
        assertEquals('a', result.value());

        result = parser.parse("b");
        assertFalse(result.matches(), "Should not match 'b' because of onlyIf");
    }

    @Test
    public void testOnlyIfEOF() {
        Parser<Character, Character> parser = Lexical.chr('a').onlyIf(CharPredicate.is('a'));
        Result<Character, Character> result = parser.parse("");
        assertFalse(result.matches(), "Should not match EOF");
    }
}
