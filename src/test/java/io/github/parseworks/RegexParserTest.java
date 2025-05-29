package io.github.parseworks;

import org.junit.jupiter.api.Test;

import static io.github.parseworks.Combinators.regex;
import static org.junit.jupiter.api.Assertions.*;

public class RegexParserTest {

    @Test
    public void testEmptyInput() {
        Parser<Character, String> parser = regex(".*");
        Result<Character, String> result = parser.parse("");
        assertTrue(result.isSuccess());
        assertEquals("", result.get());
        
        // regexGreedy with empty input
        parser = regex(".*");
        result = parser.parse("");
        assertTrue(result.isSuccess());
        assertEquals("", result.get());
    }
    
    @Test
    public void testNoMatch() {
        Parser<Character, String> parser = regex("\\d+");
        Result<Character, String> result = parser.parse("abc");
        assertTrue(result.isError());
        
        parser = regex("\\d+");
        result = parser.parse("abc");
        assertTrue(result.isError());
    }
    
    @Test
    public void testRegexVsGreedyBehavior() {
        // Standard regex stops at first complete match
        Parser<Character, String> standard = regex("[a-z]+\\.[a-z]+");
        Result<Character, String> result1 = standard.parse("example.com.org");
        assertTrue(result1.isSuccess());
        assertEquals("example.com", result1.get());
        
        // Greedy regex finds longest possible match
        Parser<Character, String> greedy = regex("[a-z]+\\.[a-z\\.]+");
        Result<Character, String> result2 = greedy.parse("example.com.org");
        assertTrue(result2.isSuccess());
        assertEquals("example.com.org", result2.get());
    }
    
    @Test
    public void testComplexPatterns() {
        // Email-like pattern
        Parser<Character, String> emailParser = regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Result<Character, String> result = emailParser.parse("user@example.com and more");
        assertTrue(result.isSuccess());
        assertEquals("user@example.com", result.get());

        result = emailParser.parse("user@example.co.uk and more");
        assertTrue(result.isSuccess());
        assertEquals("user@example.co.uk", result.get());
    }
    
    @Test
    public void testAnchors() {
        // Start anchor
        Parser<Character, String> parser = regex("^abc");
        Result<Character, String> result = parser.parse("abcdef");
        assertTrue(result.isSuccess());
        assertEquals("abc", result.get());
        
        // End anchor
        parser = regex("abc$");
        result = parser.parse("abc");
        assertTrue(result.isSuccess());
        assertEquals("abc", result.get());
        
        // End anchor not matching
        parser = regex("abc$");
        result = parser.parse("abcdef");
        // Should fail as "abc" is not at the end
        // however it won't as regex does not enforce end of string
        assertTrue(result.isError());
    }
    
    @Test
    public void testQuantifiers() {
        // Test greedy vs non-greedy quantifiers
        Parser<Character, String> greedyParser = regex("a.*b");
        Result<Character, String> result = greedyParser.parse("axbycb");
        assertTrue(result.isSuccess());
        assertEquals("axbycb", result.get());
        
        Parser<Character, String> nonGreedyParser = regex("a.*?b");
        result = nonGreedyParser.parse("axbycb");
        assertTrue(result.isSuccess());
        assertEquals("axb", result.get());
    }
    
    @Test
    public void testGroupsAndAlternatives() {
        // Capturing groups
        Parser<Character, String> parser = regex("(ab)|(cd)");
        Result<Character, String> result = parser.parse("abcd");
        assertTrue(result.isSuccess());
        assertEquals("ab", result.get());

        parser = regex("abcd|ab");
        result = parser.parse("abcdef");
        assertTrue(result.isSuccess());
        assertEquals("abcd", result.get()); // Longest match
    }
    
    @Test
    public void testLongInput() {
        // Generate a long string
        StringBuilder longInput = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            longInput.append(i % 10);
        }
        
        // Test with pattern that matches a prefix
        Parser<Character, String> parser = regex("\\d{500}");
        Result<Character, String> result = parser.parse(longInput.toString());
        assertTrue(result.isSuccess());
        assertEquals(500, result.get().length());
        
        // Test with greedy pattern near the limit
        parser = regex("\\d{900}");
        result = parser.parse(longInput.toString());
        assertTrue(result.isSuccess());
        assertEquals(900, result.get().length());
    }
    
    @Test
    public void testSpecialCharacters() {
        // Unicode characters
        Parser<Character, String> parser = regex("\\p{L}+");
        Result<Character, String> result = parser.parse("αβγδε");
        assertTrue(result.isSuccess());
        assertEquals("αβγδε", result.get());
        
        // Escaping special regex characters
        parser = regex("\\[\\]\\{\\}\\(\\)\\.");
        result = parser.parse("[]{}().");
        assertTrue(result.isSuccess());
        assertEquals("[]{}().", result.get());
    }
    
    @Test
    public void testPartialMatches() {
        // Input with partial match at start
        Parser<Character, String> parser = regex("\\d+");
        Result<Character, String> result = parser.parse("123abc");
        assertTrue(result.isSuccess());
        assertEquals("123", result.get());
        
        // regexGreedy with partial match
        parser = regex("\\d+");
        result = parser.parse("123abc");
        assertTrue(result.isSuccess());
        assertEquals("123", result.get());
    }
    
    @Test
    public void testConsecutiveMatches() {
        // Multiple matches in sequence - regex only takes first
        String input = "123 456 789";
        Parser<Character, String> parser = regex("\\d+");
        Result<Character, String> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals("123", result.get());
    }
}