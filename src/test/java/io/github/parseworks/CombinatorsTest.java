package io.github.parseworks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static io.github.parseworks.Combinators.*;
import static org.junit.jupiter.api.Assertions.*;

public class CombinatorsTest {

    @Test
    public void testAny() {
        Parser<Character, Character> parser = any(Character.class);

        Parser<Character,String> notQuote = any(Character.class).not(chr('"')).map(String::valueOf);

        // Success case
        Result<Character, Character> result = parser.parse("a");
        assertTrue(result.isSuccess());
        assertEquals('a', result.get());

        // EOF case should fail
        Result<Character, Character> eofResult = parser.parse("");
        assertFalse(eofResult.isSuccess());
    }

    @Test
    public void testThrowError() {
        Parser<Character, Object> parser = throwError(() -> new IOException("Test exception"));
        assertThrows(IOException.class, () -> parser.parse("a"));
    }

    @Test
    public void testEof() {
        Parser<Character, Void> parser = eof();

        // Success case (empty input)
        Result<Character, Void> result = parser.parse("");
        assertTrue(result.isSuccess());

        // Non-empty input should fail
        Result<Character, Void> failResult = parser.parse("a");
        assertFalse(failResult.isSuccess());
    }

    @Test
    public void testFail() {
        Parser<Character, Object> parser = Combinators.fail();

        // Should always fail regardless of input
        assertFalse(parser.parse("a").isSuccess());
        assertFalse(parser.parse("").isSuccess());
    }

    @Test
    public void testNot() {
        Parser<Character, Character> aParser = chr('a');
        Parser<Character, Character> notAParser = not(aParser);

        // Success case (not 'a')
        Result<Character, Character> result = notAParser.parse("b");
        assertTrue(result.isSuccess());

        // Failure case ('a' is present)
        Result<Character, Character> failResult = notAParser.parse("a");
        assertFalse(failResult.isSuccess());
    }

    @Test
    public void testIsNot() {
        Parser<Character, Character> parser = isNot('a');

        // Success case (not 'a')
        Result<Character, Character> result = parser.parse("b");
        assertTrue(result.isSuccess());
        assertEquals('b', result.get());

        // Failure case ('a')
        assertFalse(parser.parse("a").isSuccess());

        // EOF case
        assertFalse(parser.parse("").isSuccess());
    }

    @Test
    public void testOneOfList() {
        List<Parser<Character, Character>> parsers = Arrays.asList(
                chr('a'), chr('b'), chr('c')
        );
        Parser<Character, Character> parser = oneOf(parsers);

        // Success cases
        assertTrue(parser.parse("a").isSuccess());
        assertTrue(parser.parse("b").isSuccess());
        assertTrue(parser.parse("c").isSuccess());

        // Failure case
        assertFalse(parser.parse("d").isSuccess());

        // EOF case
        assertFalse(parser.parse("").isSuccess());
    }

    @Test
    public void testOneOfVarargs() {
        // Test with 2 parsers
        Parser<Character, Character> parser2 = oneOf(chr('a'), chr('b'));
        assertTrue(parser2.parse("a").isSuccess());
        assertTrue(parser2.parse("b").isSuccess());
        assertFalse(parser2.parse("c").isSuccess());

        // Test with 3 parsers
        Parser<Character, Character> parser3 = oneOf(chr('a'), chr('b'), chr('c'));
        assertTrue(parser3.parse("c").isSuccess());
        assertFalse(parser3.parse("d").isSuccess());

        // Additional tests for 4, 5, and 6 parser variants
        Parser<Character, Character> parser4 = oneOf(chr('a'), chr('b'), chr('c'), chr('d'));
        assertTrue(parser4.parse("d").isSuccess());

        Parser<Character, Character> parser5 = oneOf(chr('a'), chr('b'), chr('c'), chr('d'), chr('e'));
        assertTrue(parser5.parse("e").isSuccess());

        Parser<Character, Character> parser6 = oneOf(chr('a'), chr('b'), chr('c'), chr('d'), chr('e'), chr('f'));
        assertTrue(parser6.parse("f").isSuccess());
    }

