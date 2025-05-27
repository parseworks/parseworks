package io.github.parseworks;

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
 * This enum is used in parser combinators like {@link Parser#chain(Parser, Associativity)}
 * to specify how repeated operations should be grouped during parsing.
 *
 * @see Parser#chainLeftMany(Parser) for left-associative parsing
 * @see Parser#chainRightMany(Parser) for right-associative parsing
 */
public enum Associativity {
    /**
     * Left-to-right associativity.
     * <p>
     * Operations are grouped from left to right, meaning the leftmost operations
     * are performed first. This is the common associativity for:
     * <ul>
     *   <li>Arithmetic operators: addition, subtraction, multiplication, division</li>
     *   <li>String concatenation in many languages</li>
     *   <li>Method chaining (e.g., a.b().c() is evaluated as (a.b()).c())</li>
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