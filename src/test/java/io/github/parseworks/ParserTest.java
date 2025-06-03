package io.github.parseworks;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;

import static io.github.parseworks.Combinators.chr;
import static io.github.parseworks.TextUtils.numeric;
import static org.junit.jupiter.api.Assertions.*;

public class ParserTest {

    @Test
    public void testParserRef() {
        Parser<Character, Character> ref = Parser.ref();
        Parser<Character, Character> realParser = chr('a');
        ref.set(realParser);

        Result<Character, Character> result = ref.parse("a");
        assertTrue(result.isSuccess());
        assertEquals('a', result.get());
    }

    @Test
    public void testParseAllWithPartialConsumption() {
        Parser<Character, Character> parser = chr('a');
        Result<Character, Character> result = parser.parseAll("ab");
        assertTrue(result.isError()); // Should fail as not all input is consumed
    }

    @Test
    public void testParseWithoutFullConsumption() {
        Parser<Character, Character> parser = chr('a');
        Result<Character, Character> result = parser.parse("ab");
        assertTrue(result.isSuccess()); // Should succeed as partial consumption is allowed
        assertEquals('a', result.get());
    }

    @Test
    public void testBetweenSameBracket() {
        // Create a parser for content (letters)
        Parser<Character, String> content = chr(Character::isLetter).many().map(chars -> {
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

        assertTrue(result.isSuccess());
        assertEquals(test, result.get());

        // Test with mismatched or missing quotes
        Result<Character, String> resultMissingClosing = quotedParser.parse("\"" + test);
        assertTrue(resultMissingClosing.isError());

        Result<Character, String> resultMissingOpening = quotedParser.parse(test + "\"");
        assertTrue(resultMissingOpening.isError());

        Result<Character, String> resultNoQuotes = quotedParser.parse(test);
        assertTrue(resultNoQuotes.isError());
    }

    @Test
    public void testBetweenWithParsers() {
        Parser<Character, Character> open = chr('[');
        Parser<Character, Character> close = chr(']');
        Parser<Character, Character> content = chr('a');

        Parser<Character, Character> parser = content.between(open, close);
        Result<Character, Character> result = parser.parse("[a]");

        assertTrue(result.isSuccess());
        assertEquals('a', result.get());
    }

    @Test
    public void testAs() {
        Parser<Character, String> parser = chr('a').as("constant");
        Result<Character, String> result = parser.parse("a");

        assertTrue(result.isSuccess());
        assertEquals("constant", result.get());
    }

    @Test
    public void testMap() {
        Parser<Character, Integer> parser = chr('a').map(c -> (int)c);
        Result<Character, Integer> result = parser.parse("a");

        assertTrue(result.isSuccess());
        assertEquals(Integer.valueOf('a'), result.get());
    }

    @Test
    public void testMultipleThen() {
        Parser<Character, Character> a = chr('a');
        Parser<Character, Character> b = chr('b');
        Parser<Character, Character> c = chr('c');

        Parser<Character, String> parser = a.then(b).then(c)
                .map((first, second, third) -> String.valueOf(first) + second + third);

        Result<Character, String> result = parser.parse("abc");
        assertTrue(result.isSuccess());
        assertEquals("abc", result.get());
    }


    @Test
    public void testPure() {
        Parser<Character, String> parser = Parser.pure("test");
        Input<Character> input = Input.of("");
        Result<Character, String> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals("test", result.get());
    }

    @Test
    public void testZeroOrMany() {
        Parser<Character, List<Character>> parser = chr(Character::isLetter).zeroOrMany().then(chr(Character::isDigit).zeroOrMany()).map(FList::appendAll);
        Input<Character> input = Input.of("abc123");
        Result<Character, List<Character>> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(6, result.get().size());
    }

    @Test
    public void testChainr1() {
        Parser<Character, Integer> number = TextUtils.number;
        Parser<Character, BinaryOperator<Integer>> plus = chr('+').map(op -> Integer::sum);
        Parser<Character, Integer> parser = number.chainRightMany(plus);
        Input<Character> input = Input.of("1+2+3");
        Result<Character, Integer> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(6, result.get());
    }


    @Test
    public void testBetween() {
        Parser<Character, String> content = chr(Character::isLetter).many().map(chars -> {
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
        assertTrue(result.isSuccess());
        assertEquals(test, result.get());
    }

    @Test
    public void testDigit() {
        Parser<Character, Character> parser = TextUtils.numeric;
        Input<Character> input = Input.of("5");
        Result<Character, Character> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals('5', result.get());
    }

    @Test
    public void testNumber() {
        Parser<Character, Integer> parser = TextUtils.number;
        Input<Character> input = Input.of("12345");
        Result<Character, Integer> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(12345, result.get());
    }

    @Test
    public void testFailure() {
        Parser<Character, Character> parser = chr('a');
        Input<Character> input = Input.of("b");
        Result<Character, Character> result = parser.parse(input);
        assertFalse(result.isSuccess());
    }

    @Test
    public void testChoice() {
        Parser<Character, Character> parser = chr('a').or(chr('b'));
        Input<Character> input = Input.of("b");
        Result<Character, Character> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals('b', result.get());
    }

    @Test
    public void testChainl1() {
        Parser<Character, Integer> number = TextUtils.number;
        Parser<Character, BinaryOperator<Integer>> plus = chr('+').map(op -> Integer::sum);
        Parser<Character, Integer> parser = number.chainLeftMany(plus);
        Input<Character> input = Input.of("1+2+3");
        Result<Character, Integer> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(6, result.get());
    }

    @Test
    public void testChainl() {
        Parser<Character, Integer> number = TextUtils.number;
        Parser<Character, BinaryOperator<Integer>> plus = chr('-').map(op -> (a, b) -> a - b);
        Parser<Character, Integer> parser = number.chainLeftMany(plus);
        Input<Character> input = Input.of("1-2-3");
        Result<Character, Integer> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(-4, result.get());
    }

    @Test
    public void testChainr() {
        Parser<Character, Integer> number = TextUtils.number;
        Parser<Character, BinaryOperator<Integer>> plus = chr('-').map(op -> (a, b) -> a - b);
        Parser<Character, Integer> parser = number.chainRightMany(plus);
        Input<Character> input = Input.of("1-2-3");
        Result<Character, Integer> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(2, result.get());
    }

    @Test
    public void testChainr2() {
        Parser<Character, Integer> number = TextUtils.number;
        Parser<Character, BinaryOperator<Integer>> plus = chr('-').as((a, b) -> a - b);
        Parser<Character, Integer> parser = number.chainRightMany(plus);
        Input<Character> input = Input.of("1-2-3");
        Result<Character, Integer> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(2, result.get());
    }

    @Test
    public void testSeparatedByZeroOrMany() {
        Parser<Character, FList<Character>> parser = chr(Character::isLetter).separatedByZeroOrMany(chr(','));
        Input<Character> input = Input.of("a,b,c");
        Result<Character, FList<Character>> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(3, result.get().size());
    }

    @Test
    public void testOptional() {
        Parser<Character, Optional<Character>> parser = chr('a').optional();
        Input<Character> input = Input.of("a");
        Result<Character, Optional<Character>> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertTrue(result.get().isPresent());
        assertEquals('a', result.get().get());
    }

    @Test
    public void testBetweenDifferentContent() {
        Parser<Character, Character> open = chr('[');
        Parser<Character, Character> close = chr(']');
        Parser<Character, String> content = chr(Character::isLetter).many().map(chars -> {
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
        assertTrue(result.isSuccess());
        assertEquals(test, result.get());
    }

    @Test
    public void testRepeat() {
        Parser<Character, FList<Character>> parser = chr('a').repeat(3);
        Input<Character> input = Input.of("aaa");
        Result<Character, FList<Character>> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(3, result.get().size());
    }

    @Test
    public void testRepeatAtLeast() {
        Parser<Character, FList<Character>> parser = chr('a').repeatAtLeast(2);
        Input<Character> input = Input.of("aaa");
        Result<Character, FList<Character>> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(3, result.get().size());
    }

    @Test
    public void testRepeatBetween() {
        Parser<Character, FList<Character>> parser = chr('a').repeat(2, 4);
        Input<Character> input = Input.of("aaa");
        Result<Character, FList<Character>> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(3, result.get().size());
    }

    @Test
    public void testTakeWhile() {
        // Create a parser that parses digits
        Parser<Character, Character> digitParser = chr(Character::isDigit);

        // Create a condition that checks if a character is a digit
        Parser<Character, Boolean> isDigit = digitParser.map(c -> true).orElse(false);

        // Create a parser that takes digits while they are present
        Parser<Character, FList<Character>> takeWhileParser = digitParser.takeWhile(isDigit);

        // Test case 1: Only digits
        Result<Character, FList<Character>> result1 = takeWhileParser.parse("12345");
        assertTrue(result1.isSuccess());
        assertEquals(5, result1.get().size());
        assertEquals(List.of('1', '2', '3', '4', '5'), result1.get());

        // Test case 2: Digits followed by letters
        Result<Character, FList<Character>> result2 = takeWhileParser.parse("123abc");
        assertTrue(result2.isSuccess());
        assertEquals(3, result2.get().size());
        assertEquals(List.of('1', '2', '3'), result2.get());

        // Test case 3: Starts with letters
        Result<Character, FList<Character>> result3 = takeWhileParser.parse("abc123");
        assertTrue(result3.isSuccess());
        assertEquals(0, result3.get().size()); // Empty list when no matches at start

        // Test case 4: Empty input
        Result<Character, FList<Character>> result4 = takeWhileParser.parse("");
        assertTrue(result4.isSuccess());
        assertEquals(0, result4.get().size()); // Empty list for empty input

        // Test case 5: Mixed content with digits returning
        Result<Character, FList<Character>> result5 = takeWhileParser.parse("123abc456");
        assertTrue(result5.isSuccess());
        assertEquals(3, result5.get().size());
        assertEquals(List.of('1', '2', '3'), result5.get());
    }

    @Test
    public void testRepeatAtMost() {
        Parser<Character, FList<Character>> parser = chr('a').repeatAtMost(3);

        // Test case 1: Less than max
        Result<Character, FList<Character>> result1 = parser.parse("aa");
        assertTrue(result1.isSuccess());
        assertEquals(2, result1.get().size());

        // Test case 2: Exactly max
        Result<Character, FList<Character>> result2 = parser.parse("aaa");
        assertTrue(result2.isSuccess());
        assertEquals(3, result2.get().size());

        // Test case 3: More than max (should only take max)
        Result<Character, FList<Character>> result3 = parser.parse("aaaaa");
        assertTrue(result3.isSuccess());
        assertEquals(3, result3.get().size());

        // Test case 4: Zero matches
        Result<Character, FList<Character>> result4 = parser.parse("bbb");
        assertTrue(result4.isSuccess());
        assertEquals(0, result4.get().size());
    }

    @Test
    public void testZeroOrManyUntil() {
        Parser<Character, FList<Character>> parser = chr('a').zeroOrManyUntil(chr(';'));

        // Test case 1: Zero matches with terminator
        Result<Character, FList<Character>> result1 = parser.parse(";");
        assertTrue(result1.isSuccess());
        assertEquals(0, result1.get().size());

        // Test case 2: Multiple matches with terminator
        Result<Character, FList<Character>> result2 = parser.parse("aaa;");
        assertTrue(result2.isSuccess());
        assertEquals(3, result2.get().size());

        // Test case 3: No terminator (should fail)
        Result<Character, FList<Character>> result3 = parser.parse("aaa");
        assertTrue(result3.isError());
    }

    @Test
    public void testManyUntil() {
        Parser<Character, FList<Character>> parser = chr('a').manyUntil(chr(';'));

        // Test case 1: Multiple matches with terminator
        Result<Character, FList<Character>> result1 = parser.parse("aaa;");
        assertTrue(result1.isSuccess());
        assertEquals(3, result1.get().size());

        // Test case 2: Zero matches with terminator (should fail)
        Result<Character, FList<Character>> result2 = parser.parse(";");
        assertTrue(result2.isError());
    }

    @Test
    public void testThenSkipAndSkipThen() {
        // Test thenSkip - keep first result, skip second
        Parser<Character, Character> thenSkipParser = chr('a').thenSkip(chr('b'));
        Result<Character, Character> result1 = thenSkipParser.parse("ab");
        assertTrue(result1.isSuccess());
        assertEquals('a', result1.get());

        // Test skipThen - skip first result, keep second
        Parser<Character, Character> skipThenParser = chr('a').skipThen(chr('b'));
        Result<Character, Character> result2 = skipThenParser.parse("ab");
        assertTrue(result2.isSuccess());
        assertEquals('b', result2.get());
    }

    @Test
    public void testOrElse() {
        Parser<Character, Character> parser = chr('a').orElse('x');

        // Test case 1: Success
        Result<Character, Character> result1 = parser.parse("a");
        assertTrue(result1.isSuccess());
        assertEquals('a', result1.get());

        // Test case 2: Failure but returns default
        Result<Character, Character> result2 = parser.parse("b");
        assertTrue(result2.isSuccess());
        assertEquals('x', result2.get());
    }

    @Test
    public void testIsNot() {
        Parser<Character, Character> parser = chr(Character::isLetter).isNot('b');

        // Should succeed when current character is 'a'
        Result<Character, Character> result1 = parser.parse("a");
        assertTrue(result1.isSuccess());
        assertEquals('a', result1.get());

        // Should fail when current character is 'b'
        Result<Character, Character> result2 = parser.parse("b");
        assertTrue(result2.isError());
    }

    @Test
    public void testTrim() {
        Parser<Character, Character> parser = chr('a').trim();

        // Test with whitespace before and after
        Result<Character, Character> result = parser.parse("  a  ");
        assertTrue(result.isSuccess());
        assertEquals('a', result.get());
        assertTrue(result.next().isEof());
    }

    @Test
    public void testSeparatedByMany() {
        Parser<Character, FList<Character>> parser = chr('a').separatedByMany(chr(','));

        // Test with multiple separated elements
        Result<Character, FList<Character>> result1 = parser.parse("a,a,a");
        assertTrue(result1.isSuccess());
        assertEquals(3, result1.get().size());

        // Test with single element (no separators)
        Result<Character, FList<Character>> result2 = parser.parse("a");
        assertTrue(result2.isSuccess());
        assertEquals(1, result2.get().size());

        // Test with no elements (should fail)
        Result<Character, FList<Character>> result3 = parser.parse("");
        assertTrue(result3.isError());
    }

    @Test
    public void testChainZeroOrMany() {
        Parser<Character, Integer> number = TextUtils.number;
        Parser<Character, BinaryOperator<Integer>> plus = chr('+').map(op -> Integer::sum);

        // Test chainLeftZeroOrMany
        Parser<Character, Integer> leftParser = number.chainLeftZeroOrMany(plus, 0);
        Result<Character, Integer> leftResult = leftParser.parse("");
        assertTrue(leftResult.isSuccess());
        assertEquals(0, leftResult.get());  // Should return default value for empty input

        // Test chainRightZeroOrMany
        Parser<Character, Integer> rightParser = number.chainRightZeroOrMany(plus, 0);
        Result<Character, Integer> rightResult = rightParser.parse("");
        assertTrue(rightResult.isSuccess());
        assertEquals(0, rightResult.get());  // Should return default value for empty input
    }

    @Test
    public void testNot() {
        // Create a parser that recognizes the letter 'a'
        Parser<Character, Character> aParser = chr('a');

        // Create a parser that recognizes digits
        Parser<Character, Character> digitParser = chr(Character::isDigit);

        // Create a parser that recognizes 'a' but only if there's no digit
        Parser<Character, Character> aNotDigitParser = aParser.not(digitParser);

        // Test case 1: Input 'a' - should succeed because there's no digit
        Result<Character, Character> result1 = aNotDigitParser.parse("a");
        assertTrue(result1.isSuccess());
        assertEquals('a', result1.get());

        // Test case 2: Input '5' - should fail because there's a digit
        Result<Character, Character> result2 = aNotDigitParser.parse("5");
        assertTrue(result2.isError());
        //assertEquals("Parser to fail", result2.fullErrorMessage());

        // Test case 3: Input 'a5' - should succeed because digit is after 'a'
        Result<Character, Character> result3 = aNotDigitParser.parse("a5");
        assertTrue(result3.isSuccess());
        assertEquals('a', result3.get());

        // Test case 4: Multiple negations - parser that matches 'a' but not 'a' followed by 'b'
        Parser<Character, Character> abParser = chr('a').then(chr('b')).map((a, b) -> a);
        Parser<Character, Character> aNotAbParser = aParser.not(abParser);

        // Should fail on "ab" because abParser succeeds
        Result<Character, Character> result4 = aNotAbParser.parse("ab");
        assertTrue(result4.isError());

        // Should succeed on "ac" because abParser fails
        Result<Character, Character> result5 = aNotAbParser.parse("ac");
        assertTrue(result5.isSuccess());
        assertEquals('a', result5.get());
    }

    @Test
    public void testBetweenParsers() {
        // Define a parser for the bracketed content (digits)
        Parser<Character, Integer> contentParser = numeric.map(Character::getNumericValue);

        // Define parsers for the opening and closing brackets
        Parser<Character, Character> openBracketParser = chr('[');
        Parser<Character, Character> closeBracketParser = chr(']');

        // Create a parser that parses content between brackets
        Parser<Character, Integer> betweenParser = contentParser.between(openBracketParser, closeBracketParser);

        // Test input
        String input = "[5]";
        Integer result = betweenParser.parse(Input.of(input)).get();

        // Verify the result
        assertEquals(5, result);
    }

    @Test
    public void testBetweenParsersEmptyContent() {
        // Define a parser for the bracketed content (digits)
        Parser<Character, Integer> contentParser = numeric.map(Character::getNumericValue);

        // Define parsers for the opening and closing brackets
        Parser<Character, Character> openBracketParser = chr('[');
        Parser<Character, Character> closeBracketParser = chr(']');

        // Create a parser that parses content between brackets
        Parser<Character, Integer> betweenParser = contentParser.between(openBracketParser, closeBracketParser);

        // Test input with empty content
        String input = "[]";
        var result = betweenParser.parse(Input.of(input));

        // Verify the result
        assertTrue(result.isError());
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
        assertTrue(result.isError());
    }


    @Test
    public void testSeparatedByManyEmptyInput() {
        // Define a parser for comma-separated integers
        Parser<Character, Integer> integerParser = numeric.map(Character::getNumericValue);
        Parser<Character, FList<Integer>> separatedByManyParser = integerParser.separatedByMany(chr(','));

        // Test input
        String input = "";
        var result = separatedByManyParser.parse(Input.of(input));


        // Verify the result
        assertTrue(result.isError());
        assertEquals("Position 0: Expected <number> but reached end of input", result.error());
    }

    @Test
    public void testSeparatedByManySingleElement() {
        // Define a parser for comma-separated integers
        Parser<Character, Integer> integerParser = numeric.map(Character::getNumericValue);
        Parser<Character, FList<Integer>> separatedByManyParser = integerParser.separatedByMany(chr(','));

        // Test input
        String input = "7";
        List<Integer> result = separatedByManyParser.parse(Input.of(input)).get();

        // Verify the result
        assertEquals(List.of(7), result);
    }

    @Test
    public void testSeparatedByManyTrailingSeparator() {
        // Define a parser for comma-separated integers
        Parser<Character, Integer> integerParser = numeric.map(Character::getNumericValue);
        Parser<Character, FList<Integer>> separatedByManyParser = integerParser.separatedByMany(chr(','));

        // Test input
        String input = "1,2,3,";
        FList<Integer> result = separatedByManyParser.parse(Input.of(input)).get();

        // Verify the result
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    public void testSeparatedByManyMultipleSeparators() {
        // Define a parser for comma-separated integers
        Parser<Character, Integer> integerParser = numeric.map(Character::getNumericValue);
        Parser<Character, FList<Integer>> separatedByManyParser = integerParser.separatedByMany(chr(','));

        // Test input
        String input = "1,,2,3";
        FList<Integer> result = separatedByManyParser.parse(Input.of(input)).get();
        //this would return the list on the case of an optional number.
        // Verify the result
        assertEquals(List.of(1), result);
    }

    @Test
    public void testSeparatedByManyNonNumericInput() {
        // Define a parser for comma-separated integers
        Parser<Character, Integer> integerParser = numeric.map(Character::getNumericValue);
        Parser<Character, FList<Integer>> separatedByManyParser = integerParser.separatedByMany(chr(','));

        // Test input
        String input = "a,b,c";
        Result<Character, FList<Integer>> result = separatedByManyParser.parse(Input.of(input));

        // Verify the result
        assertTrue(result.isError());
    }
}