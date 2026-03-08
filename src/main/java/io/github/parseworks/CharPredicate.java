package io.github.parseworks;

/**
 * A functional interface for character predicates.
 */
@FunctionalInterface
public interface CharPredicate {

    /**
     * Tests whether the given character satisfies this predicate.
     *
     * @param c the character to test
     * @return {@code true} if the character satisfies the predicate, {@code false} otherwise
     */
    boolean test(char c);

    /**
     * Returns a composed predicate that represents a short-circuiting logical AND
     * of this predicate and another.
     *
     * @param other the other predicate to combine with this one
     * @return a composed predicate that is {@code true} only if both predicates are {@code true}
     */
    default CharPredicate and(CharPredicate other) {
        return c -> test(c) && other.test(c);
    }

    /**
     * Returns a composed predicate that represents a short-circuiting logical OR
     * of this predicate and another.
     *
     * @param other the other predicate to combine with this one
     * @return a composed predicate that is {@code true} if either predicate is {@code true}
     */
    default CharPredicate or(CharPredicate other) {
        return c -> test(c) || other.test(c);
    }

    /**
     * Returns a predicate that represents the logical negation of this predicate.
     *
     * @return a negated predicate
     */
    default CharPredicate negate() {
        return c -> !test(c);
    }

    /**
     * Returns a predicate that tests if a character is equal to the target character.
     *
     * @param target the character to compare against
     * @return a predicate that is {@code true} if the character matches the target
     */
    static CharPredicate is(char target) {
        return c -> c == target;
    }

    /**
     * Returns a predicate that tests if a character is not equal to the target character.
     *
     * @param target the character to compare against
     * @return a predicate that is {@code true} if the character does not match the target
     */
    static CharPredicate isNot(char target) {
        return c -> c != target;
    }

    /**
     * Returns a predicate that tests if a character is within the specified range (inclusive).
     *
     * @param start the start of the range (inclusive)
     * @param end   the end of the range (inclusive)
     * @return a predicate that is {@code true} if the character is within the range
     */
    static CharPredicate range(char start, char end) {
        return c -> c >= start && c <= end;
    }

    /**
     * Returns a predicate that tests if a character is present in the given string.
     *
     * @param chars a string containing the characters to match
     * @return a predicate that is {@code true} if the character is in the string
     */
    static CharPredicate anyOf(String chars) {
        return c -> chars.indexOf(c) >= 0;
    }

    /**
     * Returns a predicate that tests if a character is not present in the given string.
     *
     * @param chars a string containing the characters to avoid
     * @return a predicate that is {@code true} if the character is not in the string
     */
    static CharPredicate noneOf(String chars) {
        return c -> chars.indexOf(c) < 0;
    }

    /**
     * A predicate that tests if a character is a digit.
     */
    CharPredicate digit = Character::isDigit;

    /**
     * A predicate that tests if a character is a letter.
     */
    CharPredicate letter = Character::isLetter;

    /**
     * A predicate that tests if a character is a letter or digit.
     */
    CharPredicate letterOrDigit = Character::isLetterOrDigit;

    /**
     * A predicate that tests if a character is whitespace.
     */
    CharPredicate whitespace = Character::isWhitespace;

    /**
     * A predicate that tests if a character is upper case.
     */
    CharPredicate upperCase = Character::isUpperCase;

    /**
     * A predicate that tests if a character is lower case.
     */
    CharPredicate lowerCase = Character::isLowerCase;
}