    @Test
    public void testSequenceList() {
        List<Parser<Character, Character>> parsers = Arrays.asList(
                chr('a'), chr('b'), chr('c')
        );
        Parser<Character, List<Character>> parser = sequence(parsers);

        // Success case
        Result<Character, List<Character>> result = parser.parse("abc");
        assertTrue(result.isSuccess());
        assertEquals(List.of('a', 'b', 'c'), result.get());

        // Failure cases
        assertFalse(parser.parse("ab").isSuccess());  // incomplete
        assertFalse(parser.parse("abd").isSuccess()); // wrong sequence
    }

    @Test
    public void testSequenceVarargs() {
        // Test with 2 parsers
        Parser<Character, String> parser2 = sequence(chr('a'), chr('b'))
                .map((a, b) -> String.valueOf(a) + b);
        assertTrue(parser2.parse("ab").isSuccess());
        assertEquals("ab", parser2.parse("ab").get());

        // Test with 3 parsers
        Parser<Character, String> parser3 = sequence(chr('a'), chr('b'), chr('c'))
                .map((a, b, c) -> String.valueOf(a) + b + c);
        assertTrue(parser3.parse("abc").isSuccess());
        assertEquals("abc", parser3.parse("abc").get());
    }

    @Test
    public void testSatisfy() {
        Predicate<Character> isUppercase = Character::isUpperCase;
        Parser<Character, Character> parser = satisfy("uppercase letter", isUppercase);

        // Success case
        assertTrue(parser.parse("A").isSuccess());
        assertEquals('A', parser.parse("A").get());

        // Failure case
        assertFalse(parser.parse("a").isSuccess());

        // EOF case
        assertFalse(parser.parse("").isSuccess());
    }

    @Test
    public void testIs() {
        Parser<Character, Character> parser = is('x');

        // Success case
        assertTrue(parser.parse("x").isSuccess());
        assertEquals('x', parser.parse("x").get());

        // Failure case
        assertFalse(parser.parse("y").isSuccess());

        // EOF case
        assertFalse(parser.parse("").isSuccess());
    }

    @Test
    public void testChrPredicate() {
        Predicate<Character> isVowel = c -> "aeiouAEIOU".indexOf(c) >= 0;
        Parser<Character, Character> parser = chr(isVowel);

        // Success cases
        assertTrue(parser.parse("a").isSuccess());
        assertTrue(parser.parse("E").isSuccess());

        // Failure case
        assertFalse(parser.parse("x").isSuccess());
    }

    @Test
    public void testChrChar() {
        Parser<Character, Character> parser = chr('!');

        // Success case
        assertTrue(parser.parse("!").isSuccess());
        assertEquals('!', parser.parse("!").get());

        Parser<Character, String> keyword = string("if").or(string("else")).or(string("while"));
        Parser<Character, String> identifier = regex("[a-zA-Z][a-zA-Z0-9]*");
        Parser<Character, String> number = NumericParsers.numeric.many().map(FList::joinChars);

       Parser<Character, String> token = oneOf(Arrays.asList(
         keyword,
       identifier,
         number
         ));

        // Failure case
        assertFalse(parser.parse("?").isSuccess());
    }

    @Test
    public void testString() {
        Parser<Character, String> parser = string("hello");

        var result = parser.parse("hello world");
        var result2 = parser.parse("hello");
        // Success case
        assertTrue(result.isSuccess());
        assertEquals("hello", result2.get());

        // Failure cases
        assertFalse(parser.parse("hell").isSuccess());   // prefix only
        assertFalse(parser.parse("world").isSuccess());  // different string

        // Empty string case
        assertTrue(string("").parse("").isSuccess());
    }

