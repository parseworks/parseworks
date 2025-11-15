package io.github.parseworks;

import org.junit.jupiter.api.Test;

import static io.github.parseworks.parsers.Lexical.regex;
import static io.github.parseworks.parsers.Lexical.trim;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApplyBuilderTest {

    // Test two-parser combination
    @Test
    public void testTwoParserCombination() {
        Parser<Character, String> p1 = stringParser("hello");
        Parser<Character, String> p2 = stringParser("world");
        
        Parser<Character, String> combined = p1.then(p2)
            .map((s1, s2) -> s1 + " " + s2);
        
        Result<Character, String> result = combined.parseAll("helloworld");
        assertTrue(result.matches());
        assertEquals("hello world", result.value());
    }
    
    // Test three-parser combination
    @Test
    public void testThreeParserCombination() {
        Parser<Character, String> p1 = stringParser("a");
        Parser<Character, String> p2 = stringParser("b");
        Parser<Character, String> p3 = stringParser("c");
        
        Parser<Character, String> combined = p1.then(p2).then(p3)
            .map((a, b, c) -> a + b + c);
        
        Result<Character, String> result = combined.parseAll("abc");
        assertTrue(result.matches());
        assertEquals("abc", result.value());
    }
    
    // Test four-parser combination
    @Test
    public void testFourParserCombination() {
        Parser<Character, Integer> p1 = intParser(1);
        Parser<Character, Integer> p2 = intParser(2);
        Parser<Character, Integer> p3 = intParser(3);
        Parser<Character, Integer> p4 = intParser(4);
        
        Parser<Character, Integer> combined = p1.then(p2).then(p3).then(p4)
            .map((a, b, c, d) -> a + b + c + d);
        
        Result<Character, Integer> result = combined.parse("1234");
        assertTrue(result.matches());
        assertEquals(10, result.value()); // 1+2+3+4 = 10
    }
    
    // Test five-parser combination
    @Test
    public void testFiveParserCombination() {
        Parser<Character, Integer> p1 = intParser(1);
        Parser<Character, Integer> p2 = intParser(2);
        Parser<Character, Integer> p3 = intParser(3);
        Parser<Character, Integer> p4 = intParser(4);
        Parser<Character, Integer> p5 = intParser(5);
        
        Parser<Character, Integer> combined = p1.then(p2).then(p3).then(p4).then(p5)
            .map((a, b, c, d, e) -> a + b + c + d + e);
        
        Result<Character, Integer> result = combined.parse("12345");
        assertTrue(result.matches());
        assertEquals(15, result.value()); // 1+2+3+4+5 = 15
    }
    
    // Test six-parser combination
    @Test
    public void testSixParserCombination() {
        Parser<Character, Integer> p1 = intParser(1);
        Parser<Character, Integer> p2 = intParser(2);
        Parser<Character, Integer> p3 = intParser(3);
        Parser<Character, Integer> p4 = intParser(4);
        Parser<Character, Integer> p5 = intParser(5);
        Parser<Character, Integer> p6 = intParser(6);
        
        Parser<Character, Integer> combined = p1.then(p2).then(p3).then(p4).then(p5).then(p6)
            .map((a, b, c, d, e, f) -> a + b + c + d + e + f);
        
        Result<Character, Integer> result = combined.parse("123456");
        assertTrue(result.matches());
        assertEquals(21, result.value()); // Sum = 21
    }
    
    // Test seven-parser combination
    @Test
    public void testSevenParserCombination() {
        Parser<Character, String> p1 = stringParser("a");
        Parser<Character, String> p2 = stringParser("b");
        Parser<Character, String> p3 = stringParser("c");
        Parser<Character, String> p4 = stringParser("d");
        Parser<Character, String> p5 = stringParser("e");
        Parser<Character, String> p6 = stringParser("f");
        Parser<Character, String> p7 = stringParser("g");
        
        Parser<Character, String> combined = p1.then(p2).then(p3).then(p4).then(p5).then(p6).then(p7)
            .map((a, b, c, d, e, f, g) -> a + b + c + d + e + f + g);
        
        Result<Character, String> result = combined.parse("abcdefg");
        assertTrue(result.matches());
        assertEquals("abcdefg", result.value());
    }
    
    // Test eight-parser combination
    @Test
    public void testEightParserCombination() {
        Parser<Character, String> p1 = stringParser("1");
        Parser<Character, String> p2 = stringParser("2");
        Parser<Character, String> p3 = stringParser("3");
        Parser<Character, String> p4 = stringParser("4");
        Parser<Character, String> p5 = stringParser("5");
        Parser<Character, String> p6 = stringParser("6");
        Parser<Character, String> p7 = stringParser("7");
        Parser<Character, String> p8 = stringParser("8");
        
        Parser<Character, String> combined = p1.then(p2).then(p3).then(p4).then(p5).then(p6).then(p7).then(p8)
            .map((a, b, c, d, e, f, g, h) -> a + b + c + d + e + f + g + h);
        
        Result<Character, String> result = combined.parse("12345678");
        assertTrue(result.matches());
        assertEquals("12345678", result.value());
    }
    
    // Test with mixed types
    @Test
    public void testMixedTypesCombination() {
        Parser<Character, String> p1 = stringParser("user");
        Parser<Character, Character> p2 = charParser(':');
        Parser<Character, Integer> p3 = intParser(42);
        
        Parser<Character, String> combined = p1.then(p2).then(p3)
            .map((user, colon, id) -> user + id);
        
        Result<Character, String> result = combined.parse("user:42");
        assertTrue(result.matches());
        assertEquals("user42", result.value());
    }
    
    // Test parsing failure
    @Test
    public void testParsingFailure() {
        Parser<Character, String> p1 = stringParser("hello");
        Parser<Character, String> p2 = stringParser("world");
        
        Parser<Character, String> combined = p1.then(p2)
            .map((s1, s2) -> s1 + " " + s2);
        
        Result<Character, String> result = combined.parse("helloplanet");
        assertTrue(!result.matches());
    }
    
    // Test with incomplete input
    @Test
    public void testIncompleteInput() {
        Parser<Character, String> p1 = stringParser("hello");
        Parser<Character, String> p2 = stringParser("world");
        
        Parser<Character, String> combined = p1.then(p2)
            .map((s1, s2) -> s1 + " " + s2);
        
        Result<Character, String> result = combined.parse("hello");
        assertTrue(!result.matches());
    }
    
    // Test real-world scenario: parsing a simple assignment statement
    @Test
    public void testAssignmentParsing() {
        Parser<Character, String> identifier = regex("[a-zA-Z][a-zA-Z0-9]*");
        Parser<Character, Character> equals = charParser('=');
        Parser<Character, Integer> number = regexIntParser("[0-9]+");
        Parser<Character, Character> semicolon = charParser(';');
        
        // Parse "x = 42;"
        Parser<Character, Assignment> assignmentParser = trim(identifier).then(equals)
            .then(trim(number)).then(semicolon)
            .map((name, eq, value, semi) -> new Assignment(name, value));
        
        Result<Character, Assignment> result = assignmentParser.parseAll("myVar = 42;");
        assertTrue(result.matches());
        Assignment assignment = result.value();
        assertEquals("myVar", assignment.name);
        assertEquals(42, assignment.value);
    }
    
    // Helper class for assignment test
    private static class Assignment {
        final String name;
        final int value;
        
        Assignment(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }
    
    // Helper parser methods
    private Parser<Character, Character> charParser(char expected) {
        return new Parser<>(input -> {
            if (input.isEof() || input.current() != expected) {
                return Result.failure(input, String.valueOf(expected));
            }
            return Result.success(input.next(), expected);
        });
    }
    
    private Parser<Character, String> stringParser(String expected) {
        return new Parser<>(input -> {
            Input<Character> current = input;
            for (int i = 0; i < expected.length(); i++) {
                if (current.isEof() || current.current() != expected.charAt(i)) {
                    return Result.failure(input, expected);
                }
                current = current.next();
            }
            return Result.success(current, expected);
        });
    }
    
    private Parser<Character, Integer> intParser(int value) {
        return stringParser(Integer.toString(value)).map(s -> value);
    }

    
    private Parser<Character, Integer> regexIntParser(String pattern) {
        return regex(pattern).map(Integer::parseInt);
    }
}