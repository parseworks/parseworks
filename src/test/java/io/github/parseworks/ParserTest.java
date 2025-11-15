package io.github.parseworks;

import io.github.parseworks.parsers.Lexical;
import io.github.parseworks.parsers.Numeric;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;

import static io.github.parseworks.parsers.Combinators.*;
import static io.github.parseworks.parsers.Numeric.numeric;
import static io.github.parseworks.parsers.Lexical.trim;
import static org.junit.jupiter.api.Assertions.*;

public class ParserTest {

    @Test
    public void testParserRef() {
        Parser<Character, Character> ref = Parser.ref();
        Parser<Character, Character> realParser = Lexical.chr('a');
        ref.set(realParser);

        Result<Character, Character> result = ref.parse("a");
        assertTrue(result.matches());
        assertEquals('a', result.value());
    }

    @Test
    public void testParseAllWithPartialConsumption() {
        Parser<Character, Character> parser = Lexical.chr('a');
        Result<Character, Character> result = parser.parseAll("ab");
        assertTrue(!result.matches()); // Should fail as not all input is consumed
    }

    @Test
    public void testParseWithoutFullConsumption() {
        Parser<Character, Character> parser = Lexical.chr('a');
        Result<Character, Character> result = parser.parse("ab");
        assertTrue(result.matches()); // Should succeed as partial consumption is allowed
        assertEquals('a', result.value());
    }

    @Test
    public void testBetweenSameBracket() {
        // Create a parser for content (letters)
        Parser<Character, String> content = Lexical.chr(Character::isLetter).oneOrMore().map(chars -> {
            StringBuilder sb = new StringBuilder();
            for (var c : chars) {
                sb.append(c);
            }
            return sb.toString();
        });

        // Create a parser that parses content between matching quote characters
        Parser<Character, String> quotedParser = content.between('"');

        // Test with properly quoted input
        String test = "hello";
        Result<Character, String> result = quotedParser.parse("\"" + test + "\"");

        assertTrue(result.matches());
        assertEquals(test, result.value());

        // Test with mismatched or missing quotes
        Result<Character, String> resultMissingClosing = quotedParser.parse("\"" + test);
        assertTrue(!resultMissingClosing.matches());

        Result<Character, String> resultMissingOpening = quotedParser.parse(test + "\"");
        assertTrue(!resultMissingOpening.matches());

        Result<Character, String> resultNoQuotes = quotedParser.parse(test);
        assertTrue(!resultNoQuotes.matches());
    }

    @Test
    public void testBetweenWithParsers() {
        Parser<Character, Character> open = Lexical.chr('[');
        Parser<Character, Character> close = Lexical.chr(']');
        Parser<Character, Character> content = Lexical.chr('a');

        Parser<Character, Character> parser = content.between(open, close);
        Result<Character, Character> result = parser.parse("[a]");

        assertTrue(result.matches());
        assertEquals('a', result.value());
    }

    @Test
    public void testAs() {
        Parser<Character, String> parser = Lexical.chr('a').as("constant");
        Result<Character, String> result = parser.parse("a");

        assertTrue(result.matches());
        assertEquals("constant", result.value());
    }

    @Test
    public void testMap() {
        Parser<Character, Integer> parser = Lexical.chr('a').map(c -> (int)c);
        Result<Character, Integer> result = parser.parse("a");

        assertTrue(result.matches());
        assertEquals(Integer.valueOf('a'), result.value());
    }

    @Test
    public void testMultipleThen() {
        Parser<Character, Character> a = Lexical.chr('a');
        Parser<Character, Character> b = Lexical.chr('b');
        Parser<Character, Character> c = Lexical.chr('c');

        Parser<Character, String> parser = a.then(b).then(c)
                .map((first, second, third) -> String.valueOf(first) + second + third);

        Result<Character, String> result = parser.parse("abc");
        assertTrue(result.matches());
        assertEquals("abc", result.value());
    }


