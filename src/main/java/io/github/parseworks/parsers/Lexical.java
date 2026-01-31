package io.github.parseworks.parsers;

import io.github.parseworks.*;
import io.github.parseworks.impl.inputs.CharArrayInput;
import io.github.parseworks.impl.result.Match;
import io.github.parseworks.impl.result.NoMatch;
import io.github.parseworks.impl.result.PartialMatch;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.parseworks.parsers.Combinators.satisfy;

/**
 * The {@code Lexical} class provides a comprehensive set of parsers for common text parsing tasks.
 * <p>
 * This utility class contains static parser definitions and methods for parsing:
 * <ul>
 *   <li>Numeric values (digits, integers, longs, doubles)</li>
 *   <li>Alphabetic and alphanumeric characters</li>
 *   <li>Whitespace</li>
 *   <li>Words and textual content</li>
 *   <li>Signs and special characters</li>
 * </ul>
 * <p>
 * The parsers in this class are designed to be composable building blocks for creating
 * more complex parsers. They follow functional programming principles, allowing them to
 * be combined using methods like {@code then()}, {@code or()}, {@code oneOrMore()}, etc.
 * <p>
 * Key parsers include:
 * <ul>
 *   <li>{@link Numeric#numeric} - Parses any digit character (0-9)</li>
 *   <li>{@link Numeric#nonZeroDigit} - Parses any non-zero digit character (1-9)</li>
 *   <li>{@link #alpha} - Parses any letter character</li>
 *   <li>{@link #alphaNumeric} - Parses any alphanumeric character</li>
 *   <li>{@link #whitespace} - Parses any whitespace character</li>
 *   <li>{@link Numeric#unsignedInteger} - Parses an unsigned integer</li>
 *   <li>{@link Numeric#integer} - Parses a signed integer</li>
 *   <li>{@link Numeric#doubleValue} - Parses a floating-point number</li>
 *   <li>{@link #word} - Parses a sequence of letters</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * // Parse an integer
 * Parser<Character, Integer> parser = Numeric.integer;
 * Result<Character, Integer> result = parser.parse("-123");
 * int value = result.value();  // -123
 *
 * // Parse a word
 * Parser<Character, String> wordParser = Lexical.word;
 * String word = wordParser.parse("Hello123").value();  // "Hello"
 *
 * // Combine parsers
 * Parser<Character, Pair<String, Integer>> nameAndAge =
 *     Lexical.word.skip(whitespace).then(Numeric.integer);
 * }</pre>
 *
 * This class works closely with {@link Combinators} which provides the fundamental
 * parser combinators used to build these text parsing utilities.
 *
 * @author jason bailey
 * @version $Id: $Id
 * @see Combinators
 * @see Parser
 */
public class Lexical {


    /**
     * Creates a parser that matches a single alphabetical character (a-z, A-Z).
     * <p>
     * The {@code alpha} parser recognizes any letter as defined by {@link Character#isLetter(char)},
     * which includes both uppercase and lowercase letters from the ASCII set and other Unicode
     * letter characters. The parsing process works as follows:
     * <ol>
     *   <li>Checks if the current input character is a letter</li>
     *   <li>If it is a letter, returns that character as the result and advances the input position</li>
     *   <li>If it is not a letter, the parser fails</li>
     * </ol>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Uses Java's {@link Character#isLetter(char)} method to determine what constitutes a letter</li>
     *   <li>Consumes exactly one character on success</li>
     *   <li>Fails if the input is empty or the current character is not a letter</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a single letter
     * Parser<Character, Character> letterParser = alpha();
     *
     * // Succeeds with 'a' for input "abc"
     * // Succeeds with 'Z' for input "Zed"
     * // Fails for input "123" (no letters)
     * // Fails for input "" (empty input)
     * }</pre>
     *
     * @see Numeric#numeric for parsing numeric digits
     * @see #alphaNumeric for parsing alphanumeric characters
     * @see Lexical#chr for parsing with a custom character predicate
     */
    public static final Parser<Character, Character> alpha = satisfy("<alphabet>", Character::isLetter);

    /**
     * Parses a single alphanumeric character.
     * This parser will succeed if the next input symbol is a letter or digit.
     */
    public static final Parser<Character, Character> alphaNumeric = satisfy( "<alphanumeric>", Character::isLetterOrDigit);

