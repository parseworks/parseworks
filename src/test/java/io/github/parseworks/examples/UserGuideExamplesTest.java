package io.github.parseworks.examples;

import io.github.parseworks.FList;
import io.github.parseworks.Input;
import io.github.parseworks.Parser;
import io.github.parseworks.Result;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.BinaryOperator;

import static io.github.parseworks.Combinators.*;
import static io.github.parseworks.TextParsers.trim;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that the examples in the user-guide.md work correctly.
 */
public class UserGuideExamplesTest {

    /**
     * Test for Tutorial 1: Creating Your First Parser
     */
    @Test
    public void testTutorial1() {
        // Create a parser that recognizes the string "hello"
        Parser<Character, String> helloParser = string("hello");

        // Parse the input "hello world"
        Result<Character, String> result = helloParser.parse(Input.of("hello world"));

        // Check if parsing succeeded
        assertTrue(result.isSuccess());
        assertEquals("hello", result.get());
        assertEquals(' ', result.next().current()); // Check the current character of the remaining input

        // Test the handle method
        String message = result.handle(
            success -> "Successfully parsed: " + success.get(),
            failure -> "Parsing failed: " + failure.error()
        );
        assertEquals("Successfully parsed: hello", message);
    }

    /**
     * Test for Tutorial 2: Combining Parsers
     */
    @Test
    public void testTutorial2() {
        // Parser for the word "hello"
        Parser<Character, String> helloParser = string("hello");

        // Parser for the word "world"
        Parser<Character, String> worldParser = string("world");

        // Parser for whitespace
        Parser<Character, String> whitespaceParser = chr(' ').many().as("");

        // Parser for "hello world" that ignores whitespace
        Parser<Character, String> cleanerParser = helloParser
            .thenSkip(whitespaceParser)
            .then(worldParser)
            .map(hello -> world -> hello + " " + world);

        Result<Character, String> result = cleanerParser.parse(Input.of("hello world"));
        assertTrue(result.isSuccess());
        assertEquals("hello world", result.get());
    }

    /**
     * Test for Tutorial 3: Parsing Structured Data
     */
    @Test
    public void testTutorial3() {
        // Parser for keys (alphanumeric strings)
        Parser<Character, String> keyParser = regex("[a-zA-Z0-9]+");

        // Parser for the equals sign
        Parser<Character, Character> equalsParser = chr('=');

        // Parser for values (any string until end of line)
        Parser<Character, String> valueParser = regex("[^\\n]*");

        // Parser for a key-value pair
        Parser<Character, KeyValue> keyValueParser = keyParser
            .thenSkip(equalsParser)
            .then(valueParser)
            .map(key -> value -> new KeyValue(key, value));

        // Parser for multiple key-value pairs separated by newlines
        Parser<Character, FList<KeyValue>> configParser = keyValueParser
            .manySeparatedBy(chr('\n'));

        // Parse a configuration file
        String config = "server=localhost\nport=8080\nuser=admin";
        Result<Character, FList<KeyValue>> result = configParser.parse(Input.of(config));

        assertTrue(result.isSuccess());
        FList<KeyValue> keyValues = result.get();
        assertEquals(3, keyValues.size());
        assertEquals("server", keyValues.get(0).getKey());
        assertEquals("localhost", keyValues.get(0).getValue());
        assertEquals("port", keyValues.get(1).getKey());
        assertEquals("8080", keyValues.get(1).getValue());
        assertEquals("user", keyValues.get(2).getKey());
        assertEquals("admin", keyValues.get(2).getValue());
    }

