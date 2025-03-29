package io.github.parseworks;

import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.parseworks.Combinators.chr;
import static io.github.parseworks.TextUtils.digit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParserTest {



    @Test
    public void testBetweenParsers() {
        // Define a parser for the bracketed content (digits)
        Parser<Character, Integer> contentParser = digit.map(Character::getNumericValue);

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
        Parser<Character, Integer> contentParser = digit.map(Character::getNumericValue);

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
        Parser<Character, Integer> contentParser = digit.map(Character::getNumericValue);

        // Define parsers for the opening and closing brackets
        Parser<Character, Character> openBracketParser = chr('[');
        Parser<Character, Character> closeBracketParser = chr(']');

        // Create a parser that parses content between brackets
        Parser<Character, Integer> betweenParser = contentParser.between(openBracketParser, closeBracketParser);

        // Test input with non-numeric content
        String input = "[a]";
        var result = betweenParser.parse(Input.of(input));

        // Verify the result
        assertTrue(result.isError());
    }






    @Test
    public void testSeparatedByMany() {
        // Define a parser for comma-separated integers
        Parser<Character, Integer> integerParser = digit.map(Character::getNumericValue);
        Parser<Character, FList<Integer>> separatedByManyParser = integerParser.separatedByMany(chr(','));

        // Test input
        String input = "1,2,3,4,5";
        List<Integer> result = separatedByManyParser.parse(Input.of(input)).get();

        // Verify the result
        assertEquals(List.of(1, 2, 3, 4, 5), result);
    }


    @Test
    public void testSeparatedByManyEmptyInput() {
        // Define a parser for comma-separated integers
        Parser<Character, Integer> integerParser = digit.map(Character::getNumericValue);
        Parser<Character, FList<Integer>> separatedByManyParser = integerParser.separatedByMany(chr(','));

        // Test input
        String input = "";
        var result = separatedByManyParser.parse(Input.of(input));


        // Verify the result
        assertTrue(result.isError());
        assertEquals("Position 0: Expected <number> but reached end of input", result.fullErrorMessage());
    }

    @Test
    public void testSeparatedByManySingleElement() {
        // Define a parser for comma-separated integers
        Parser<Character, Integer> integerParser = digit.map(Character::getNumericValue);
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
        Parser<Character, Integer> integerParser = digit.map(Character::getNumericValue);
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
        Parser<Character, Integer> integerParser = digit.map(Character::getNumericValue);
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
        Parser<Character, Integer> integerParser = digit.map(Character::getNumericValue);
        Parser<Character, FList<Integer>> separatedByManyParser = integerParser.separatedByMany(chr(','));

        // Test input
        String input = "a,b,c";
        Result<Character, FList<Integer>> result = separatedByManyParser.parse(Input.of(input));

        // Verify the result
        assertTrue(result.isError());
    }
}