    /**
     * Creates a parser that trims whitespace from the input before and after parsing.
     * <p>
     * The {@code trim} parser applies the given parser to the input after skipping any leading
     * whitespace, and then skips any trailing whitespace from the result. This is useful for
     * ensuring that whitespace does not interfere with the parsing of the main content.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Uses {@link #skipWhitespace(Input)} to remove leading and trailing whitespace</li>
     *   <li>Returns a new parser that applies the original parser to the trimmed input</li>
     * </ul>
     *
     * @param parser the parser to apply after trimming whitespace
     * @param <A>    the type of the parsed value
     * @return a new parser that trims whitespace around the original parser
     */
    public static <A> Parser<Character,A>  trim(Parser<Character, A> parser) {
        return new Parser<>(in -> {
            Input<Character> trimmedInput = skipWhitespace(in);
            Result<Character, A> result = parser.apply(trimmedInput);
            if (result.matches()) {
                trimmedInput = skipWhitespace(result.input());
                return new Match<>(result.value(), trimmedInput);
            }
            return result;
        });
    }

    private static Input<Character> skipWhitespace(Input<Character> in) {
        while (!in.isEof() && Character.isWhitespace(in.current())) {
            in = in.next();
        }
        return in;
    }

    /**
     * Parses a sequence of letters and returns them as a string.
     * <p>
     * The {@code word} parser accepts one or more consecutive letter characters
     * (as defined by {@link Character#isLetter(char)}) and concatenates them into a
     * string result. If no letters are found, it returns an empty string.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Uses {@link #alpha} parser with {@code oneOrMore()} to collect letter characters</li>
     *   <li>Efficiently converts the character sequence to a string using StringBuilder</li>
     *   <li>Returns the concatenated string of all matched letters</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a word
     * Parser<Character, String> parser = word;
     * parser.parse("Hello123").value();  // Returns "Hello"
     * parser.parse("").value();          // Returns "" (empty string)
     *
     * // Use with other parsers
     * Parser<Character, String> identifier = word.then(alphaNumeric.oneOrMore())
     *     .map((w, rest) -> w + rest.stream()
     *         .map(String::valueOf)
     *         .collect(Collectors.joining()));
     * }</pre>
     *
     * @see #alpha for parsing single letter characters
     * @see #alphaNumeric for parsing alphanumeric characters
     */
    public static final Parser<Character, String> word = alpha.oneOrMore().map(Lists::join);

    public static final Parser<Character,Character> whitespace = satisfy("<whitespace>", Character::isWhitespace);

    /**
     * Fast scan that collects characters until the first occurrence of the given needle.
     * - Returns characters before the needle
     * - Does NOT consume the needle (chain with thenSkip(string(needle)) if needed)
     * - O(n) using indexOf-like scanning when input is a CharSequence/char[] backed input
     */
    public static Parser<Character, List<Character>> takeUntil(String needle) {
        Objects.requireNonNull(needle, "needle");
        if (needle.isEmpty()) {
            // Edge-case: empty delimiter – always succeed with empty list
            return new Parser<>(in -> new Match<>(Collections.emptyList(), in));
        }
        final char first = needle.charAt(0);

        return new Parser<>(in -> {

            // Fast path for CharSequenceInput
            if (in instanceof io.github.parseworks.impl.inputs.CharSequenceInput csi) {
                CharSequence data = csi.data();
                int from = csi.position();
                int idx = indexOf(data, needle, from);
                if (idx < 0) {
                    // Not found: consume to EOF
                    List<Character> out = toList(data, from, data.length());
                    return new Match<>(out, csi.skip(data.length() - from));
                } else {
                    List<Character> out = toList(data, from, idx);
                    return new Match<>(out, csi.skip(idx - from));
                }
            }

            // Fast path for CharArrayInput
            if (in instanceof CharArrayInput cai) {
                char[] data = cai.data();
                int from = cai.position();
                int idx = indexOf(data, needle, from);
                if (idx < 0) {
                    // Not found: consume to EOF
                    List<Character> out = toList(data, from, data.length);
                    return new Match<>(out, cai.skip(data.length - from));
                } else {
                    List<Character> out = toList(data, from, idx);
                    return new Match<>(out, cai.skip(idx - from));
                }
            }

            // Fallback: generic scan
            Input<Character> cur = in;
            List<Character> buf = new ArrayList<>();
            while (!cur.isEof()) {
                // quick pre-check by first char to avoid building sub-strings often
                if (cur.current() == first) {
                    Result<Character, String> tryNeedle = string(needle).apply(cur);
                    if (tryNeedle.matches()) {
                        return new Match<>(buf, cur); // do not consume needle
                    }
                }
                buf.add(cur.current());
                cur = cur.next();
            }
            return new Match<>(buf, cur);
        });
    }

