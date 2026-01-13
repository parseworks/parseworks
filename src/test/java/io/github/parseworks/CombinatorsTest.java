package io.github.parseworks;

import io.github.parseworks.parsers.Combinators;
import io.github.parseworks.parsers.Lexical;
import io.github.parseworks.parsers.Numeric;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static io.github.parseworks.parsers.Combinators.*;
import static org.junit.jupiter.api.Assertions.*;

public class CombinatorsTest {

    @Test
    public void testAny() {
        Parser<Character, Character> parser = any(Character.class);
        
        // Match case
        Result<Character, Character> result = parser.parse("a");
        assertTrue(result.matches());
        assertEquals('a', result.value());

        // EOF case should fail
        Result<Character, Character> eofResult = parser.parse("");
        assertFalse(eofResult.matches());
    }

    @Test
    public void testThrowError() {
        Parser<Character, Object> parser = throwError(() -> new IOException("Test exception"));
        assertThrows(IOException.class, () -> parser.parse("a"));
    }

    @Test
    public void testEof() {
        Parser<Character, Void> parser = eof();

        // Match case (empty input)
        Result<Character, Void> result = parser.parse("");
        assertTrue(result.matches());

        // Non-empty input should fail
        Result<Character, Void> failResult = parser.parse("a");
        assertFalse(failResult.matches());
    }

    @Test
    public void testFail() {
        Parser<Character, Object> parser = Combinators.fail();

        // Should always fail regardless of input
        assertFalse(parser.parse("a").matches());
        assertFalse(parser.parse("").matches());
    }

    @Test
    public void testNot() {
        Parser<Character, Character> aParser = Lexical.chr('a');
        Parser<Character, Character> notAParser = not(aParser);

        // Match case (not 'a')
        Result<Character, Character> result = notAParser.parse("b");
        assertTrue(result.matches());

        // NoMatch case ('a' is present)
        Result<Character, Character> failResult = notAParser.parse("a");
        assertFalse(failResult.matches());
    }

    @Test
    public void testIsNot() {
        Parser<Character, Character> parser = isNot('a');

        // Match case (not 'a')
        Result<Character, Character> result = parser.parse("b");
        assertTrue(result.matches());
        assertEquals('b', result.value());

        // NoMatch case ('a')
        assertFalse(parser.parse("a").matches());

        // EOF case
        assertFalse(parser.parse("").matches());
    }

    @Test
    public void testOneOfList() {
        List<Parser<Character, Character>> parsers = Arrays.asList(
                Lexical.chr('a'), Lexical.chr('b'), Lexical.chr('c')
        );
        Parser<Character, Character> parser = oneOf(parsers);

        // Match cases
        assertTrue(parser.parse("a").matches());
        assertTrue(parser.parse("b").matches());
        assertTrue(parser.parse("c").matches());

        // NoMatch case
        assertFalse(parser.parse("d").matches());

        // EOF case
        assertFalse(parser.parse("").matches());
    }

    @Test
    public void testOneOfVarargs() {
        // Test with 2 parsers
        Parser<Character, Character> parser2 = oneOf(Lexical.chr('a'), Lexical.chr('b'));
        assertTrue(parser2.parse("a").matches());
        assertTrue(parser2.parse("b").matches());
        assertFalse(parser2.parse("c").matches());

        // Test with 3 parsers
        Parser<Character, Character> parser3 = oneOf(Lexical.chr('a'), Lexical.chr('b'), Lexical.chr('c'));
        assertTrue(parser3.parse("c").matches());
        assertFalse(parser3.parse("d").matches());

        // Additional tests for 4, 5, and 6 parser variants
        Parser<Character, Character> parser4 = oneOf(Lexical.chr('a'), Lexical.chr('b'), Lexical.chr('c'), Lexical.chr('d'));
        assertTrue(parser4.parse("d").matches());

        Parser<Character, Character> parser5 = oneOf(Lexical.chr('a'), Lexical.chr('b'), Lexical.chr('c'), Lexical.chr('d'), Lexical.chr('e'));
        assertTrue(parser5.parse("e").matches());

        Parser<Character, Character> parser6 = oneOf(Lexical.chr('a'), Lexical.chr('b'), Lexical.chr('c'), Lexical.chr('d'), Lexical.chr('e'), Lexical.chr('f'));
        assertTrue(parser6.parse("f").matches());
    }

    @Test
    public void testSequenceList() {
        List<Parser<Character, Character>> parsers = Arrays.asList(
                Lexical.chr('a'), Lexical.chr('b'), Lexical.chr('c')
        );
        Parser<Character, List<Character>> parser = sequence(parsers);

        // Match case
        Result<Character, List<Character>> result = parser.parse("abc");
        assertTrue(result.matches());
        assertEquals(List.of('a', 'b', 'c'), result.value());

        // NoMatch cases
        assertFalse(parser.parse("ab").matches());  // incomplete
        assertFalse(parser.parse("abd").matches()); // wrong sequence
    }

    @Test
    public void testSequenceVarargs() {
        // Test with 2 parsers
        Parser<Character, String> parser2 = sequence(Lexical.chr('a'), Lexical.chr('b'))
                .map((a, b) -> String.valueOf(a) + b);
        assertTrue(parser2.parse("ab").matches());
        assertEquals("ab", parser2.parse("ab").value());

        // Test with 3 parsers
        Parser<Character, String> parser3 = sequence(Lexical.chr('a'), Lexical.chr('b'), Lexical.chr('c'))
                .map((a, b, c) -> String.valueOf(a) + b + c);
        assertTrue(parser3.parse("abc").matches());
        assertEquals("abc", parser3.parse("abc").value());
    }

