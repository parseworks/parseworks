package io.github.parseworks.parsers;

import io.github.parseworks.*;
import io.github.parseworks.impl.result.Match;
import io.github.parseworks.impl.result.NoMatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Core parser combinators for choice, sequence, and filtering.
 */
public class Combinators {

    private Combinators() {
    }

    /**
     * Matches any single input element of the specified type.
     * <pre>{@code
     * any(Character.class).parse("abc").value(); // 'a'
     * }</pre>
     *
     * @param type class of the input elements
     * @param <I>  input type
     * @return a parser that matches any single input element
     */
    public static <I> Parser<I, I> any(Class<I> type) {
        return new Parser<>(input -> {
            if (input.isEof()) {
                return new NoMatch<I, I>(input, type.descriptorString()).cast();
            } else {
                return new Match<>(input.current(), input.next());
            }
        });
    }

    /**
     * Unconditionally throws an exception from the supplier.
     * <pre>{@code
     * Parser<Character, Object> critical = throwError(() -> new IllegalStateException("Fail"));
     * }</pre>
     *
     * @param supplier exception supplier
     * @param <I>      input type
     * @return a parser that always throws
     * @see #fail()
     */
    public static <I> Parser<I, ? super Object> throwError(Supplier<? extends Exception> supplier) {
        return new Parser<>(in -> {
            throw sneakyThrow(supplier.get());
        });
    }