    private static int indexOf(CharSequence haystack, String needle, int from) {
        // Use the platform’s indexOf for CharSequence (via toString() only if necessary)
        if (haystack instanceof String s) {
            return s.indexOf(needle, from);
        }
        // Avoid copying when possible: manual scan using first-char filter
        char c0 = needle.charAt(0);
        int max = haystack.length() - needle.length();
        outer: for (int i = Math.max(0, from); i <= max; i++) {
            if (haystack.charAt(i) != c0) continue;
            for (int j = 1; j < needle.length(); j++) {
                if (haystack.charAt(i + j) != needle.charAt(j)) continue outer;
            }
            return i;
        }
        return -1;
    }

    private static int indexOf(char[] haystack, String needle, int from) {
        char c0 = needle.charAt(0);
        int max = haystack.length - needle.length();
        outer: for (int i = Math.max(0, from); i <= max; i++) {
            if (haystack[i] != c0) continue;
            for (int j = 1; j < needle.length(); j++) {
                if (haystack[i + j] != needle.charAt(j)) continue outer;
            }
            return i;
        }
        return -1;
    }

    private static List<Character> toList(CharSequence data, int start, int end) {
        List<Character> out = new ArrayList<>();
        for (int i = start; i < end; i++) out.add(data.charAt(i));
        return out;
    }

    private static List<Character> toList(char[] data, int start, int end) {
        List<Character> out = new ArrayList<>();
        for (int i = start; i < end; i++) out.add(data[i]);
        return out;
    }

    /**
     * Creates a parser that matches an exact string of characters.
     * <p>
     * The {@code string()} method creates a parser that attempts to match the input exactly
     * against the provided string. This parser succeeds only if the entire string is matched
     * character by character. The parsing process works as follows:
     * <ol>
     *   <li>Compares each character in the input with the corresponding character in the target string</li>
     *   <li>If all characters match in sequence, returns the entire matched string</li>
     *   <li>If any character differs, fails with information about the mismatch point</li>
     *   <li>If input is too short, fails with an EOF error</li>
     * </ol>
     * <p>
     * Key features:
     * <ul>
     *   <li>Performs exact, case-sensitive string matching</li>
     *   <li>Provides detailed error information showing where the match failed</li>
     *   <li>Optimized for performance with direct character comparison</li>
     *   <li>Special case handling for empty strings</li>
     * </ul>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Uses character-by-character comparison rather than multiple parsers</li>
     *   <li>Returns the original string rather than rebuilding it from characters</li>
     *   <li>Provides context in error messages showing partial matches</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Match specific keywords
     * Parser<Character, String> ifKeyword = string("if");
     * Parser<Character, String> elseKeyword = string("else");
     *
     * // Match operators
     * Parser<Character, String> plusOperator = string("+");
     * Parser<Character, String> minusOperator = string("-");
     *
     * // Match multi-character tokens
     * Parser<Character, String> arrow = string("->");
     * Parser<Character, String> equality = string("==");
     *
     * // Combine with other parsers
     * Parser<Character, String> helloWorld = string("Hello").thenSkip(string(" ")).then(string("World"))
     *     .map((hello, world) -> hello + " " + world);
     * }</pre>
     *
     * @param str the exact string to match
     * @return a parser that matches the specified string
     * @see #regex(String) for flexible pattern matching
     * @see #chr(char) for single character matching
     */
    public static Parser<Character, String> string(String str) {
        return new Parser<>(in -> {
            Input<Character> currentInput = in;

            // Handle empty string case
            if (str.isEmpty()) {
                return new Match<>("", currentInput);
            }

            // Check if we have enough characters left in the input
            for (int i = 0; i < str.length(); i++) {
                if (currentInput.isEof() || str.charAt(i) != currentInput.current() ) {
                    Failure<Character, String> noMatch = new NoMatch<>(currentInput, str.substring(i,i+1));
                    if (i > 0) {
                        return new PartialMatch<>(currentInput, noMatch);
                    }
                    return noMatch;
                }
                currentInput = currentInput.next();
            }

            return new Match<>(str, currentInput);
        });
    }

