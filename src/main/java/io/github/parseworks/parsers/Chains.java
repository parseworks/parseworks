package io.github.parseworks.parsers;

import io.github.parseworks.Lists;
import io.github.parseworks.Parser;
import io.github.parseworks.impl.Pair;

import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

public class Chains {


    /**
     * Creates a repeating parser that handles operator expressions with specified associativity.
     * <p>
     * The {@code chain} method is a powerful parser combinator for parsing sequences of
     * operands separated by operators, commonly used for expression parsing. It processes
     * the input as follows:
     * <ol>
     *   <li>First applies this parser to obtain an initial operand</li>
     *   <li>Then repeatedly tries to parse an operator followed by another operand</li>
     *   <li>Combines the results using the parsed operators according to the specified associativity</li>
     * </ol>
     * <p>
     * The method supports two associativity modes:
     * <ul>
     *   <li><b>LEFT</b>: Operators are evaluated from left to right. For example, "a+b+c" is
     *       interpreted as "(a+b)+c"</li>
     *   <li><b>RIGHT</b>: Operators are evaluated from right to left. For example, "a+b+c" is
     *       interpreted as "a+(b+c)"</li>
     * </ul>
     * <p>
     * This method serves as the foundation for more specific chainXxx methods like
     * {@link Parser#chainLeftOneOrMore(Parser)} and {@link Parser#chainRightOneOrMore(Parser)}.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse addition expressions with left associativity
     * Parser<Character, Integer> number = Numeric.integer;
     * Parser<Character, BinaryOperator<Integer>> add =
     *     Lexical.chr('+').as((a, b) -> a + b);
     *
     * Parser<Character, Integer> expression =
     *     Chains.chain(number, add, Associativity.LEFT);
     *
     * // Parses "1+2+3" as (1+2)+3 = 6
     * }</pre>
     *
     * @param op            the parser that recognizes and returns binary operators
     * @param associativity the associativity rule to apply (LEFT or RIGHT)
     * @return a parser that handles operator expressions with the specified associativity
     * @throws IllegalArgumentException if any parameter is null
     * @see Parser#chainLeftOneOrMore(Parser) for a specialized left-associative version
     * @see Parser#chainRightOneOrMore(Parser) for a specialized right-associative version
     * @see Associativity for the associativity options
     */
    public static <I,A> Parser<I, A> chain(Parser<I,A> parser, Parser<I, BinaryOperator<A>> op, Associativity associativity) {
        if (associativity == Associativity.LEFT) {
            final Parser<I, UnaryOperator<A>> plo =
                op.then(parser)
                    .map((f, y) -> x -> f.apply(x, y));
            return parser.then(plo.zeroOrMore())
                .map((a, lf) -> Lists.foldLeft(lf, a, (acc, f) -> f.apply(acc)));
        } else {
            return parser.then(op.then(parser).map(Pair::new).zeroOrMore())
                .map((a, pairs) -> pairs.stream().reduce(a, (acc, tuple) -> tuple.left().apply(tuple.right(), acc), (a1, a2) -> a1));
        }
    }

    /**
     * Defines operator associativity rules for expression parsing.
     * <p>
     * Associativity determines the order in which operators of the same precedence
     * are evaluated when multiple operations occur in sequence without explicit grouping.
     * This is a fundamental concept in parsing expressions, especially in the context
     * of building abstract syntax trees or evaluating expressions.
     * <p>
     * For example, in the expression "a + b + c":
     * <ul>
     *   <li>With LEFT associativity, it's evaluated as "(a + b) + c"</li>
     *   <li>With RIGHT associativity, it's evaluated as "a + (b + c)"</li>
     * </ul>
     * <p>
     * While associativity doesn't affect the result for commutative and associative
     * operations like addition, it's critical for non-commutative operations like
     * subtraction or exponentiation where different groupings produce different results.
     * <p>
     * This enum is used in parser combinators like {@link Chains#chain(Parser, Parser, Associativity)}
     * to specify how repeated operations should be grouped during parsing.
     *
     * @see Parser#chainLeftOneOrMore(Parser) for left-associative parsing
     * @see Parser#chainRightOneOrMore(Parser) for right-associative parsing
     */
    public enum Associativity {
        /**
         * Left-to-right associativity.
         * <p>
         * Operations are grouped from left to right, meaning the leftmost operations
         * are performed first. This is the common associativity for:
         * <ul>
         *   <li>Arithmetic operators: addition, subtraction, multiplication, division</li>
         *   <li>String concatenation in some languages</li>
         * </ul>
         * <p>
         * Example: "5-3-2" with LEFT associativity is evaluated as "(5-3)-2" = 0
         */
        LEFT,

        /**
         * Right-to-left associativity.
         * <p>
         * Operations are grouped from right to left, meaning the rightmost operations
         * are performed first. This is the common associativity for:
         * <ul>
         *   <li>Exponentiation (e.g., 2^3^2 is evaluated as 2^(3^2) = 2^9 = 512)</li>
         *   <li>Assignment operators (e.g., a=b=c is equivalent to a=(b=c))</li>
         *   <li>Conditional operators in some languages</li>
         * </ul>
         * <p>
         * Example: "2^3^2" with RIGHT associativity is evaluated as "2^(3^2)" = 2^9 = 512
         */
        RIGHT
    }
}
