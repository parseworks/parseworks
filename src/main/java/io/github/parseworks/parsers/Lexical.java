package io.github.parseworks.parsers;

import io.github.parseworks.*;
import io.github.parseworks.impl.IntObjectMap;
import io.github.parseworks.impl.result.Match;
import io.github.parseworks.impl.result.NoMatch;
import io.github.parseworks.impl.result.PartialMatch;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.parseworks.parsers.Combinators.satisfy;

/**
 * Common text parsers for characters, strings, and whitespace.
 * <pre>{@code
 * Parser<Character, String> greeting =
 *     Lexical.string("Hello").thenSkip(Lexical.whitespace).then(Lexical.word);
 * }</pre>
 */
public class Lexical {


    /**
     * Matches a single alphabetical character (a-z, A-Z).
     * <pre>{@code
     * alpha.parse("abc").value(); // 'a'
     * alpha.parse("123").matches(); // false
     * }</pre>
     *
     * @see Numeric#numeric
     * @see #alphaNumeric
     */
    public static final Parser<Character, Character> alpha = satisfy("<alphabet>", (CharPredicate) Character::isLetter);

    /**
     * Matches a single alphanumeric character.
     */
    public static final Parser<Character, Character> alphaNumeric = satisfy( "<alphanumeric>", (CharPredicate) Character::isLetterOrDigit);

    /**
     * Trims whitespace around the given parser.
     * <pre>{@code
     * trim(string("foo")).parse("  foo  ").value(); // "foo"
     * }</pre>
     *
     * @param parser parser to wrap
     * @param <A>    result type
     * @return trimmed parser
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
     * Matches a sequence of letters and returns them as a string.
     * <pre>{@code
     * word.parse("Hello123").value(); // "Hello"
     * }</pre>
     */
    public static final Parser<Character, String> word = alpha.oneOrMore().map(Lists::join);

    /**
     * Matches a single whitespace character.
     */
    public static final Parser<Character,Character> whitespace = satisfy("<whitespace>", (CharPredicate) Character::isWhitespace);

    /**
     * Creates a parser that repeatedly applies this parser as long as the condition evaluates to true.
     * <p>
     * This parser will:
     * <ul>
     *   <li>Check if the condition is true for the current input position</li>
     *   <li>If true, apply this parser and collect the result</li>
     *   <li>Continue until either the condition becomes false, parsing fails, or input is exhausted</li>
     *   <li>Return all collected results as a String</li>
     * </ul>
     * <p>
     * The implementation includes a check to prevent infinite loops in cases where the parser
     * succeeds but doesn't advance the input position.
     *
     * @param condition a predicate that returns a boolean indicating whether to continue collecting
     * @return a parser that collects characters while the condition is true
     * @throws IllegalArgumentException if the condition predicate is null
     */
    public static Parser<Character,String> takeWhile(CharPredicate condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Condition parser cannot be null");
        }

