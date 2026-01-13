package io.github.parseworks;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * {@code ApplyBuilder} combines parsers via successive calls to {@code then} and {@code thenSkip}.
 * <p>
 * {@code ApplyBuilder} provides a fluent interface for combining parsers.
 * The left two parsers are combined by calling {@link Parser#map Parser.map},
 * which returns an {@code ApplyBuilder} instance.
 * Each successive parser is incorporated by passing it to a call to {@code then} or {@code thenSkip}.
 * The chain of calls is concluded by calling {@code map} with a handler for the parse results.
 * <p>
 * {@code ApplyBuilder} is a more readable way of using {@link ApplyBuilder#apply Parser.ap}.
 * For example, {@code pa.then(pb).then(pc).map(f)} is equivalent to {@code ap(ap(pa.map(f), pb), pc)}.
 *
 * @param <I> the type of the input to the parsers
 * @param <A> the type of the result of the first parser
 * @param <B> the type of the result of the second parser
 * @author jason bailey
 * @version $Id: $Id
 */
public record ApplyBuilder<I, A, B>(Parser<I, A> pa, Parser<I, B> pb) {

    /**
     * Creates a new {@code ApplyBuilder} instance with the given parsers.
     *
     * @param pa  the first parser
     * @param pb  the second parser
     * @param <I> the type of the input to the parsers
     * @param <A> the type of the result of the first parser
     * @param <B> the type of the result of the second parser
     * @return a new {@code ApplyBuilder} instance with the given parsers
     */
    public static <I, A, B> ApplyBuilder<I, A, B> of(Parser<I, A> pa, Parser<I, B> pb) {
        return new ApplyBuilder<>(pa, pb);
    }

    /**
     * Applies a function provided by one parser to the result of another parser.
     *
     * @param functionProvider the parser that provides the function
     * @param valueParser      the parser that provides the value
     * @param <I>              the type of the input symbols
     * @param <A>              the type of the parsed value
     * @param <B>              the type of the result of the function
     * @return a parser that applies the function to the value
     */
    public static <I, A, B> Parser<I, B> apply(Parser<I, Function<A, B>> functionProvider, Parser<I, A> valueParser) {
        return new Parser<>(in -> {
            Result<I, Function<A, B>> functionResult = functionProvider.apply(in);
            if (!functionResult.matches()) {
                return functionResult.cast();
            }
            Function<A, B> func = functionResult.value();
            Input<I> in2 = functionResult.input();
            Result<I, A> valueResult = valueParser.apply(in2);
            if (!valueResult.matches()) {
                return Result.partial(valueResult.input(), (Failure<I, A>) valueResult).cast();
            }
            return valueResult.map(func);
        });
    }

    /**
     * Creates a parser that applies a function to the result of another parser.
     * <p>
     * The {@code apply} method creates a composite parser that first runs the input parser {@code pa}
     * and then transforms its result using the provided function {@code f}. The parsing process works
     * as follows:
     * <ol>
     *   <li>First applies the parser {@code pa} to the input</li>
     *   <li>If {@code pa} succeeds, applies the function {@code f} to its result</li>
     *   <li>Returns a new parser that produces the transformed result</li>
     * </ol>
     * <p>
     * This method is a fundamental building block for functional parser composition, enabling
     * transformation of parser results without affecting the parsing logic. It implements the
     * applicative functor pattern, allowing functions to be applied to values inside a parser context.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Uses {@link Parser#pure(Object)} to lift the function into a parser context</li>
     *   <li>Delegates to {@link ApplyBuilder#apply(Parser, Parser)} for the actual application</li>
     *   <li>Preserves the original parsing behavior but transforms the result type</li>
     *   <li>Input consumption depends solely on the behavior of parser {@code pa}</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // A parser that recognizes integers
     * Parser<Character, Integer> intParser = intr;
     *
     * // A function that doubles a number
     * Function<Integer, Integer> doubleIt = n -> n * 2;
     *
     * // Create a parser that recognizes integers and doubles them
     * Parser<Character, Integer> doubledInt = Parser.apply(doubleIt, intParser);
     *
     * // Succeeds with 84 for input "42"
     * // Fails for input "abc" (not an integer)
     * }</pre>
     *
     * @param f   the function to apply to the parser's result
     * @param pa  the parser that provides the value to transform
     * @param <I> the type of the input symbols
     * @param <A> the type of the original parser's result
     * @param <B> the type of the transformed result
     * @return a parser that applies the function to the result of the input parser
     * @see Parser#pure(Object) for creating a parser that always returns a constant value
     * @see #apply(Parser, Object) for applying a parser-provided function to a constant value
     * @see Parser#map(Function) for a similar operation that directly maps a parser's result
     */
    public static <I, A, B> Parser<I, B> apply(Function<A, B> f, Parser<I, A> pa) {
        return ApplyBuilder.apply(Parser.pure(f), pa);
    }

    /**
     * Creates a parser that applies a function produced by one parser to a constant value.
     * <p>
     * The {@code apply} method creates a composite parser that first runs the function-producing
     * parser {@code pf} and then applies the resulting function to the constant value {@code a}.
     * The parsing process works as follows:
     * <ol>
     *   <li>First applies the parser {@code pf} to the input to obtain a function</li>
     *   <li>If {@code pf} succeeds, applies the parsed function to the constant value {@code a}</li>
     *   <li>Returns a new parser that produces the transformed result</li>
     * </ol>
     * <p>
     * This method is the dual of {@link #apply(Function, Parser)}, implementing the applicative
     * functor pattern for parsers. It enables combining parsers with fixed values, which is
     * useful for parameterizing parsers with external data.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Uses {@link Parser#pure(Object)} to lift the constant value into a parser context</li>
     *   <li>Delegates to {@link ApplyBuilder#apply(Parser, Parser)} for the actual application</li>
     *   <li>The input consumption depends solely on the behavior of parser {@code pf}</li>
     *   <li>The constant value is never parsed from the input</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // A parser that recognizes a function symbol and returns a math function
     * Parser<Character, Function<Integer, Integer>> opParser =
     *     chr('+').as(n -> n + 1)
     *     .or(chr('-').as(n -> n - 1))
     *     .or(chr('*').as(n -> n * 2))
     *     .or(chr('/').as(n -> n / 2));
     *
     * // Apply that function to a constant value
     * Parser<Character, Integer> appliedToTen = Parser.apply(opParser, 10);
     *
     * // Succeeds with 11 for input "+"
     * // Succeeds with 9 for input "-"
     * // Succeeds with 20 for input "*"
     * // Succeeds with 5 for input "/"
     * // Fails for input "^" (not a recognized operator)
     * }</pre>
     *
     * @param pf  the parser that provides the function to apply
     * @param a   the constant value to which the function will be applied
     * @param <I> the type of the input symbols
     * @param <A> the type of the constant value
     * @param <B> the type of the result after applying the function
     * @return a parser that applies the parsed function to the constant value
     * @see #apply(Function, Parser) for applying a constant function to a parser's result
     * @see Parser#pure(Object) for creating a parser that always returns a constant value
     * @see ApplyBuilder for combining multiple parsers with functions
     */
    public static <I, A, B> Parser<I, B> apply(Parser<I, Function<A, B>> pf, A a) {
        return ApplyBuilder.apply(pf, Parser.pure(a));
    }

    /**
     * Maps the results of the parsers to a new result using the provided function.
     *
     * @param f   the function to map the results
     * @param <R> the result type
     * @return a new parser with the mapped result
     */
    public <R> Parser<I, R> map(Function<A, Function<B, R>> f) {
        return apply(pa.map(f), pb);
    }

    /**
     * Maps the results of the parsers to a new result using the provided bi-function.
     *
     * @param f   the bi-function to map the results
     * @param <R> the result type
     * @return a new parser with the mapped result
     */
    public <R> Parser<I, R> map(BiFunction<A, B, R> f) {
        return new Parser<>(in -> {
            Result<I, A> ra = pa.apply(in);
            if (!ra.matches()) {
                return ra.cast();
            }
            Result<I, B> rb = pb.apply(ra.input());
            if (!rb.matches()) {
                return Result.partial(rb.input(), (Failure<I, B>) rb).cast();
            }
            return Result.success(rb.input(), f.apply(ra.value(), rb.value()));
        });
    }

    /**
     * Adds a parser to be skipped after the current parser.
     *
     * @param pc  the parser to be skipped
     * @param <C> the type of the skipped parser's result
     * @return a new {@code ApplyBuilder} instance with the skipped parser
     */
    public <C> ApplyBuilder<I, A, B> thenSkip(Parser<I, C> pc) {
        return new ApplyBuilder<>(pa, pb.thenSkip(pc));
    }

    /**
     * Adds a parser to be applied after the current parser.
     *
     * @param pc  the parser to be applied
     * @param <C> the type of the next parser's result
     * @return a new {@code ApplyBuilder} instance with the next parser
     */
    public <C> ApplyBuilder<I, C, B> skipThen(Parser<I, C> pc) {
        return new ApplyBuilder<>(pa.skipThen(pc), pb);
    }

    /**
     * Adds a parser to be applied after the current parser.
     *
     * @param pc  the parser to be applied
     * @param <C> the type of the next parser's result
     * @return a new {@code ApplyBuilder3} instance with the next parser
     */
    public <C> ApplyBuilder3<C> then(Parser<I, C> pc) {
        return new ApplyBuilder3<>(pc);
    }

    /**
     * {@code ApplyBuilder3} is a builder class for combining parsers with three levels of parsing.
     * <p>
     * This class allows for the sequential combination of parsers, where each parser can be followed
     * by another parser or skipped. The results of the parsers can be mapped to a new result using
     * a function that takes three arguments.
     *
     * @param <C> the type of the third parser's result
     */
    public class ApplyBuilder3<C> {
        private final Parser<I, C> pc;

        private ApplyBuilder3(Parser<I, C> pc) {
            this.pc = pc;
        }

        /**
         * Maps the results of the parsers to a new result using the provided function.
         *
         * @param f   the function to map the results
         * @param <R> the result type
         * @return a new parser with the mapped result
         */
        public <R> Parser<I, R> map(Function<A, Function<B, Function<C, R>>> f) {
            return apply(ApplyBuilder.this.map(f), pc);
        }

        /**
         * Maps the results of the parsers to a new result using the provided function with three arguments.
         *
         * @param f   the function to map the results
         * @param <R> the result type
         * @return a new parser with the mapped result
         */
        public <R> Parser<I, R> map(Functions.Func3<A, B, C, R> f) {
            return map(a -> b -> c -> f.apply(a, b, c));
        }

        /**
         * Adds a parser to be skipped after the current parser.
         *
         * @param pd  the parser to be skipped
         * @param <D> the type of the skipped parser's result
         * @return a new {@code ApplyBuilder3} instance with the skipped parser
         */
        public <D> ApplyBuilder3<C> thenSkip(Parser<I, D> pd) {
            return new ApplyBuilder3<>(pc.thenSkip(pd));
        }

        /**
         * Adds a parser to be applied after the current parser.
         *
         * @param pd  the parser to be applied
         * @param <D> the type of the next parser's result
         * @return a new {@code ApplyBuilder3} instance with the next parser
         */
        public <D> ApplyBuilder3<D> skipThen(Parser<I, D> pd) {
            return new ApplyBuilder3<>(pc.skipThen(pd));
        }

        /**
         * Adds a parser to be applied after the current parser.
         *
         * @param pd  the parser to be applied
         * @param <D> the type of the next parser's result
         * @return a new {@code ApplyBuilder4} instance with the next parser
         */
        public <D> ApplyBuilder4<D> then(Parser<I, D> pd) {
            return new ApplyBuilder4<>(pd);
        }

        /**
         * {@code ApplyBuilder4} is a builder class for combining parsers with four levels of parsing.
         * <p>
         * This class allows for the sequential combination of parsers, where each parser can be followed
         * by another parser or skipped. The results of the parsers can be mapped to a new result using
         * a function that takes four arguments.
         *
         * @param <D> the type of the fourth parser's result
         */
        public class ApplyBuilder4<D> {
            private final Parser<I, D> pd;

            private ApplyBuilder4(Parser<I, D> pd) {
                this.pd = pd;
            }

            /**
             * Maps the results of the parsers to a new result using the provided function.
             *
             * @param f   the function to map the results
             * @param <R> the result type
             * @return a new parser with the mapped result
             */
            public <R> Parser<I, R> map(Function<A, Function<B, Function<C, Function<D, R>>>> f) {
                return apply(ApplyBuilder3.this.map(f), pd);
            }

            /**
             * Maps the results of the parsers to a new result using the provided function with four arguments.
             *
             * @param f   the function to map the results
             * @param <R> the result type
             * @return a new parser with the mapped result
             */
            public <R> Parser<I, R> map(Functions.Func4<A, B, C, D, R> f) {
                return map(a -> b -> c -> d -> f.apply(a, b, c, d));
            }

            /**
             * Adds a parser to be skipped after the current parser.
             *
             * @param pe  the parser to be skipped
             * @param <E> the type of the skipped parser's result
             * @return a new {@code ApplyBuilder4} instance with the skipped parser
             */
            public <E> ApplyBuilder4<D> thenSkip(Parser<I, E> pe) {
                return new ApplyBuilder4<>(pd.thenSkip(pe));
            }

            /**
             * Adds a parser to be applied after the current parser.
             *
             * @param pe  the parser to be applied
             * @param <E> the type of the next parser's result
             * @return a new {@code ApplyBuilder4} instance with the next parser
             */
            public <E> ApplyBuilder4<E> skipThen(Parser<I, E> pe) {
                return new ApplyBuilder4<>(pd.skipThen(pe));
            }

            /**
             * Adds a parser to be applied after the current parser.
             *
             * @param pe  the parser to be applied
             * @param <E> the type of the next parser's result
             * @return a new {@code ApplyBuilder5} instance with the next parser
             */
            public <E> ApplyBuilder5<E> then(Parser<I, E> pe) {
                return new ApplyBuilder5<>(pe);
            }

            /**
             * {@code ApplyBuilder5} is a builder class for combining parsers with five levels of parsing.
             * <p>
             * This class allows for the sequential combination of parsers, where each parser can be followed
             * by another parser or skipped. The results of the parsers can be mapped to a new result using
             * a function that takes five arguments.
             *
             * @param <E> the type of the fifth parser's result
             */
            public class ApplyBuilder5<E> {
                private final Parser<I, E> pe;

                private ApplyBuilder5(Parser<I, E> pe) {
                    this.pe = pe;
                }

                /**
                 * Maps the results of the parsers to a new result using the provided function.
                 *
                 * @param f   the function to map the results
                 * @param <R> the result type
                 * @return a new parser with the mapped result
                 */
                public <R> Parser<I, R> map(Function<A, Function<B, Function<C, Function<D, Function<E, R>>>>> f) {
                    return apply(ApplyBuilder4.this.map(f), pe);
                }

                /**
                 * Maps the results of the parsers to a new result using the provided function with five arguments.
                 *
                 * @param f   the function to map the results
                 * @param <R> the result type
                 * @return a new parser with the mapped result
                 */
                public <R> Parser<I, R> map(Functions.Func5<A, B, C, D, E, R> f) {
                    return map(a -> b -> c -> d -> e -> f.apply(a, b, c, d, e));
                }

                /**
                 * Adds a parser to be skipped after the current parser.
                 *
                 * @param pg  the parser to be skipped
                 * @param <G> the type of the skipped parser's result
                 * @return a new {@code ApplyBuilder5} instance with the skipped parser
                 */
                public <G> ApplyBuilder5<E> thenSkip(Parser<I, G> pg) {
                    return new ApplyBuilder5<>(pe.thenSkip(pg));
                }

                /**
                 * Adds a parser to be applied after the current parser.
                 *
                 * @param pg  the parser to be applied
                 * @param <G> the type of the next parser's result
                 * @return a new {@code ApplyBuilder5} instance with the next parser
                 */
                public <G> ApplyBuilder5<G> skipThen(Parser<I, G> pg) {
                    return new ApplyBuilder5<>(pe.skipThen(pg));
                }

                /**
                 * Adds a parser to be applied after the current parser.
                 *
                 * @param pg  the parser to be applied
                 * @param <G> the type of the next parser's result
                 * @return a new {@code ApplyBuilder6} instance with the next parser
                 */
                public <G> ApplyBuilder6<G> then(Parser<I, G> pg) {
                    return new ApplyBuilder6<>(pg);
                }

                /**
                 * {@code ApplyBuilder6} is a builder class for combining parsers with six levels of parsing.
                 * <p>
                 * This class allows for the sequential combination of parsers, where each parser can be followed
                 * by another parser or skipped. The results of the parsers can be mapped to a new result using
                 * a function that takes six arguments.
                 *
                 * @param <G> the type of the sixth parser's result
                 */
                public class ApplyBuilder6<G> {
                    private final Parser<I, G> pg;

                    private ApplyBuilder6(Parser<I, G> pg) {
                        this.pg = pg;
                    }

                    /**
                     * Maps the results of the parsers to a new result using the provided function.
                     *
                     * @param f   the function to map the results
                     * @param <R> the result type
                     * @return a new parser with the mapped result
                     */
                    public <R> Parser<I, R> map(Function<A, Function<B, Function<C, Function<D, Function<E, Function<G, R>>>>>> f) {
                        return apply(ApplyBuilder5.this.map(f), pg);
                    }

                    /**
                     * Maps the results of the parsers to a new result using the provided function with six arguments.
                     *
                     * @param f   the function to map the results
                     * @param <R> the result type
                     * @return a new parser with the mapped result
                     */
                    public <R> Parser<I, R> map(Functions.Func6<A, B, C, D, E, G, R> f) {
                        return map(a -> b -> c -> d -> e -> g -> f.apply(a, b, c, d, e, g));
                    }

                    /**
                     * Adds a parser to be skipped after the current parser.
                     *
                     * @param ph  the parser to be skipped
                     * @param <H> the type of the skipped parser's result
                     * @return a new {@code ApplyBuilder6} instance with the skipped parser
                     */
                    public <H> ApplyBuilder6<G> thenSkip(Parser<I, H> ph) {
                        return new ApplyBuilder6<>(pg.thenSkip(ph));
                    }

                    /**
                     * Adds a parser to be applied after the current parser.
                     *
                     * @param ph  the parser to be applied
                     * @param <H> the type of the next parser's result
                     * @return a new {@code ApplyBuilder6} instance with the next parser
                     */
                    public <H> ApplyBuilder6<H> skipThen(Parser<I, H> ph) {
                        return new ApplyBuilder6<>(pg.skipThen(ph));
                    }

                    /**
                     * Adds a parser to be applied after the current parser.
                     *
                     * @param ph  the parser to be applied
                     * @param <H> the type of the next parser's result
                     * @return a new {@code ApplyBuilder7} instance with the next parser
                     */
                    public <H> ApplyBuilder7<H> then(Parser<I, H> ph) {
                        return new ApplyBuilder7<>(ph);
                    }

                    /**
                     * {@code ApplyBuilder7} is a builder class for combining parsers with seven levels of parsing.
                     * <p>
                     * This class allows for the sequential combination of parsers, where each parser can be followed
                     * by another parser or skipped. The results of the parsers can be mapped to a new result using
                     * a function that takes seven arguments.
                     *
                     * @param <H> the type of the seventh parser's result
                     */
                    public class ApplyBuilder7<H> {
                        private final Parser<I, H> ph;

                        private ApplyBuilder7(Parser<I, H> ph) {
                            this.ph = ph;
                        }

                        /**
                         * Maps the results of the parsers to a new result using the provided function.
                         *
                         * @param f   the function to map the results
                         * @param <R> the result type
                         * @return a new parser with the mapped result
                         */
                        public <R> Parser<I, R> map(Function<A, Function<B, Function<C, Function<D, Function<E, Function<G, Function<H, R>>>>>>> f) {
                            return apply(ApplyBuilder6.this.map(f), ph);
                        }

                        /**
                         * Maps the results of the parsers to a new result using the provided function with seven arguments.
                         *
                         * @param f   the function to map the results
                         * @param <R> the result type
                         * @return a new parser with the mapped result
                         */
                        public <R> Parser<I, R> map(Functions.Func7<A, B, C, D, E, G, H, R> f) {
                            return map(a -> b -> c -> d -> e -> g -> h -> f.apply(a, b, c, d, e, g, h));
                        }

                        /**
                         * Adds a parser to be skipped after the current parser.
                         *
                         * @param pj  the parser to be skipped
                         * @param <J> the type of the skipped parser's result
                         * @return a new {@code ApplyBuilder7} instance with the skipped parser
                         */
                        public <J> ApplyBuilder7<H> thenSkip(Parser<I, J> pj) {
                            return new ApplyBuilder7<>(ph.thenSkip(pj));
                        }

                        /**
                         * Adds a parser to be applied after the current parser.
                         *
                         * @param pj  the parser to be applied
                         * @param <J> the type of the next parser's result
                         * @return a new {@code ApplyBuilder7} instance with the next parser
                         */
                        public <J> ApplyBuilder7<J> skipThen(Parser<I, J> pj) {
                            return new ApplyBuilder7<>(ph.skipThen(pj));
                        }

                        /**
                         * Adds a parser to be applied after the current parser.
                         *
                         * @param pj  the parser to be applied
                         * @param <J> the type of the next parser's result
                         * @return a new {@code ApplyBuilder8} instance with the next parser
                         */
                        public <J> ApplyBuilder8<J> then(Parser<I, J> pj) {
                            return new ApplyBuilder8<>(pj);
                        }

                        /**
                         * {@code ApplyBuilder8} is a builder class for combining parsers with eight levels of parsing.
                         * <p>
                         * This class allows for the sequential combination of parsers, where each parser can be followed
                         * by another parser or skipped. The results of the parsers can be mapped to a new result using
                         * a function that takes eight arguments.</p>
                         *
                         * @param <J> the type of the eighth parser's result
                         */
                        public class ApplyBuilder8<J> {
                            private final Parser<I, J> pj;

                            private ApplyBuilder8(Parser<I, J> pj) {
                                this.pj = pj;
                            }

                            /**
                             * Maps the results of the parsers to a new result using the provided function.
                             *
                             * @param f   the function to map the results
                             * @param <R> the result type
                             * @return a new parser with the mapped result
                             */
                            public <R> Parser<I, R> map(Function<A, Function<B, Function<C, Function<D, Function<E, Function<G, Function<H, Function<J, R>>>>>>>> f) {
                                return apply(ApplyBuilder7.this.map(f), pj);
                            }

                            /**
                             * Maps the results of the parsers to a new result using the provided function with eight arguments.
                             *
                             * @param f   the function to map the results
                             * @param <R> the result type
                             * @return a new parser with the mapped result
                             */
                            public <R> Parser<I, R> map(Functions.Func8<A, B, C, D, E, G, H, J, R> f) {
                                return map(a -> b -> c -> d -> e -> g -> h -> j -> f.apply(a, b, c, d, e, g, h, j));
                            }

                            /**
                             * Adds a parser to be skipped after the current parser.
                             *
                             * @param pk  the parser to be skipped
                             * @param <K> the type of the skipped parser's result
                             * @return a new {@code ApplyBuilder8} instance with the skipped parser
                             */
                            public <K> ApplyBuilder8<J> thenSkip(Parser<I, K> pk) {
                                return new ApplyBuilder8<>(pj.thenSkip(pk));
                            }

                            /**
                             * Adds a parser to be applied after the current parser.
                             *
                             * @param pk  the parser to be applied
                             * @param <K> the type of the next parser's result
                             * @return a new {@code ApplyBuilder8} instance with the next parser
                             */
                            public <K> ApplyBuilder8<K> skipThen(Parser<I, K> pk) {
                                return new ApplyBuilder8<>(pj.skipThen(pk));
                            }
                        }
                    }
                }
            }
        }
    }
}