    @Test
    public void testPure() {
        Parser<Character, String> parser = Parser.pure("test");
        Input<Character> input = Input.of("");
        Result<Character, String> result = parser.parse(input);
        assertTrue(result.matches());
        assertEquals("test", result.value());
    }

    @Test
    public void testZeroOrMore() {
        Parser<Character, List<Character>> parser = Lexical.chr(Character::isLetter).zeroOrMore().then(Lexical.chr(Character::isDigit).zeroOrMore()).map(FList::appendAll);
        Input<Character> input = Input.of("abc123");
        Result<Character, List<Character>> result = parser.parse(input);
        assertTrue(result.matches());
        assertEquals(6, result.value().size());
    }

    @Test
    public void testChainr1() {
        Parser<Character, Integer> number = Numeric.number;
        Parser<Character, BinaryOperator<Integer>> plus = Lexical.chr('+').map(op -> Integer::sum);
        Parser<Character, Integer> parser = number.chainRightOneOrMore(plus);
        Input<Character> input = Input.of("1+2+3");
        Result<Character, Integer> result = parser.parse(input);
        assertTrue(result.matches());
        assertEquals(6, result.value());
    }


    @Test
    public void testBetween() {
        Parser<Character, String> content = Lexical.chr(Character::isLetter).oneOrMore().map(chars -> {
            StringBuilder sb = new StringBuilder();
            for (var c : chars) {
                sb.append(c);
            }
            return sb.toString();
        });
        String test = "compute";
        Parser<Character, String> parser = content.between('(', ')');
        Input<Character> input = Input.of("(" + test + ")");
        Result<Character, String> result = parser.parse(input);
        assertTrue(result.matches());
        assertEquals(test, result.value());
    }

    @Test
    public void testDigit() {
        Input<Character> input = Input.of("5");
        Result<Character, Character> result = numeric.parse(input);
        assertTrue(result.matches());
        assertEquals('5', result.value());
    }

    @Test
    public void testNumber() {
        Parser<Character, Integer> parser = Numeric.number;
        Input<Character> input = Input.of("12345");
        Result<Character, Integer> result = parser.parse(input);
        assertTrue(result.matches());
        assertEquals(12345, result.value());
    }

    @Test
    public void testFailure() {
        Parser<Character, Character> parser = Lexical.chr('a');
        Input<Character> input = Input.of("b");
        Result<Character, Character> result = parser.parse(input);
        assertFalse(result.matches());
    }

    @Test
    public void testChoice() {
        Parser<Character, Character> parser = Lexical.chr('a').or(Lexical.chr('b'));
        Input<Character> input = Input.of("b");
        Result<Character, Character> result = parser.parse(input);
        assertTrue(result.matches());
        assertEquals('b', result.value());
    }

    @Test
    public void testChainl() {
        Parser<Character, Integer> number = Numeric.number;
        Parser<Character, BinaryOperator<Integer>> plus = Lexical.chr('-').map(op -> (a, b) -> a - b);
        Parser<Character, Integer> parser = number.chainLeftMany(plus);
        Input<Character> input = Input.of("1-2-3");
        Result<Character, Integer> result = parser.parse(input);
        assertTrue(result.matches());
        assertEquals(-4, result.value());
    }

    // Note: additional chain-right tests pruned; behavior covered by testChainr1 and AssociativityTest

    @Test
    public void testZeroOrMoreSeparatedBy() {
        Parser<Character, FList<Character>> parser = Lexical.chr(Character::isLetter).zeroOrManySeparatedBy(Lexical.chr(','));
        Input<Character> input = Input.of("a,b,c");
        Result<Character, FList<Character>> result = parser.parse(input);
        assertTrue(result.matches());
        assertEquals(3, result.value().size());
    }

    @Test
    public void testOptional() {
        Parser<Character, Optional<Character>> parser = Lexical.chr('a').optional();
        Input<Character> input = Input.of("a");
        Result<Character, Optional<Character>> result = parser.parse(input);
        assertTrue(result.matches());
        assertTrue(result.value().isPresent());
        assertEquals('a', result.value().get());
    }