    /**
     * Creates a parser that accepts any character that appears in the provided string.
     * <p>
     * The {@code oneOf(String)} method creates a parser that succeeds when the current input
     * character matches any character in the provided string. This parser is useful for defining
     * character classes or sets of acceptable characters. The parsing process works as follows:
     * <ol>
     *   <li>Checks if the input is not at EOF</li>
     *   <li>Tests if the current character appears in the provided string</li>
     *   <li>If the character is found in the string, consumes it and returns it</li>
     *   <li>If the character is not in the string or at EOF, fails with an error message</li>
     * </ol>
     * <p>
     * Performance considerations:
     * <ul>
     *   <li>For small strings (less than 10 characters), uses {@link String#indexOf(int)} for lookup</li>
     *   <li>For larger strings, creates a {@link HashSet} of characters for O(1) lookup performance</li>
     *   <li>This optimization makes character class matching efficient even with large character sets</li>
     * </ul>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Delegates to {@link Combinators#satisfy(String, Predicate)} with appropriate predicates</li>
     *   <li>Uses different implementation strategies based on input string length</li>
     *   <li>Consumes exactly one character when successful</li>
     *   <li>Returns the matched character as the result</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse any digit
     * Parser<Character, Character> digit = oneOf("0123456789");
     *
     * // Parse any hexadecimal digit
     * Parser<Character, Character> hexDigit = oneOf("0123456789ABCDEFabcdef");
     *
     * // Parse any vowel
     * Parser<Character, Character> vowel = oneOf("aeiouAEIOU");
     *
     * // Parse any operator
     * Parser<Character, Character> operator = oneOf("+-*\/");
     *
     * // Using with other parsers
     * Parser<Character, String> identifier =
     *     oneOf("_$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
     *     .then(oneOf("_$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789").oneOrMore())
     *     .map((first, rest) -> first + rest.stream()
     *         .map(String::valueOf)
     *         .collect(Collectors.joining()));
     * }</pre>
     *
     * @param str the string containing all acceptable characters
     * @return a parser that accepts any character that appears in the provided string
     * @see Lexical#chr(Predicate) for matching characters with arbitrary predicates
     * @see #chr(char) for matching a specific character
     * @see Combinators#oneOf(Object...) for the generic version of this parser
     */
    public static Parser<Character, Character> oneOf(String str) {
        // For small strings (under 10 chars), this approach is efficient
        if (str.length() < 10) {
            return satisfy("<oneOf> " + str, c -> str.indexOf(c) != -1);
        }

        // For larger character sets, use a Set for O(1) lookups
        Set<Character> charSet = new HashSet<>();
        for (int i = 0; i < str.length(); i++) {
            charSet.add(str.charAt(i));
        }

        return satisfy("character in set [" + str + "]", charSet::contains);
    }