        return new Parser<>(in -> {
            CharSequence data = in.data();
            int start = in.position();
            int current = start;
            int length = data.length();

            while (current < length && condition.test(data.charAt(current))) {
                current++;
            }
            return new Match<>(data.subSequence(start, current).toString(), in.skip(current - start));
        });
    }

    /**
     * Collects characters until the first occurrence of the given needle.
     * <pre>{@code
     * takeUntil("-->").parse("comment-->").value(); // ['c','o','m','m','e','n','t']
     * }</pre>
     *
     * @param needle delimiter string
     * @return characters before the needle
     */
    public static Parser<Character, List<Character>> takeUntil(String needle) {
        Objects.requireNonNull(needle, "needle");
        if (needle.isEmpty()) {
            // Edge-case: empty delimiter – always succeed with empty list
            return new Parser<>(in -> new Match<>(Collections.emptyList(), in));
        }
        final char first = needle.charAt(0);

        return new Parser<>(in -> {
            if (needle.isEmpty()) {
                return new Match<>(Collections.emptyList(), in);
            }
            CharSequence data = in.data();
            int start = in.position();
            int idx = indexOf(data, needle, start);
            if (idx < 0) {
                // Not found: consume to EOF
                List<Character> out = toList(data, start, data.length());
                return new Match<>(out, in.skip(data.length() - start));
            } else {
                List<Character> out = toList(data, start, idx);
                return new Match<>(out, in.skip(idx - start));
            }
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
     * Matches an exact string of characters.
     * <pre>{@code
     * string("if").parse("if").value(); // "if"
     * }</pre>
     *
     * @param str exact string to match
     * @return a parser matching the string
     * @see #regex(String)
     * @see #chr(char)
     */
    public static Parser<Character, String> string(String str) {
        return new Parser<>(in -> {
            if (str.isEmpty()) {
                return new Match<>("", in);
            }

            CharSequence data = in.data();
            int start = in.position();
            int strLen = str.length();

            if (start + strLen > data.length()) {
                Failure<Character, String> noMatch = new NoMatch<>(in, str.substring(0, 1));
                // Find how many characters matched
                int matched = 0;
                while (matched < data.length() - start && str.charAt(matched) == data.charAt(start + matched)) {
                    matched++;
                }
                if (matched > 0) {
                    return new PartialMatch<>(in.skip(matched), noMatch);
                }
                return noMatch;
            }

            for (int i = 0; i < strLen; i++) {
                if (str.charAt(i) != data.charAt(start + i)) {
                    Failure<Character, String> noMatch = new NoMatch<>(in.skip(i), str.substring(i, i + 1));
                    if (i > 0) {
                        return new PartialMatch<>(in.skip(i), noMatch);
                    }
                    return noMatch;
                }
            }

            return new Match<>(str, in.skip(strLen));
        });
    }

    /**
     * Matches any single character from the provided string.
     * <pre>{@code
     * oneOf("aeiou").parse("e").value(); // 'e'
     * }</pre>
     *
     * @param str acceptable characters
     * @return a parser matching any character in the string
     */
    public static Parser<Character, Character> oneOf(String str) {
        // For small strings (under 10 chars), this approach is efficient
        if (str.length() < 10) {
            return satisfy("<oneOf> " + str, (CharPredicate) (c -> str.indexOf(c) != -1));
        }

        // For larger character sets, use a Set for O(1) lookups
        Set<Character> charSet = new HashSet<>();
        for (int i = 0; i < str.length(); i++) {
            charSet.add(str.charAt(i));
        }

        return satisfy("character in set [" + str + "]", (Predicate<Character>) charSet::contains);
    }

    /**
     * Matches input against a regular expression pattern.
     * <pre>{@code
     * regex("[a-z]+", Pattern.CASE_INSENSITIVE).parse("ABC").value(); // "ABC"
     * }</pre>
     *
     * @param regex regular expression pattern
     * @param flags Pattern flags
     * @return a parser matching the regex
     * @see Pattern
     */
    public static Parser<Character, String> regex(String regex, int flags) {
        Pattern pattern = Pattern.compile(regex, flags);

        return new Parser<>(in -> {
            CharSequence data = in.data();
            int start = in.position();
            Matcher matcher = pattern.matcher(data);
            matcher.region(start, data.length());

            if (matcher.lookingAt()) {
                String match = matcher.group();
                return new Match<>(match, in.skip(match.length()));
            }

            return new NoMatch<>(in, regex);
        });
    }

    /**
     * Matches input against a regular expression pattern using default flags.
     * <pre>{@code
     * regex("\\d+").parse("123").value(); // "123"
     * }</pre>
     *
     * @param regex regular expression pattern
     * @return a parser matching the regex
     * @see #regex(String, int)
     */
    public static Parser<Character, String> regex(String regex) {
        return regex(regex, 0);
    }


    /**
     * Parses a string enclosed in quotes with support for escape sequences.
     * <p>
     * This version uses an {@link IntObjectMap} for escape character mappings to avoid boxing.
     *
     * @param quote   the character used for quoting (e.g., '"')
     * @param escape  the character used for escaping (e.g., '\\')
     * @param escapes an {@link IntObjectMap} of escape characters to their literal values
     * @return a parser that matches an escaped string and returns its unescaped content
     */
    public static Parser<Character,String> escapedString(char quote, char escape, IntObjectMap<Character> escapes) {
        return new Parser<>(in -> {
            if (in.isEof() || in.current() != quote) {
                return new NoMatch<>(in, String.valueOf(quote));
            }

            CharSequence data = in.data();
            int startPos = in.position();
            int currentPos = startPos + 1; // skip opening quote
            int dataLen = data.length();
            StringBuilder sb = new StringBuilder();
            int lastCopiedPos = currentPos;

            while (currentPos < dataLen) {
                char c = data.charAt(currentPos);
                if (c == escape) {
                    int escapePos = currentPos;
                    currentPos++;
                    if (currentPos >= dataLen) {
                        if (escape == quote) {
                            sb.append(data, lastCopiedPos, escapePos);
                            return new Match<>(sb.toString(), in.skip(escapePos - startPos + 1));
                        }
                        return new NoMatch<>(in.skip(currentPos - startPos), "closing quote after escape");
                    }
                    char escapedChar = data.charAt(currentPos);
                    Character replacement = escapes.get(escapedChar);
                    if (replacement != null) {
                        sb.append(data, lastCopiedPos, escapePos);
                        sb.append(replacement);
                        lastCopiedPos = currentPos + 1;
                    } else if (escape == quote) {
                        // Rollback and treat as closing quote
                        sb.append(data, lastCopiedPos, escapePos);
                        return new Match<>(sb.toString(), in.skip(escapePos - startPos + 1));
                    } else {
                        sb.append(data, lastCopiedPos, escapePos);
                        sb.append(escapedChar);
                        lastCopiedPos = currentPos + 1;
                    }
                } else if (c == quote) {
                    sb.append(data, lastCopiedPos, currentPos);
                    return new Match<>(sb.toString(), in.skip(currentPos - startPos + 1));
                }
                currentPos++;
            }

            return new NoMatch<>(in.skip(currentPos - startPos), "closing quote '" + quote + "'");
        });
    }

    /**
     * Parses a string enclosed in quotes with support for escape sequences.
     * <p>
     * This parser is optimized for performance by directly scanning the input data.
     * It handles:
     * <ul>
     *   <li>Finding the opening quote (must match exactly at the current position)</li>
     *   <li>Scanning for the closing quote</li>
     *   <li>Handling escape sequences defined in the provided map</li>
     *   <li>Correctly reporting errors for unclosed strings or invalid escape sequences</li>
     * </ul>
     *
     * @param quote   the character used for quoting (e.g., '"')
     * @param escape  the character used for escaping (e.g., '\\')
     * @param escapes a map of escape characters to their literal values (e.g., 'n' -> '\n')
     * @return a parser that matches an escaped string and returns its unescaped content
     */
    public static Parser<Character,String> escapedString(char quote, char escape, Map<Character, Character> escapes) {
        IntObjectMap<Character> map = new IntObjectMap<>();
        escapes.forEach(map::put);
        return escapedString(quote, escape, map);
    }



    /**
     * Matches a specific character.
     * <pre>{@code
     * chr(',').parse(",").value(); // ','
     * }</pre>
     *
     * @param c character to match
     * @return a parser matching the character
     * @see Combinators#is
     */
    public static Parser<Character, Character> chr(char c) {
        return Combinators.is(c);
    }

    /**
     * Matches a single character matching the given predicate.
     * <pre>{@code
     * chr(Character::isDigit).parse("1").value(); // '1'
     * }</pre>
     *
     * @param predicate condition for the character
     * @return a parser matching characters by predicate
     * @see Combinators#satisfy
     */
    public static Parser<Character, Character> chr(Predicate<Character> predicate) {
        return satisfy("<character>", predicate);
    }
}