    @Test
    public void testBetweenDifferentContent() {
        Parser<Character, Character> open = Lexical.chr('[');
        Parser<Character, Character> close = Lexical.chr(']');
        Parser<Character, String> content = Lexical.chr(Character::isLetter).oneOrMore().map(chars -> {
            StringBuilder sb = new StringBuilder();
            for (var c : chars) {
                sb.append(c);
            }
            return sb.toString();
        });
        String test = "example";
        Parser<Character, String> parser = content.between(open, close);
        Input<Character> input = Input.of("[" + test + "]");
        Result<Character, String> result = parser.parse(input);
        assertTrue(result.matches());
        assertEquals(test, result.value());
    }

    @Test
    public void testRepeat() {
        Parser<Character, FList<Character>> parser = Lexical.chr('a').repeat(3);
        Input<Character> input = Input.of("aaa");
        Result<Character, FList<Character>> result = parser.parse(input);
        assertTrue(result.matches());
        assertEquals(3, result.value().size());
    }

    @Test
    public void testRepeatAtLeast() {
        Parser<Character, FList<Character>> parser = Lexical.chr('a').repeatAtLeast(2);
        Input<Character> input = Input.of("aaa");
        Result<Character, FList<Character>> result = parser.parse(input);
        assertTrue(result.matches());
        assertEquals(3, result.value().size());
    }

    @Test
    public void testRepeatBetween() {
        Parser<Character, FList<Character>> parser = Lexical.chr('a').repeat(2, 4);
        Input<Character> input = Input.of("aaa");
        Result<Character, FList<Character>> result = parser.parse(input);
        assertTrue(result.matches());
        assertEquals(3, result.value().size());
    }

    @Test
    public void testTakeWhile() {
        // Create a parser that parses digits
        Parser<Character, Character> digitParser = Lexical.chr(Character::isDigit);

        // Create a condition that checks if a character is a digit
        Parser<Character, Boolean> isDigit = digitParser.map(c -> true).orElse(false);

        // Create a parser that takes digits while they are present
        Parser<Character, FList<Character>> takeWhileParser = digitParser.takeWhile(isDigit);

        // Test case 1: Only digits
        Result<Character, FList<Character>> result1 = takeWhileParser.parse("12345");
        assertTrue(result1.matches());
        assertEquals(5, result1.value().size());
        assertEquals(List.of('1', '2', '3', '4', '5'), result1.value());

        // Test case 2: Digits followed by letters
        Result<Character, FList<Character>> result2 = takeWhileParser.parse("123abc");
        assertTrue(result2.matches());
        assertEquals(3, result2.value().size());
        assertEquals(List.of('1', '2', '3'), result2.value());

        // Test case 3: Starts with letters
        Result<Character, FList<Character>> result3 = takeWhileParser.parse("abc123");
        assertTrue(result3.matches());
        assertEquals(0, result3.value().size()); // Empty list when no matches at start

        // Test case 4: Empty input
        Result<Character, FList<Character>> result4 = takeWhileParser.parse("");
        assertTrue(result4.matches());
        assertEquals(0, result4.value().size()); // Empty list for empty input

        // Test case 5: Mixed content with digits returning
        Result<Character, FList<Character>> result5 = takeWhileParser.parse("123abc456");
        assertTrue(result5.matches());
        assertEquals(3, result5.value().size());
        assertEquals(List.of('1', '2', '3'), result5.value());
    }

