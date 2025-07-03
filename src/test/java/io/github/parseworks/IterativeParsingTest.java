package io.github.parseworks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static io.github.parseworks.TextParsers.*;
import static io.github.parseworks.Combinators.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

public class IterativeParsingTest {

    @Test
    public void testIterateParseWithWords() {
        // Parse space-separated words
        Parser<Character, String> wordParser = alpha.many().map(chars ->
            chars.stream().map(String::valueOf).collect(Collectors.joining()));

        String input = "hello world test";
        Input<Character> charInput = Input.of(input);

        Iterator<String> iterator = wordParser.iterateParse(charInput);

        List<String> words = new ArrayList<>();
        while (iterator.hasNext()) {
            words.add(iterator.next());
        }

        assertEquals(3, words.size());
        assertEquals("hello", words.get(0));
        assertEquals("world", words.get(1));
        assertEquals("test", words.get(2));
    }

    @Test
    public void testIterateParseWithNumbers() {
        Parser<Character, Integer> numberParser = NumericParsers.integer;
        String input = "123 456 789";
        Input<Character> charInput = Input.of(input);

        Iterator<Integer> iterator = numberParser.iterateParse( charInput);

        List<Integer> numbers = new ArrayList<>();
        while (iterator.hasNext()) {
            numbers.add(iterator.next());
        }

        assertEquals(3, numbers.size());
        assertEquals(123, numbers.get(0));
        assertEquals(456, numbers.get(1));
        assertEquals(789, numbers.get(2));
    }

    @Test
    public void testStreamParseWithWords() {
        Parser<Character, String> wordParser = alpha.many().map(chars ->
            chars.stream().map(String::valueOf).collect(Collectors.joining()));

        String input = "hello world test";
        Input<Character> charInput = Input.of(input);

        List<String> words = wordParser.streamParse(charInput)
            .collect(Collectors.toList());

        assertEquals(3, words.size());
        assertEquals("hello", words.get(0));
        assertEquals("world", words.get(1));
        assertEquals("test", words.get(2));
    }

    @Test
    public void testEmptyInput() {
        Parser<Character, String> wordParser = alpha.many().map(chars ->
            chars.stream().map(String::valueOf).collect(Collectors.joining()));

        Input<Character> charInput = Input.of("");
        Iterator<String> iterator = wordParser.iterateParse(charInput);

        assertFalse(iterator.hasNext());
    }

    @Test
    public void testIteratorNoSuchElementException() {
        Parser<Character, String> wordParser = alpha.many().map(chars ->
            chars.stream().map(String::valueOf).collect(Collectors.joining()));

        Input<Character> charInput = Input.of("test");
        Iterator<String> iterator = wordParser.iterateParse(charInput);

        assertTrue(iterator.hasNext());
        assertEquals("test", iterator.next());
        assertFalse(iterator.hasNext());

        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    public void testParserWithErrors() {
        Parser<Character, String> wordParser = alpha.many().map(chars ->
            chars.stream().map(String::valueOf).collect(Collectors.joining()));

        // Input with numbers between words - should skip over them
        String input = "hello123world456test";
        Input<Character> charInput = Input.of(input);

        List<String> words = wordParser.streamParse(charInput)
            .toList();

        assertEquals(3, words.size());
        assertEquals("hello", words.get(0));
        assertEquals("world", words.get(1));
        assertEquals("test", words.get(2));
    }

    @Test
    public void testMultipleCallsToHasNext() {
        Parser<Character, String> wordParser = alpha.many().map(chars ->
            chars.stream().map(String::valueOf).collect(Collectors.joining()));

        Input<Character> charInput = Input.of("test");
        Iterator<String> iterator = wordParser.iterateParse(charInput);

        assertTrue(iterator.hasNext());
        assertTrue(iterator.hasNext()); // Multiple calls should return the same result
        assertEquals("test", iterator.next());
        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasNext()); // Multiple calls should return the same result
    }

    @Test
    public void testParallelStreamProcessing() {
        Parser<Character, String> wordParser = alpha.many().map(chars ->
            chars.stream().map(String::valueOf).collect(Collectors.joining()));

        String input = "one two three four five";
        Input<Character> charInput = Input.of(input);

        List<String> words = wordParser.streamParse(charInput)
            .parallel()
            .map(String::toUpperCase)
            .toList();

        assertEquals(5, words.size());
        assertTrue(words.contains("ONE"));
        assertTrue(words.contains("TWO"));
        assertTrue(words.contains("THREE"));
        assertTrue(words.contains("FOUR"));
        assertTrue(words.contains("FIVE"));
    }
}