    /**
     * Creates a parser that matches input against a regular expression pattern with specified flags.
     * <p>
     * The {@code regex()} method creates a parser that incrementally matches characters from the input
     * stream against the provided regular expression pattern, using the specified Pattern flags.
     * This parser handles both start (^) and end ($) anchors appropriately in the context of streaming input.
     * <p>
     * Valid flag values from {@link Pattern} include:
     * <ul>
     *   <li>{@link Pattern#CASE_INSENSITIVE} - Case-insensitive matching</li>
     *   <li>{@link Pattern#MULTILINE} - Multiline mode, affects ^ and $ behavior</li>
     *   <li>{@link Pattern#DOTALL} - Dot matches all characters including line terminators</li>
     *   <li>{@link Pattern#UNICODE_CASE} - Unicode-aware case folding</li>
     *   <li>{@link Pattern#CANON_EQ} - Canonical equivalence</li>
     *   <li>{@link Pattern#LITERAL} - Treat pattern as a literal string</li>
     *   <li>{@link Pattern#UNICODE_CHARACTER_CLASS} - Unicode character classes</li>
     *   <li>{@link Pattern#COMMENTS} - Permits whitespace and comments in pattern</li>
     * </ul>
     * <p>
     * Multiple flags can be combined using the bitwise OR operator (|).
     * <p>
     * Key features:
     * <ul>
     *   <li>Progressively reads characters one by one, building a buffer</li>
     *   <li>Attempts matching after each character to support early success/failure</li>
     *   <li>Properly respects start anchors (^) by only matching from the beginning</li>
     *   <li>Properly respects end anchors ($) by only accepting matches at the end of input</li>
     *   <li>For non-anchored patterns, returns the longest valid match</li>
     *   <li>Efficiently handles streaming input by using {@link Matcher#hitEnd()} for optimization</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Case-insensitive word parser
     * Parser<Character, String> caseInsensitiveWord = regex("[a-z]+", Pattern.CASE_INSENSITIVE);
     * caseInsensitiveWord.parse("Hello").value();  // Returns "Hello"
     *
     * // Multiline regex with DOTALL flag
     * Parser<Character, String> multiline = regex(".*end", Pattern.DOTALL);
     * multiline.parse("line1\nline2\nend").value();  // Returns "line1\nline2\nend"
     *
     * // Combined flags
     * Parser<Character, String> complex = regex("# match digits\n\\d+",
     *     Pattern.COMMENTS | Pattern.UNICODE_CHARACTER_CLASS);
     * }</pre>
     *
     * @param regex the regular expression pattern to match against
     * @param flags the flags to be used with this pattern, as defined in {@link Pattern}
     * @return a parser that matches the input against the given regular expression with specified flags
     * @see Pattern for information about Java regular expression syntax and flag constants
     * @see #regex(String) for the simpler version with default flags
     */
    public static Parser<Character, String> regex(String regex, int flags) {
        boolean hasEndAnchor = regex.endsWith("$") && !regex.endsWith("\\$");
        Pattern pattern = Pattern.compile(regex, flags);

        return new Parser<>(in -> {
            // Special case for empty input
            if (in.isEof()) {
                Matcher emptyMatcher = pattern.matcher("");
                if (emptyMatcher.lookingAt()) {
                    return new Match<>(emptyMatcher.group(), in);
                }
                return new NoMatch<>(in, regex);
            }

            StringBuilder buffer = new StringBuilder();
            Input<Character> current = in;
            int maxLookAhead = 1000; // Safety limit
            int position = 0;

            // Track best match found so far
            String bestMatch = null;

            // Progressive matching loop
            while (!current.isEof() && position++ < maxLookAhead) {
                char c = current.current();
                buffer.append(c);

                // Get next position to check for EOF
                Input<Character> next = current.next();
                boolean isAtEnd = next.isEof();

                // Try matching at each step
                Matcher matcher = pattern.matcher(buffer);

                if (matcher.lookingAt()) {
                    String match = matcher.group();

                    // For end-anchored patterns, only accept matches at end of input
                    if (!hasEndAnchor || isAtEnd) {
                        bestMatch = match;
                    }
                }

                // If the matcher doesn't benefit from more input, we can stop
                if (!matcher.hitEnd()) {
                    break;
                }

                // Continue reading the next character
                current = next;
            }

            // Return the best match found if any
            if (bestMatch != null) {
                return new Match<>(bestMatch, in.skip(bestMatch.length()));
            }

            // No match found
            //String preview = buffer.length() > 10 ? buffer.substring(0, 10) + "..." : buffer.toString();
            return new NoMatch<>(in, regex);
        });
    }

    /**
     * Creates a parser that matches input against a regular expression pattern using default flags.
     * <p>
     * This is a convenience method that calls {@link #regex(String, int)} with flags set to 0,
     * which means no special Pattern flags are enabled. The parser incrementally matches characters
     * from the input stream against the provided regular expression pattern.
     * <p>
     * Key features:
     * <ul>
     *   <li>Progressively reads characters one by one, building a buffer</li>
     *   <li>Attempts matching after each character to support early success/failure</li>
     *   <li>Properly respects start anchors (^) by only matching from the beginning</li>
     *   <li>Properly respects end anchors ($) by only accepting matches at the end of input</li>
     *   <li>For non-anchored patterns, returns the longest valid match</li>
     *   <li>Efficiently handles streaming input by using {@link Matcher#hitEnd()} for optimization</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a word (sequence of letters)
     * Parser<Character, String> wordParser = regex("[a-zA-Z]+");
     * wordParser.parse("Hello123").value();  // Returns "Hello"
     *
     * // Parse an identifier (letters, digits, underscore)
     * Parser<Character, String> identifier = regex("[a-zA-Z_][a-zA-Z0-9_]*");
     * identifier.parse("_var123").value();  // Returns "_var123"
     *
     * // Parse a number with optional decimal part
     * Parser<Character, String> number = regex("\\d+(\\.\\d+)?");
     * number.parse("42.5").value();  // Returns "42.5"
     * }</pre>
     *
     * @param regex the regular expression pattern to match against
     * @return a parser that matches the input against the given regular expression
     * @see #regex(String, int) for creating a regex parser with specific flags
     * @see Pattern for information about Java regular expression syntax
     */
    public static Parser<Character, String> regex(String regex) {
        return regex(regex, 0);
    }