    @Test
    public void testRepeatAtMost() {
        Parser<Character, FList<Character>> parser = Lexical.chr('a').repeatAtMost(3);

        // Test case 1: Less than max
        Result<Character, FList<Character>> result1 = parser.parse("aa");
        assertTrue(result1.matches());
        assertEquals(2, result1.value().size());

        // Test case 2: Exactly max
        Result<Character, FList<Character>> result2 = parser.parse("aaa");
        assertTrue(result2.matches());
        assertEquals(3, result2.value().size());

        // Test case 3: More than max (should only take max)
        Result<Character, FList<Character>> result3 = parser.parse("aaaaa");
        assertTrue(result3.matches());
        assertEquals(3, result3.value().size());

        // Test case 4: Zero matches
        Result<Character, FList<Character>> result4 = parser.parse("bbb");
        assertTrue(result4.matches());
        assertEquals(0, result4.value().size());
    }

    @Test
    public void testZeroOrMoreUntil() {
        Parser<Character, FList<Character>> parser = Lexical.chr('a').zeroOrManyUntil(Lexical.chr(';'));

        // Test case 1: Zero matches with terminator
        Result<Character, FList<Character>> result1 = parser.parse(";");
        assertTrue(result1.matches());
        assertEquals(0, result1.value().size());

        // Test case 2: Multiple matches with terminator
        Result<Character, FList<Character>> result2 = parser.parse("aaa;");
        assertTrue(result2.matches());
        assertEquals(3, result2.value().size());

        // Test case 3: No terminator (should fail)
        Result<Character, FList<Character>> result3 = parser.parse("aaa");
        assertTrue(!result3.matches());
    }

    @Test
    public void testOneOrManyUntil() {
        Parser<Character, FList<Character>> parser = Lexical.chr('a').oneOrMoreUntil(Lexical.chr(';'));

        // Test case 1: Multiple matches with terminator
        Result<Character, FList<Character>> result1 = parser.parse("aaa;");
        assertTrue(result1.matches());
        assertEquals(3, result1.value().size());

        // Test case 2: Zero matches with terminator (should fail)
        Result<Character, FList<Character>> result2 = parser.parse(";");
        assertTrue(!result2.matches());
    }

    @Test
    public void testThenSkipAndSkipThen() {
        // Test thenSkip - keep first result, skip second
        Parser<Character, Character> thenSkipParser = Lexical.chr('a').thenSkip(Lexical.chr('b'));
        Result<Character, Character> result1 = thenSkipParser.parse("ab");
        assertTrue(result1.matches());
        assertEquals('a', result1.value());

        // Test skipThen - skip first result, keep second
        Parser<Character, Character> skipThenParser = Lexical.chr('a').skipThen(Lexical.chr('b'));
        Result<Character, Character> result2 = skipThenParser.parse("ab");
        assertTrue(result2.matches());
        assertEquals('b', result2.value());
    }

    @Test
    public void testOrElse() {
        Parser<Character, Character> parser = Lexical.chr('a').orElse('x');

        // Test case 1: Match
        Result<Character, Character> result1 = parser.parse("a");
        assertTrue(result1.matches());
        assertEquals('a', result1.value());

        // Test case 2: NoMatch but returns default
        Result<Character, Character> result2 = parser.parse("b");
        assertTrue(result2.matches());
        assertEquals('x', result2.value());
    }

    @Test
    public void testIsNot() {
        Parser<Character, Character> parser = Lexical.chr(Character::isLetter).where(isNot('b'));

        // Should succeed when current character is 'a'
        Result<Character, Character> result1 = parser.parse("a");
        assertTrue(result1.matches());
        assertEquals('a', result1.value());

        // Should fail when current character is 'b'
        Result<Character, Character> result2 = parser.parse("b");
        assertTrue(!result2.matches());
    }

    @Test
    public void testTrim() {
        Parser<Character, Character> parser = trim(Lexical.chr('a'));

        // Test with whitespace before and after
        Result<Character, Character> result = parser.parse("  a  ");
        assertTrue(result.matches());
        assertEquals('a', result.value());
        assertTrue(result.input().isEof());
    }

