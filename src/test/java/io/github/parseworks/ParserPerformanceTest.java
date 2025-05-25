package io.github.parseworks;

import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static io.github.parseworks.Combinators.*;
import static org.junit.jupiter.api.Assertions.*;

public class ParserPerformanceTest {

    @Test
    public void testRepetitionPerformance() {
        // Create a parser that matches letters followed by numbers
        Parser<Character, FList<Character>> letterParser = chr(Character::isLetter).zeroOrMany();
        Parser<Character, FList<Character>> digitParser = chr(Character::isDigit).zeroOrMany();
        
        // Generate test input with many repetitions
        StringBuilder input = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            input.append("a").append(i % 10);
        }
        
        long startTime = System.nanoTime();

        var parser = letterParser.then(digitParser).map(a -> a::appendAll);

        // Parse the input multiple times to measure performance
        for (int i = 0; i < 5; i++) {
            Result<Character, FList<Character>> result = parser.parse(input.toString());
            assertTrue(result.isSuccess(), "Parsing should succeed");
        }
        
        long duration = System.nanoTime() - startTime;
        System.out.println("Repetition parsing took: " + TimeUnit.NANOSECONDS.toMillis(duration) + "ms");
        
        // If you want to test against a baseline, you can use a threshold
        assertTrue(TimeUnit.NANOSECONDS.toMillis(duration) < 5000, 
                "Parsing should complete within reasonable time");
    }

    @Test
    public void testInfiniteLoopDetectionPerformance() {
        // Create a complex parser with many branches that might trigger loop detection
        Parser<Character, String> word = chr(Character::isLetter).many().map(chars -> 
                chars.stream().map(String::valueOf).reduce("", String::concat));

        Parser<Character, String> complexParser = Parser.ref();

        Parser<Character, String> nested = word
                .or(chr('(').skipThen(word).thenSkip(chr(')')))
                .or(chr('[').skipThen(word).thenSkip(chr(']')))
                .or(chr('{').skipThen(word).thenSkip(chr('}')))
                .or(complexParser);

        complexParser.set(nested);
        
        // Generate test input with many nested structures
        StringBuilder input = new StringBuilder();
        StringBuilder end = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            input.append("((word)").append("[test]").append("{data}");
            end.append(")");
        }
        input.append(end);
        
        long startTime = System.nanoTime();
        
        // Parse the input multiple times
        for (int i = 0; i < 5; i++) {
            Result<Character, FList<String>> result = complexParser.many().parse(input.toString());
            assertTrue(result.isError(), "Parsing should fail");
        }
        
        long duration = System.nanoTime() - startTime;
        System.out.println("Loop detection parsing took: " + TimeUnit.NANOSECONDS.toMillis(duration) + "ms");
        
        assertTrue(TimeUnit.NANOSECONDS.toMillis(duration) < 3000, 
                "Loop detection should be efficient");
    }

    @Test
    public void testLargeInputPerformance() {
        // Create a parser for CSV-like data
        Parser<Character, FList<FList<String>>> csvParser = 
                chr(Character::isLetterOrDigit).many()
                .map(chars -> chars.stream().map(String::valueOf).reduce("", String::concat))
                .separatedByMany(chr(','))
                .separatedByMany(chr('\n'));
        
        // Generate a large CSV-like input
        StringBuilder input = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            for (int j = 0; j < 10; j++) {
                input.append("field").append(j);
                if (j < 9) input.append(",");
            }
            input.append("\n");
        }
        
        long startTime = System.nanoTime();
        Result<Character, FList<FList<String>>> result = csvParser.parse(input.toString());
        long duration = System.nanoTime() - startTime;
        
        assertTrue(result.isSuccess(), "Parsing should succeed");
        assertEquals(1000, result.get().size(), "Should parse all lines");
        
        System.out.println("Large input parsing took: " + TimeUnit.NANOSECONDS.toMillis(duration) + "ms");
        assertTrue(TimeUnit.NANOSECONDS.toMillis(duration) < 10000, 
                "Large input parsing should be efficient");
    }

    @Test
    public void testStringCachingPerformance() {
        // Create a simple parser
        Parser<Character, Character> letterParser = chr('a');
        
        // Parse the same string multiple times to test caching
        String input = "a";
        
        long startTime = System.nanoTime();
        
        // First parse might be slower
        Result<Character, Character> firstResult = letterParser.parse(input);
        long firstParseDuration = System.nanoTime() - startTime;
        
        // Subsequent parses should be faster if caching works
        startTime = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            Result<Character, Character> result = letterParser.parse(input);
            assertTrue(result.isSuccess());
        }
        long subsequentParsesDuration = System.nanoTime() - startTime;
        
        System.out.println("First parse took: " + TimeUnit.NANOSECONDS.toMicros(firstParseDuration) + "μs");
        System.out.println("1000 subsequent parses took: " + TimeUnit.NANOSECONDS.toMillis(subsequentParsesDuration) + "ms");
        
        // Calculate average time per parse for subsequent parses
        double avgTimePerParse = (double)subsequentParsesDuration / 1000;
        System.out.println("Average time per parse: " + TimeUnit.NANOSECONDS.toMicros((long)avgTimePerParse) + "μs");
        
        // If caching is effective, the average time should be significantly lower
        assertTrue(avgTimePerParse < firstParseDuration, 
                "Subsequent parses should be faster due to caching");
    }
}