    @Test
    public void testOneOfString() {
        Parser<Character, Character> parser = oneOf("0123456789");

        // Success cases - all digits
        for (char c = '0'; c <= '9'; c++) {
            assertTrue(parser.parse(String.valueOf(c)).isSuccess());
            assertEquals(c, parser.parse(String.valueOf(c)).get());
        }

        // Failure case
        assertFalse(parser.parse("a").isSuccess());
    }

    @Test
    public void testRegex() {
        // Email pattern
        Parser<Character, String> parser = regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

        // Another regex for testing
        Parser<Character, String> parser2 = regex("[a-zA-Z]+");
        // Success case
        assertTrue(parser.parse("test@example.com").isSuccess());
        assertFalse(parser.parse("test@example").isSuccess());
        assertEquals("test@example.com", parser.parse("test@example.com").get());

        // Failure case
        assertFalse(parser.parse("not-an-email").isSuccess());

        assertFalse(parser2.parse("234454535233443234556435435435634534").isSuccess());

        // Prefix match case
        Result<Character, String> prefixResult = parser.parse("user@domain.com_extratext");
        assertTrue(prefixResult.isSuccess());
        assertEquals("user@domain.com", prefixResult.get());

        // With explicit start anchor
        Parser<Character, String> anchoredParser = regex("^[0-9]{3}-[0-9]{2}-[0-9]{4}");
        assertTrue(anchoredParser.parse("123-45-6789").isSuccess());
    }

    @Test
    public void testComplexParsers() {
        // Simple arithmetic expression: number + number
        Parser<Character, Integer> number = oneOf("0123456789").map(Character::getNumericValue);
        Parser<Character, Character> plus = chr('+');
        Parser<Character, Integer> expr = number.then(plus).then(number)
                .map((n1, op, n2) -> n1 + n2);

        // Success case
        assertTrue(expr.parse("2+3").isSuccess());
        assertEquals(5, expr.parse("2+3").get());

        // Failure cases
        assertFalse(expr.parse("2-3").isSuccess()); // Wrong operator
        assertFalse(expr.parse("23").isSuccess());  // Missing operator
        assertFalse(expr.parse("2+").isSuccess());  // Missing second number
    }

    @Test
    public void testJsonLikeParser() {
        // Parser for "key": "value" pattern
        Parser<Character, Character> quote = chr('"');
        Parser<Character, String> chars = chr(c -> c != '"').many()
                .map(list -> {
                    StringBuilder sb = new StringBuilder();
                    for (Character c : list) {
                        sb.append(c);
                    }
                    return sb.toString();
                });
        Parser<Character, String> quotedString = chars.between(quote);
        Parser<Character, String> keyValue = quotedString.then(string(": ")).then(quotedString)
                .map((key, sep, value) -> key + "=" + value);

        // Success case
        Result<Character, String> result = keyValue.parse("\"name\": \"John\"");
        assertTrue(result.isSuccess());
        assertEquals("name=John", result.get());

        // Failure cases
        assertFalse(keyValue.parse("name: \"John\"").isSuccess());    // Missing quotes around key
        assertFalse(keyValue.parse("\"name\":\"John\"").isSuccess()); // Missing space after colon
    }

    @Test
    public void testCombinedNotIsNot() {
        // Test combining not and isNot
        Parser<Character, Character> notDigit = not(chr(Character::isDigit));
        Parser<Character, Character> letter = chr(Character::isLetter);

        // Parser that accepts a letter that's not followed by a digit
        Parser<Character, Character> letterNotFollowedByNotDigit = letter.not(letter.thenSkip(notDigit));

        var firstResult = letterNotFollowedByNotDigit.parse("a");
        var secondResult = letterNotFollowedByNotDigit.parse("a1");
        var thirdResult = letterNotFollowedByNotDigit.parse("aX");

        assertTrue(firstResult.isError());
        assertTrue(secondResult.isSuccess());
        assertTrue(thirdResult.isError());
    }
}