    /**
     * Utility method to bypass checked exception requirements.
     */
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> E sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }


    /**
     * Matches the current input element against a set of possible values.
     * <pre>{@code
     * oneOf('1', '2', '3').parse("1").value(); // '1'
     * }</pre>
     *
     * @param items values to match against
     * @param <I>   input type
     * @return a parser matching any of the items
     * @see #is(Object)
     */
    @SafeVarargs
    public static <I> Parser<I, I> oneOf(I... items) {
        return new Parser<>(in -> {
            if (in.isEof()) {
                return new NoMatch<>(in, "one of the expected values");
            }
            I current = in.current();
            for (I item : items) {
                if (Objects.equals(current, item)) {
                    return new Match<>(current, in.next());
                }
            }

            // Create a readable list of expected items
            StringBuilder expectedItems = new StringBuilder();
            if (items.length > 0) {
                expectedItems.append(items[0]);
                for (int i = 1; i < items.length; i++) {
                    if (i == items.length - 1) {
                        expectedItems.append(" or ");
                    } else {
                        expectedItems.append(", ");
                    }
                    expectedItems.append(items[i]);
                }
            }

            return new NoMatch<>(in, "one of [" + expectedItems + "]");
        });
    }

    /**
     * Succeeds if the input is at the end of the file (EOF).
     */
    public static <I> Parser<I, Void> eof() {
        return new Parser<>(input -> {
            if (input.isEof()) {
                return new Match<>(null, input);
            } else {
                return new NoMatch<>(input, "end of input");
            }
        });
    }

    /**
     * Unconditionally fails, consuming no input.
     */
    public static <I, A> Parser<I, A> fail() {
        return new Parser<>(in -> new NoMatch<>(in, "parser explicitly set to fail"));
    }

    /**
     * Fails with a specific error message.
     */
    public static <I, A> Parser<I, A> fail(String expected) {
        return new Parser<>(in -> new NoMatch<>(in, expected));
    }

    /**
     * Succeeds if the provided parser fails, returning the current input element.
     * <pre>{@code
     * not(Lexical.chr(Character::isDigit)).parse("a").value(); // 'a'
     * }</pre>
     *
     * @param parser parser to negate
     * @param <I>    input type
     * @return a negation parser
     * @see #isNot(Object)
     */
    public static <I, A> Parser<I, I> not(Parser<I, A> parser) {
        return new Parser<>(in -> {
            Result<I, A> result = parser.apply(in);
            if (result.matches() || !result.input().hasMore()) {
                // Provide more context about what was found that shouldn't have matched
                String found = result.input().hasMore() ? "expected parser to fail" : "end of input";
                return new NoMatch<>(in, found);
            }
            return new Match<>(in.current(), in.next());

        });
    }

    /**
     * Succeeds if the current input element is not equal to the provided value.
     * <pre>{@code
     * isNot(',').parse("a").value(); // 'a'
     * }</pre>
     *
     * @param value value to exclude
     * @param <I>   input type
     * @return a parser matching anything except the value
     * @see #not(Parser)
     * @see #is(Object)
     */
    public static <I> Parser<I, I> isNot(I value) {
        return new Parser<>(in -> {
            if (in.isEof()) {
                return new NoMatch<>(in, "any value except " + value);
            }
            I item = in.current();
            if (Objects.equals(item, value)) {
                return new NoMatch<>(in, "any value except " + value);
            } else {
                return new Match<>(item, in.next());
            }
        });
    }


    /**
     * Tries multiple parsers in sequence until one succeeds.
     *
     * @param parsers list of parsers to try
     * @param <I>     input type
     * @param <A>     result type
     * @return a choice parser
     * @see Parser#or(Parser)
     */
    public static <I, A> Parser<I, A> oneOf(List<Parser<I, A>> parsers) {
        if (parsers.isEmpty()) {
            throw new IllegalArgumentException("There must be at least one parser defined");
        }
        return new Parser<>(in -> {
            if (in.isEof()) {
                return new NoMatch<>(in, "eof before `oneOf` parser");
            }
            List<Failure<I, A>> failures = null;

            for (Parser<I, A> parser : parsers) {
                Result<I, A> result = parser.apply(in);
                if (result.matches()) {
                    return result;
                }
                
                // If it's a hard failure (consumed input), stop and return it
                if (result.type() == ResultType.PARTIAL && result.input().position() > in.position()) {
                    return result;
                }

                if (failures == null){
                    failures = new ArrayList<>();
                }
                failures.add((Failure<I, A>) result);
            }
            assert failures != null;
            return new NoMatch<>(failures);
        });
    }

    /**
     * Tries each of the provided parsers in order and succeeds with the first match.
     */
    @SafeVarargs
    public static <I, A> Parser<I, A> oneOf(Parser<I, A>... parsers) {
        return oneOf(Arrays.asList(parsers));
    }

    /**
     * Applies multiple parsers in sequence and collects their results in a list.
     * <pre>{@code
     * sequence(Arrays.asList(p1, p2, p3)).parse("123").value(); // [1, 2, 3]
     * }</pre>
     *
     * @param parsers parsers to apply
     * @param <I>     input type
     * @param <A>     result type
     * @return a sequence parser
     */
    public static <I, A> Parser<I, List<A>> sequence(List<Parser<I, A>> parsers) {
        return new Parser<>(in -> {
            List<A> results = new ArrayList<>();
            Input<I> currentInput = in;
            for (Parser<I, A> parser : parsers) {
                Result<I, A> result = parser.apply(currentInput);
                if (!result.matches()) {
                    return result.cast();
                }
                results.add(result.value());
                currentInput = result.input();
            }
            return new Match<>(results, currentInput);
        });
    }

    /**
     * Applies two parsers in sequence and returns an ApplyBuilder.
     */
    public static <I, A> ApplyBuilder<I, A, A> sequence(Parser<I, A> parserA, Parser<I, A> parserB) {
        return parserA.then(parserB);
    }

    /**
     * Applies three parsers in sequence and returns an ApplyBuilder3.
     */
    public static <I, A> ApplyBuilder<I, A, A>.ApplyBuilder3<A> sequence(Parser<I, A> parserA, Parser<I, A> parserB, Parser<I, A> parserC) {
        return parserA.then(parserB).then(parserC);
    }

    /**
     * Parses a single item that satisfies the given predicate.
     *
     * @param expectedType error message if not satisfied
     * @param predicate    condition to satisfy
     * @param <I>          input type
     * @return a satisfy parser
     */
    public static <I> Parser<I, I> satisfy(String expectedType, Predicate<I> predicate) {
        return new Parser<>(in -> {
            if (in.isEof()) {
                return new NoMatch<>(in, expectedType);
            }
            I item = in.current();
            if (predicate.test(item)) {
                return new Match<>(item, in.next());
            } else {
                return new NoMatch<>(in, expectedType);
            }
        });
    }

    /**
     * Matches the current input item against the provided value.
     */
    public static <I> Parser<I, I> is(I equivalence) {
        return new Parser<>(in -> {
            if (in.isEof()) {
                return new NoMatch<>(in, String.valueOf(equivalence));
            }
            I item = in.current();
            if (Objects.equals(item, equivalence)) {
                return new Match<>(item, in.next());
            } else {
                return new NoMatch<>(in, String.valueOf(equivalence));
            }
        });
    }


    /**
     * Backtracks on failure, reporting failure at the original position.
     */
    public static <I, A> Parser<I, A> attempt(Parser<I, A> parser) {
        return new Parser<>(in -> {
            Result<I, A> res = parser.apply(in);
            if (res.matches()) return res;
            return new NoMatch<>(in, "parse attempt", (Failure<?, ?>) res);
        });
    }

}