    /**
     * Creates a parser that matches a specific character.
     * <p>
     * The {@code chr(char)} method creates a parser that tests if the current input character
     * equals the specified character value. This parser succeeds only when the current input
     * character exactly matches the target character. The parsing process works as follows:
     * <ol>
     *   <li>Checks if the input is not at EOF</li>
     *   <li>Compares the current character with the specified character</li>
     *   <li>If they match, consumes the character and returns it</li>
     *   <li>If they don't match or at EOF, fails with an error message</li>
     * </ol>
     * <p>
     * This parser is a specialized version of {@link Combinators#is(Object)} for character input. It's
     * commonly used for parsing punctuation, operators, and other specific characters in
     * text-based parsing.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Delegates to {@link Combinators#is(Object)} for equality checking</li>
     *   <li>Consumes exactly one character when successful</li>
     *   <li>Returns the matched character as the result</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse specific characters
     * Parser<Character, Character> comma = chr(',');
     * Parser<Character, Character> semicolon = chr(';');
     * Parser<Character, Character> plus = chr('+');
     *
     * // Combine to parse a simple addition expression
     * Parser<Character, Integer> addExpr = digit.then(plus).then(digit)
     *     .map((d1, op, d2) ->
     *         Character.getNumericValue(d1) + Character.getNumericValue(d2));
     *
     * // Parse punctuated list items
     * Parser<Character, List<String>> commaSeparated =
     *     word.separatedByMany(chr(','));
     * }</pre>
     *
     * @param c the specific character to match
     * @return a parser that matches the specified character
     * @see Combinators#is(Object) for the generic version of this parser
     * @see Lexical#chr(Predicate) for matching characters based on predicates
     * @see Lexical#string(String) for matching sequences of characters
     */
    public static Parser<Character, Character> chr(char c) {
        return Combinators.is(c);
    }

    /**
     * Creates a parser that accepts a single character matching the given predicate.
     * <p>
     * The {@code chr(Predicate)} method creates a parser that tests if the current input character
     * satisfies the provided predicate function. This parser succeeds if the predicate returns true
     * for the current character. The parsing process works as follows:
     * <ol>
     *   <li>Checks if the input is not at EOF</li>
     *   <li>Applies the predicate to the current character</li>
     *   <li>If the predicate is satisfied, consumes the character and returns it</li>
     *   <li>If the predicate is not satisfied or at EOF, fails with an error message</li>
     * </ol>
     * <p>
     * This parser is a specialized version of {@link Combinators#satisfy(String, Predicate)} for character input.
     * It's a fundamental building block for text-based parsers, allowing character filtering based
     * on arbitrary conditions.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Delegates to {@link Combinators#satisfy(String, Predicate)} with a generic "character" expected type</li>
     *   <li>Consumes exactly one character when successful</li>
     *   <li>Returns the matched character as the result</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse any digit character
     * Parser<Character, Character> digit = chr(Character::isDigit);
     *
     * // Parse any uppercase letter
     * Parser<Character, Character> uppercase = chr(Character::isUpperCase);
     *
     * // Parse any whitespace character
     * Parser<Character, Character> whitespace = chr(Character::isWhitespace);
     *
     * // Parse any character except 'x'
     * Parser<Character, Character> notX = chr(c -> c != 'x');
     *
     * // Combining character parsers
     * Parser<Character, String> hexDigit = chr(c ->
     *     Character.isDigit(c) || "ABCDEFabcdef".indexOf(c) >= 0)
     *     .oneOrMore().map(chars -> chars.stream()
     *         .map(String::valueOf)
     *         .collect(Collectors.joining()));
     * }</pre>
     *
     * @param predicate the condition that characters must satisfy
     * @return a parser that matches a single character based on the predicate
     * @see Combinators#satisfy(String, Predicate) for the generic version of this parser
     * @see Lexical#chr(char) for matching a specific character
     * @see Lexical#oneOf(String) for matching against a set of characters
     */
    public static Parser<Character, Character> chr(Predicate<Character> predicate) {
        return satisfy("<character>", predicate);
    }
}