    /**
     * Test for Tutorial 4: Error Handling
     */
    @Test
    public void testTutorial4() {
        // Parser for keys (alphanumeric strings)
        Parser<Character, String> keyParser = regex("[a-zA-Z0-9]+");

        // Parser for the equals sign
        Parser<Character, Character> equalsParser = chr('=');

        // Parser for values (any string until end of line)
        Parser<Character, String> valueParser = regex("[^\\n,}]*");

        // Parser for a key-value pair
        Parser<Character, KeyValue> keyValueParser = keyParser
            .thenSkip(equalsParser)
            .then(valueParser)
            .map(key -> value -> new KeyValue(key, value));

        // Parser for a JSON-like object
        Parser<Character, Map<String, String>> objectParser = chr('{')
            .skipThen(
                keyValueParser.manySeparatedBy(string(","))
            )
            .thenSkip(chr('}'))
            .map(pairs -> {
                Map<String, String> map = new HashMap<>();
                for (KeyValue kv : pairs) {
                    map.put(kv.getKey(), kv.getValue());
                }
                return map;
            });

        // Test with valid input
        String validInput = "{name=John,age=30}";
        Result<Character, Map<String, String>> validResult = objectParser.parse(Input.of(validInput));
        assertTrue(validResult.isSuccess());
        Map<String, String> map = validResult.get();
        assertEquals(2, map.size());
        assertEquals("John", map.get("name"));
        assertEquals("30", map.get("age"));

        // Test with invalid input
        String invalidInput = "{name=John,age=30"; // Missing closing brace
        Result<Character, Map<String, String>> invalidResult = objectParser.parse(Input.of(invalidInput));
        assertTrue(invalidResult.isError());
    }

    /**
     * Test for Tutorial 5: Creating a Calculator Parser
     */
    @Test
    public void testTutorial5() {

        // Parser for numbers
        Parser<Character, Integer> number = regex("[0-9]+")
            .map(Integer::parseInt);

        // Create references for recursive parsers
        Parser<Character, Integer> expr = Parser.ref();
        Parser<Character, Integer> term = Parser.ref();
        Parser<Character, Integer> factor = Parser.ref();

        // Factor can be a number or an expression in parentheses
        Parser<Character, Integer> parenFactor = chr('(')
            .skipThen(trim(expr))
            .thenSkip(chr(')')); // Using trim from TextParsers

        factor.set(
            trim(oneOf(number, parenFactor))
        );

        // Parser for multiplication operator
        Parser<Character, BinaryOperator<Integer>> mulOp = trim(chr('*'))
            .as((a, b) -> a * b);

        // Parser for division operator
        Parser<Character, BinaryOperator<Integer>> divOp = trim(chr('/'))
            .as((a, b) -> a / b);

        // Term handles multiplication and division
        term.set(
            factor.chainLeft(oneOf(mulOp, divOp), 0)
        );

        // Parser for addition operator
        Parser<Character, BinaryOperator<Integer>> addOp = trim(chr('+'))
            .as(Integer::sum);

        // Parser for subtraction operator
        Parser<Character, BinaryOperator<Integer>> subOp = trim(chr('-'))
            .as((a, b) -> a - b);

        // Expression handles addition and subtraction
        expr.set(
            term.chainLeft(oneOf(addOp, subOp), 0)
        );

        // Parse and evaluate expressions
        String[] expressions = {
            "2 + 3",
            "2 * 3 + 4",
            "2 + 3 * 4",
            "(2 + 3) * 4",
            "8 / 4 / 2"
        };

        int[] expectedResults = {
            5,    // 2 + 3 = 5
            10,   // 2 * 3 + 4 = 10
            14,   // 2 + 3 * 4 = 14
            20,   // (2 + 3) * 4 = 20
            1     // 8 / 4 / 2 = 1
        };

        for (int i = 0; i < expressions.length; i++) {
            Result<Character, Integer> result = expr.parseAll(Input.of(expressions[i]));
            assertTrue(result.isSuccess(), "Failed to parse: " + expressions[i]);
            assertEquals(expectedResults[i], result.get(), "Incorrect result for: " + expressions[i]);
        }
    }

    /**
     * Simple KeyValue class for testing
     */
    static class KeyValue {
        private final String key;
        private final String value;

        public KeyValue(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() { return key; }
        public String getValue() { return value; }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }
}
