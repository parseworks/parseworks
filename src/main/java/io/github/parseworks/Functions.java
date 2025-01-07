package io.github.parseworks;

/**
 * The `Functions` class provides a set of functional interfaces for functions with various arities.
 * These interfaces can be used to define functions that take multiple arguments and return a result.
 *
 * <p>Each functional interface in this class represents a function with a specific number of arguments,
 * ranging from three to eight. These interfaces are useful for functional programming and can be used
 * in lambda expressions, method references, and other functional contexts.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Functions.Func3<Integer, Integer, Integer, Integer> addThreeNumbers = (a, b, c) -> a + b + c;
 * int result = addThreeNumbers.apply(1, 2, 3); // result is 6
 * }</pre>
 *
 * @see java.util.function.Function
 * @see java.util.function.BiFunction
 */
public class Functions {
    /**
     * Function of arity 3.
     *
     * @param <A> the function's first argument type
     * @param <B> the function's second argument type
     * @param <C> the function's third argument type
     * @param <R> the function's return type
     */
    @FunctionalInterface
    public interface Func3<A, B, C, R> {
        /**
         * Apply this function.
         *
         * @param a the function's first argument
         * @param b the function's second argument
         * @param c the function's third argument
         * @return the result of applying this function
         */
        R apply(A a, B b, C c);
    }

    /**
     * Function of arity 4.
     *
     * @param <A> the function's first argument type
     * @param <B> the function's second argument type
     * @param <C> the function's third argument type
     * @param <D> the function's fourth argument type
     * @param <R> the function's return type
     */
    @FunctionalInterface
    public interface Func4<A, B, C, D, R> {

        /**
         * Apply this function.
         *
         * @param a the function's first argument
         * @param b the function's second argument
         * @param c the function's third argument
         * @param d the function's fourth argument
         * @return the result of applying this function
         */
        R apply(A a, B b, C c, D d);

    }

    /**
     * Function of arity 5.
     *
     * @param <A> the function's first argument type
     * @param <B> the function's second argument type
     * @param <C> the function's third argument type
     * @param <D> the function's fourth argument type
     * @param <E> the function's fifth argument type
     * @param <R> the function's return type
     */
    @FunctionalInterface
    public interface Func5<A, B, C, D, E, R> {

        /**
         * Apply this function.
         *
         * @param a the function's first argument
         * @param b the function's second argument
         * @param c the function's third argument
         * @param d the function's fourth argument
         * @param e the function's fifth argument
         * @return the result of applying this function
         */
        R apply(A a, B b, C c, D d, E e);

    }

    /**
     * Function of arity 6.
     *
     * @param <A> the function's first argument type
     * @param <B> the function's second argument type
     * @param <C> the function's third argument type
     * @param <D> the function's fourth argument type
     * @param <E> the function's fifth argument type
     * @param <G> the function's sixth argument type
     * @param <R> the function's return type
     */
    @FunctionalInterface
    public interface Func6<A, B, C, D, E, G, R> {
        /**
         * Apply this function.
         *
         * @param a the function's first argument
         * @param b the function's second argument
         * @param c the function's third argument
         * @param d the function's fourth argument
         * @param e the function's fifth argument
         * @param g the function's sixth argument
         * @return the result of applying this function
         */
        R apply(A a, B b, C c, D d, E e, G g);
    }

    /**
     * Function of arity 7.
     *
     * @param <A> the function's first argument type
     * @param <B> the function's second argument type
     * @param <C> the function's third argument type
     * @param <D> the function's fourth argument type
     * @param <E> the function's fifth argument type
     * @param <G> the function's sixth argument type
     * @param <H> the function's seventh argument type
     * @param <R> the function's return type
     */
    @FunctionalInterface
    public interface Func7<A, B, C, D, E, G, H, R> {

        /**
         * Apply this function.
         *
         * @param a the function's first argument
         * @param b the function's second argument
         * @param c the function's third argument
         * @param d the function's fourth argument
         * @param e the function's fifth argument
         * @param g the function's sixth argument
         * @param h the function's seventh argument
         * @return the result of applying this function
         */
        R apply(A a, B b, C c, D d, E e, G g, H h);

    }

    /**
     * Function of arity 8.
     *
     * @param <A> the function's first argument type
     * @param <B> the function's second argument type
     * @param <C> the function's third argument type
     * @param <D> the function's fourth argument type
     * @param <E> the function's fifth argument type
     * @param <G> the function's sixth argument type
     * @param <H> the function's seventh argument type
     * @param <I> the function's eighth argument type
     * @param <R> the function's return type
     */
    @FunctionalInterface
    public interface Func8<A, B, C, D, E, G, H, I, R> {

        /**
         * Apply this function.
         *
         * @param a the function's first argument
         * @param b the function's second argument
         * @param c the function's third argument
         * @param d the function's fourth argument
         * @param e the function's fifth argument
         * @param g the function's sixth argument
         * @param h the function's seventh argument
         * @param i the function's eighth argument
         * @return the result of applying this function
         */
        R apply(A a, B b, C c, D d, E e, G g, H h, I i);

    }
}
