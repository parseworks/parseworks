package io.github.parseworks;

import io.github.parseworks.parsers.Combinators;
import io.github.parseworks.parsers.Lexical;
import io.github.parseworks.parsers.Numeric;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.github.parseworks.parsers.Combinators.attempt;
import static io.github.parseworks.parsers.Combinators.oneOf;
import static io.github.parseworks.parsers.Lexical.chr;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParserPerformanceTest {

    @Test
    public void testRepetitionPerformance() {
        // Create a parser that matches letters followed by numbers
        Parser<Character, List<Character>> letterParser = chr(Character::isLetter).zeroOrMore();
        Parser<Character, List<Character>> digitParser = chr(Character::isDigit).zeroOrMore();
        
        // Generate test input with oneOrMore repetitions
        StringBuilder input = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            input.append("a").append(i % 10);
        }
        
        long startTime = System.nanoTime();

        var parser = letterParser.then(digitParser).map(Lists::appendAll);

        // Parse the input multiple times to measure performance
        for (int i = 0; i < 5; i++) {
            Result<Character, List<Character>> result = parser.parse(input.toString());
            assertTrue(result.matches(), "Parsing should succeed");
        }
        
        long duration = System.nanoTime() - startTime;
        System.out.println("Repetition parsing took: " + TimeUnit.NANOSECONDS.toMillis(duration) + "ms");
        
        // If you want to test against a baseline, you can use a threshold
        assertTrue(TimeUnit.NANOSECONDS.toMillis(duration) < 5000, 
                "Parsing should complete within reasonable time");
    }

    @Test
    public void testLargeInputPerformance() {
        // Whitespace and separators
        Parser<Character, List<Character>> ws = chr(Character::isWhitespace).zeroOrMore();
        Parser<Character, Character> commaOnly = chr(',');
        Parser<Character, Character> comma = ws.skipThen(commaOnly).thenSkip(ws); // optional spaces around comma
        Parser<Character, Character> eol = chr('\n');

        // QUOTED FIELD: "..." with doubled quotes inside
        Parser<Character, String> escapedQuote = Lexical.string("\\\"");
        Parser<Character, Character> notQuote = chr(c -> c != '"');
        Parser<Character, String> quotedChunk = Combinators.oneOf(
            escapedQuote.map(s -> "\\\""),            // "" -> "
            notQuote.map(Object::toString)             // any non-quote char
        ).oneOrMore().map(Lists::join);

        Parser<Character, String> quotedField =
            chr('"').skipThen(quotedChunk).thenSkip(chr('"'));

        // UNQUOTED FIELD: runs until comma or EOL; use conditional to ensure we don't start with a quote
        Parser<Character, String> unquotedFieldCore =
            chr(c -> c != ',' && c != '\n' && c != '\r')
                .oneOrMore()
                .map(Lists::join);

        // Conditional: only allow the unquoted variant if the next char is NOT a quote
        Parser<Character, String> unquotedField = unquotedFieldCore.onlyIf(Combinators.not(chr('\"')));

        // TYPED VALUES via oneOf: boolean, null, number OR fallback to quoted/unquoted text
        Parser<Character, String> boolToken = oneOf(
            Lexical.string("true"),
            Lexical.string("false")
        );

        Parser<Character, String> nullToken = oneOf(
            Lexical.string("NULL"),
            Lexical.string("null")
        );

        // A lenient number (int or decimal); keep as string for uniform result type
        Parser<Character, String> numberToken = Numeric.doubleValue.map(String::valueOf);

        // Prefer more specific tokens first; then quoted; then raw unquoted
        Parser<Character, String> field = oneOf(
            boolToken.expecting("boolean"),
            nullToken.expecting("null"),
            numberToken.expecting("number"),
            quotedField.expecting("quoted"),
            unquotedField.expecting("unquoted")
        );

        // Row: fields separated by commas (with optional surrounding whitespace)
        Parser<Character, List<String>> row = field.oneOrMoreSeparatedBy(attempt(comma));

        // CSV: rows separated by newlines
        Parser<Character, List<List<String>>> csvParser = row.oneOrMoreSeparatedBy(eol);

        // Generate a large CSV-like input
        String input = getInput();

        long startTime = System.nanoTime();
        Result<Character, List<List<String>>> result = csvParser.parse(input);
        long duration = System.nanoTime() - startTime;
        if (!result.matches()){
            System.out.println(result.error());
        }
        assertTrue(result.matches(), "Parsing should succeed");
        assertEquals(500_000, result.value().size(), "Should parse all lines");
        System.out.println("Parsed " + result.value().size() + " lines successfully");
        System.out.println("String size: " + String.format("%.2f MB", input.length() / 1048576f));
        System.out.println("Large input parsing took: " + TimeUnit.NANOSECONDS.toMillis(duration) + "ms");
        assertTrue(TimeUnit.NANOSECONDS.toMillis(duration) < 100_000,
            "Large input parsing should be efficient");
    }

    private static String getInput() {
        StringBuilder input = new StringBuilder();
        for (int i = 0; i < 500_000; i++) {
            for (int j = 0; j < 10; j++) {
                switch (j % 5) {
                    case 0 -> input.append("\"field\\\"").append(j).append("\"");
                    case 1 -> input.append("true");
                    case 2 -> input.append("-123.45");
                    case 3 -> input.append("NULL");
                    default -> input.append("plain_").append(j);
                }
                if (j < 9) input.append(", ");
            }
            // Only append newline if it's not the last row
            if (i < 499_999) {
                input.append("\n");
            }
        }
        return input.toString();
    }

    @Test()
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
            assertTrue(result.matches());
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