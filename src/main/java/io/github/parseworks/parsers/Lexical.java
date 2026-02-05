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
    public static final Parser<Character, Character> alpha = satisfy("<alphabet>", Character::isLetter);

    /**
     * Matches a single alphanumeric character.
     */
    public static final Parser<Character, Character> alphaNumeric = satisfy( "<alphanumeric>", Character::isLetterOrDigit);

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
    public static final Parser<Character,Character> whitespace = satisfy("<whitespace>", Character::isWhitespace);

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