    @Test
    public void testOneOrMoreSeparatedBy() {
        Parser<Character, FList<Character>> parser = Lexical.chr('a').oneOrMoreSeparatedBy(Lexical.chr(','));

        // Test with multiple separated elements
        Result<Character, FList<Character>> result1 = parser.parse("a,a,a");
        assertTrue(result1.matches());
        assertEquals(3, result1.value().size());

        // Test with single element (no separators)
        Result<Character, FList<Character>> result2 = parser.parse("a");
        assertTrue(result2.matches());
        assertEquals(1, result2.value().size());

        // Test with no elements (should fail)
        Result<Character, FList<Character>> result3 = parser.parse("");
        assertTrue(!result3.matches());
    }

    @Test
    public void testChainZeroOrMore() {
        Parser<Character, Integer> number = Numeric.number;
        Parser<Character, BinaryOperator<Integer>> plus = Lexical.chr('+').map(op -> Integer::sum);

        // Test chainLeftZeroOrMany
        Parser<Character, Integer> leftParser = number.chainLeft(plus, 0);
        Result<Character, Integer> leftResult = leftParser.parse("");
        assertTrue(leftResult.matches());
        assertEquals(0, leftResult.value());  // Should return default value for empty input

        // Test chainRightZeroOrMany
        Parser<Character, Integer> rightParser = number.chainRight(plus, 0);
        Result<Character, Integer> rightResult = rightParser.parse("");
        assertTrue(rightResult.matches());
        assertEquals(0, rightResult.value());  // Should return default value for empty input
    }

    @Test
    public void testNot() {
        // Create a parser that recognizes the letter 'a'
        Parser<Character, Character> aParser = Lexical.chr('a');

        // Create a parser that recognizes digits
        Parser<Character, Character> digitParser = Lexical.chr(Character::isDigit);

        // Create a parser that recognizes 'a' followed by a non-digit
        Parser<Character, Character> aNotDigitParser = aParser.peek(not(digitParser));

        // Test case 1: Input 'a' - should fail because there's no character after 'a' for not(digitParser) to check
        Result<Character, Character> result1 = aNotDigitParser.parse("a");
        assertFalse(result1.matches());

        // Test case 2: Input '5' - should fail because it doesn't start with 'a'
        Result<Character, Character> result2 = aNotDigitParser.parse("5");
        assertTrue(!result2.matches());
        //assertEquals("Parser to fail", result2.fullErrorMessage());

        // Test case 3: Input 'a5' - should fail because '5' is a digit
        Result<Character, Character> result3 = aNotDigitParser.parse("a5");
        assertFalse(result3.matches());

        // Test case 4: Multiple negations - parser that matches 'a' but not 'a' followed by 'b'
        Parser<Character, Character> abParser = Lexical.chr('a').then(Lexical.chr('b')).map((a, b) -> a);
        Parser<Character, Character> aNotAbParser = aParser.where(not(abParser));

        // Should fail on "ab" because abParser succeeds
        Result<Character, Character> result4 = aNotAbParser.parse("ab");
        assertTrue(!result4.matches());

        // Should succeed on "ac" because abParser fails
        Result<Character, Character> result5 = aNotAbParser.parse("ac");
        assertTrue(result5.matches());
        assertEquals('a', result5.value());
    }

    @Test
    public void testBetweenParsers() {
        // Define a parser for the bracketed content (digits)
        Parser<Character, Integer> contentParser = numeric.map(Character::getNumericValue);

        // Define parsers for the opening and closing brackets
        Parser<Character, Character> openBracketParser = Lexical.chr('[');
        Parser<Character, Character> closeBracketParser = Lexical.chr(']');

        // Create a parser that parses content between brackets
        Parser<Character, Integer> betweenParser = contentParser.between(openBracketParser, closeBracketParser);

        // Test input
        String input = "[5]";
        Integer result = betweenParser.parse(Input.of(input)).value();

        // Verify the result
        assertEquals(5, result);
    }