    @Test
    public void testSatisfy() {
        Predicate<Character> isUppercase = Character::isUpperCase;
        Parser<Character, Character> parser = satisfy("uppercase letter", isUppercase);

        // Match case
        assertTrue(parser.parse("A").matches());
        assertEquals('A', parser.parse("A").value());

        // NoMatch case
        assertFalse(parser.parse("a").matches());

        // EOF case
        assertFalse(parser.parse("").matches());
    }

    @Test
    public void testIs() {
        Parser<Character, Character> parser = is('x');

        // Match case
        assertTrue(parser.parse("x").matches());
        assertEquals('x', parser.parse("x").value());

        // NoMatch case
        assertFalse(parser.parse("y").matches());

        // EOF case
        assertFalse(parser.parse("").matches());
    }

    @Test
    public void testChrPredicate() {
        Predicate<Character> isVowel = c -> "aeiouAEIOU".indexOf(c) >= 0;
        Parser<Character, Character> parser = Lexical.chr(isVowel);

        // Match cases
        assertTrue(parser.parse("a").matches());
        assertTrue(parser.parse("E").matches());

        // NoMatch case
        assertFalse(parser.parse("x").matches());
    }

    @Test
    public void testChrChar() {
        Parser<Character, Character> parser = Lexical.chr('!');

        // Match case
        assertTrue(parser.parse("!").matches());
        assertEquals('!', parser.parse("!").value());

        Parser<Character, String> keyword = Lexical.string("if").or(Lexical.string("else")).or(Lexical.string("while"));
        Parser<Character, String> identifier = Lexical.regex("[a-zA-Z][a-zA-Z0-9]*");
        Parser<Character, String> number = Numeric.numeric.oneOrMore().map(Lists::join);

       Parser<Character, String> token = oneOf(Arrays.asList(
         keyword,
       identifier,
         number
         ));

        // NoMatch case
        assertFalse(parser.parse("?").matches());
    }

    @Test
    public void testString() {
        Parser<Character, String> parser = Lexical.string("hello");

        var result = parser.parse("hello world");
        var result2 = parser.parse("hello");
        // Match case
        assertTrue(result.matches());
        assertEquals("hello", result2.value());

        // NoMatch cases
        assertFalse(parser.parse("hell").matches());   // prefix only
        assertFalse(parser.parse("world").matches());  // different string

        // Empty string case
        assertTrue(Lexical.string("").parse("").matches());
    }

    @Test
    public void testOneOfString() {
        Parser<Character, Character> parser = Lexical.oneOf("0123456789");

        // Match cases - all digits
        for (char c = '0'; c <= '9'; c++) {
            assertTrue(parser.parse(String.valueOf(c)).matches());
            assertEquals(c, parser.parse(String.valueOf(c)).value());
        }

        // NoMatch case
        assertFalse(parser.parse("a").matches());
    }

    @Test
    public void testRegex() {
        // Keep a simple composition example; detailed regex semantics live in RegexParserTest
        Parser<Character, String> letters = Lexical.regex("[A-Za-z]+");
        Result<Character, String> r1 = letters.parse("hello123");
        assertTrue(r1.matches());
        assertEquals("hello", r1.value());
        assertTrue(letters.parse("abc").matches());
        assertFalse(letters.parse("123").matches());
    }

    @Test
    public void testComplexParsers() {
        // Simple arithmetic expression: number + number
        Parser<Character, Integer> number = Lexical.oneOf("0123456789").map(Character::getNumericValue);
        Parser<Character, Character> plus = Lexical.chr('+');
        Parser<Character, Integer> expr = number.then(plus).then(number)
                .map((n1, op, n2) -> n1 + n2);

        // Match case
        assertTrue(expr.parse("2+3").matches());
        assertEquals(5, expr.parse("2+3").value());

        // NoMatch cases
        assertFalse(expr.parse("2-3").matches()); // Wrong operator
        assertFalse(expr.parse("23").matches());  // Missing operator
        assertFalse(expr.parse("2+").matches());  // Missing second number
    }

    @Test
    public void testJsonLikeParser() {
        // Parser for "key": "value" pattern
        Parser<Character, Character> quote = Lexical.chr('"');
        Parser<Character, String> chars = Lexical.chr(c -> c != '"').oneOrMore()
                .map(list -> {
                    StringBuilder sb = new StringBuilder();
                    for (Character c : list) {
                        sb.append(c);
                    }
                    return sb.toString();
                });
        Parser<Character, String> quotedString = chars.between(quote);
        Parser<Character, String> keyValue = quotedString.then(Lexical.string(": ")).then(quotedString)
                .map((key, sep, value) -> key + "=" + value);

        // Match case
        Result<Character, String> result = keyValue.parse("\"name\": \"John\"");
        assertTrue(result.matches());
        assertEquals("name=John", result.value());

        // NoMatch cases
        assertFalse(keyValue.parse("name: \"John\"").matches());    // Missing quotes around key
        assertFalse(keyValue.parse("\"name\":\"John\"").matches()); // Missing space after colon
    }

    @Test
    public void testCombinedNotIsNot() {
        // Test combining not and isNot
        Parser<Character, Character> notDigit = not(Lexical.chr(Character::isDigit));
        Parser<Character, Character> letter = Lexical.chr(Character::isLetter);

        // Parser that accepts a letter that's followed by a non-digit
        Parser<Character, Character> letterFollowedByNonDigit = letter.peek(notDigit);

        var firstResult = letterFollowedByNonDigit.parse("a");
        var secondResult = letterFollowedByNonDigit.parse("a1");
        var thirdResult = letterFollowedByNonDigit.parse("aX");

        assertFalse(firstResult.matches());
        assertFalse(secondResult.matches());
        assertTrue(thirdResult.matches());
    }
}
