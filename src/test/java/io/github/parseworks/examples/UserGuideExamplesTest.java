package io.github.parseworks.examples;

import io.github.parseworks.Input;
import io.github.parseworks.Lists;
import io.github.parseworks.Parser;
import io.github.parseworks.Result;
import io.github.parseworks.parsers.Lexical;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;

import static io.github.parseworks.parsers.Combinators.oneOf;
import static io.github.parseworks.parsers.Lexical.trim;
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
        Parser<Character, String> helloParser = Lexical.string("hello");

        // Parse the input "hello world"
        Result<Character, String> result = helloParser.parse(Input.of("hello world"));

        // Check if parsing succeeded
        assertTrue(result.matches());
        assertEquals("hello", result.value());
        assertEquals(' ', result.input().current()); // Check the current character of the remaining input

        // Test the handle method
        String message = result.handle(
            success -> "Successfully parsed: " + success.value(),
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
        Parser<Character, String> helloParser = Lexical.string("hello");

        // Parser for the word "world"
        Parser<Character, String> worldParser = Lexical.string("world");

        // Parser for whitespace
        Parser<Character, String> whitespaceParser = Lexical.chr(' ').oneOrMore().as("");

        // Parser for "hello world" that ignores whitespace
        Parser<Character, String> cleanerParser = helloParser
            .thenSkip(whitespaceParser)
            .then(worldParser)
            .map(hello -> world -> hello + " " + world);

        Result<Character, String> result = cleanerParser.parse(Input.of("hello world"));
        assertTrue(result.matches());
        assertEquals("hello world", result.value());
    }

    /**
     * Test for Tutorial 3: Parsing Structured Data
     */
    @Test
    public void testTutorial3() {
        // Parser for keys (alphanumeric strings)
        Parser<Character, String> keyParser = Lexical.regex("[a-zA-Z0-9]+");

        // Parser for the equals sign
        Parser<Character, Character> equalsParser = Lexical.chr('=');

        // Parser for values (any string until end of line)
        Parser<Character, String> valueParser = Lexical.regex("[^\\n]*");

        // Parser for a key-value pair
        Parser<Character, KeyValue> keyValueParser = keyParser
            .thenSkip(equalsParser)
            .then(valueParser)
            .map(key -> value -> new KeyValue(key, value));

        // Parser for multiple key-value pairs separated by newlines
        Parser<Character, List<KeyValue>> configParser = keyValueParser
            .oneOrMoreSeparatedBy(Lexical.chr('\n'));

        // Parse a configuration file
        String config = "server=localhost\nport=8080\nuser=admin";
        Result<Character, List<KeyValue>> result = configParser.parse(Input.of(config));

        assertTrue(result.matches());
        List<KeyValue> keyValues = result.value();
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
        Parser<Character, String> keyParser = Lexical.regex("[a-zA-Z0-9]+").expecting("key");

        // Parser for the equal sign
        Parser<Character, Character> equalsParser = Lexical.chr('=').expecting("equals");

        // Parser for values (any string until the end of line)
        Parser<Character, String> valueParser = Lexical.chr(c -> c != '\n' && c != ',' && c != '}')
            .oneOrMore()
            .map(Lists::join)
            .expecting("value");

        // Parser for a key-value pair
        Parser<Character, KeyValue> keyValueParser = keyParser
            .thenSkip(equalsParser)
            .then(valueParser)
            .map(key -> value -> new KeyValue(key, value))
            .expecting("key-value pair");

        // Parser for a JSON-like object
        Parser<Character, Map<String, String>> objectParser = Lexical.chr('{')
            .skipThen(
                keyValueParser.oneOrMoreSeparatedBy(Lexical.string(","))
            )
            .thenSkip(Lexical.chr('}'))
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
        assertTrue(validResult.matches());
        Map<String, String> map = validResult.value();
        assertEquals(2, map.size());
        assertEquals("John", map.get("name"));
        assertEquals("30", map.get("age"));

        // Test with invalid input
        String invalidInput = "{name=John,age="; // Missing closing brace
        Result<Character, Map<String, String>> invalidResult = objectParser.parse(Input.of(invalidInput));
        assertFalse(invalidResult.matches());
        invalidResult.errorOptional().ifPresent(System.out::println);
    }

    /**
     * Validation for Tutorial 4 Step 5: Label failures with expecting(...)
     * Mirrors the user-guide example to ensure it stays correct.
     */
    @Test
    public void testTutorial4_ExpectingExample() {
        // Suppose an identifier is a letter followed by zero or more alphanumerics
        // Use a regex-based parser for a concise identifier definition
        Parser<Character, String> identifier =
            Lexical.regex("[A-Za-z][A-Za-z0-9]*")
                .expecting("identifier");

        Result<Character, String> r = identifier.parse("123");
        assertTrue(!r.matches(), "Identifier should fail when starting with a digit");
        String msg = r.error();
        assertTrue(msg.contains("expected identifier"),
                () -> "Error should mention relabeled expectation, but was:\n" + msg);
    }

    /**
     * Test for Tutorial 5: Creating a Calculator Parser
     */
    @Test
    public void testTutorial5() {

        // Parser for numbers
        Parser<Character, Integer> number = Lexical.regex("[0-9]+")
            .map(Integer::parseInt);

        // Create references for recursive parsers
        Parser<Character, Integer> expr = Parser.ref();
        Parser<Character, Integer> term = Parser.ref();
        Parser<Character, Integer> factor = Parser.ref();

        // Factor can be a number or an expression in parentheses
        Parser<Character, Integer> parenFactor = Lexical.chr('(')
            .skipThen(trim(expr))
            .thenSkip(Lexical.chr(')')); // Using trim from Lexical

        factor.set(
            trim(oneOf(number, parenFactor))
        );

        // Parser for multiplication operator
        Parser<Character, BinaryOperator<Integer>> mulOp = trim(Lexical.chr('*'))
            .as((a, b) -> a * b);

        // Parser for division operator
        Parser<Character, BinaryOperator<Integer>> divOp = trim(Lexical.chr('/'))
            .as((a, b) -> a / b);

        // Term handles multiplication and division
        term.set(
            factor.chainLeftZeroOrMore(oneOf(mulOp, divOp), 0)
        );

        // Parser for addition operator
        Parser<Character, BinaryOperator<Integer>> addOp = trim(Lexical.chr('+'))
            .as(Integer::sum);

        // Parser for subtraction operator
        Parser<Character, BinaryOperator<Integer>> subOp = trim(Lexical.chr('-'))
            .as((a, b) -> a - b);

        // Expression handles addition and subtraction
        expr.set(
            term.chainLeftZeroOrMore(oneOf(addOp, subOp), 0)
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
            assertTrue(result.matches(), "Failed to parse: " + expressions[i]);
            assertEquals(expectedResults[i], result.value(), "Incorrect result for: " + expressions[i]);
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