    @Test
    public void testBetweenParsersEmptyContent() {
        // Define a parser for the bracketed content (digits)
        Parser<Character, Integer> contentParser = numeric.map(Character::getNumericValue);

        // Define parsers for the opening and closing brackets
        Parser<Character, Character> openBracketParser = Lexical.chr('[');
        Parser<Character, Character> closeBracketParser = Lexical.chr(']');

        // Create a parser that parses content between brackets
        Parser<Character, Integer> betweenParser = contentParser.between(openBracketParser, closeBracketParser);

        // Test input with empty content
        String input = "[]";
        var result = betweenParser.parse(Input.of(input));

        // Verify the result
        assertTrue(!result.matches());
    }

    @Test
    public void testBetweenParsersNonNumericContent() {
        // Define a parser for the bracketed content (digits)
        Parser<Character, Integer> contentParser = numeric.map(Character::getNumericValue);

        // Create a parser that parses content between brackets
        Parser<Character, Integer> betweenParser = contentParser.between('[', ']');

        // Test input with non-numeric content
        String input = "[a]";
        var result = betweenParser.parse(Input.of(input));

        // Verify the result
        assertTrue(!result.matches());
    }


    @Test
    public void testOneOrMoreSeparatedByEmptyInput() {
        // Define a parser for comma-separated integers
        Parser<Character, Integer> integerParser = numeric.map(Character::getNumericValue);
        Parser<Character, FList<Integer>> separatedByManyParser = integerParser.oneOrMoreSeparatedBy(Lexical.chr(','));

        // Test input\
        String input = "fish";
        var result = separatedByManyParser.parse(Input.of(input));


        // Verify the result (avoid detailed error text checks outside error-focused suites)
        assertTrue(!result.matches());
    }

    @Test
    public void testOneOrMoreSeparatedBySingleElement() {
        // Define a parser for comma-separated integers
        Parser<Character, Integer> integerParser = numeric.map(Character::getNumericValue);
        Parser<Character, FList<Integer>> separatedByManyParser = integerParser.oneOrMoreSeparatedBy(Lexical.chr(','));

        // Test input
        String input = "7";
        List<Integer> result = separatedByManyParser.parse(Input.of(input)).value();

        // Verify the result
        assertEquals(List.of(7), result);
    }

    @Test
    public void testOneOrMoreSeparatedByTrailingSeparator() {
        // Define a parser for comma-separated integers
        Parser<Character, Integer> integerParser = numeric.map(Character::getNumericValue);
        Parser<Character, FList<Integer>> separatedByManyParser = integerParser.oneOrMoreSeparatedBy(Lexical.chr(','));

        // Test input
        String input = "1,2,3,";
        Result<Character, FList<Integer>> result = separatedByManyParser.parse(Input.of(input));

        // Verify the result
        assertTrue(!result.matches());
    }

    @Test
    public void testOneOrMoreSeparatedByMultipleSeparators() {
        // Define a parser for comma-separated integers
        Parser<Character, Integer> integerParser = numeric.map(Character::getNumericValue);
        Parser<Character, FList<Integer>> separatedByManyParser = integerParser.oneOrMoreSeparatedBy(Lexical.chr(','));

        // Test input
        String input = "1,,2,3";
        Result<Character, FList<Integer>> result = separatedByManyParser.parse(Input.of(input));
        //this would return the list on the case of an optional number.
        // Verify the result
        assertTrue(!result.matches(),"result should be an error");
    }

    @Test
    public void testOneOrMoreSeparatedByNonNumericInput() {
        // Define a parser for comma-separated integers
        Parser<Character, Integer> integerParser = numeric.map(Character::getNumericValue);
        Parser<Character, FList<Integer>> separatedByManyParser = integerParser.oneOrMoreSeparatedBy(Lexical.chr(','));

        // Test input
        String input = "a,b,c";
        Result<Character, FList<Integer>> result = separatedByManyParser.parse(Input.of(input));

        // Verify the result
        assertTrue(!result.matches());
    }
}
