package io.github.parseworks;

import io.github.parseworks.impl.parser.NoCheckParser;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * {@code ApplyBuilder} combines parsers via successive calls to {@code then} and {@code thenSkip}.
 * <p>
 * {@code ApplyBuilder} provides a fluent interface for combining parsers.
 * The left two parsers are combined by calling {@link io.github.parseworks.Parser#map Parser.map},
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
public class ApplyBuilder<I, A, B> {
    private final Parser<I, A> pa;
    private final Parser<I, B> pb;

    ApplyBuilder(Parser<I, A> pa, Parser<I, B> pb) {
        this.pa = pa;
        this.pb = pb;
    }

    /**
     * Creates a new {@code ApplyBuilder} instance with the given parsers.
     *
     * @param pa the first parser
     * @param pb the second parser
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
        return new NoCheckParser<>(in -> {
            Result<I, Function<A, B>> functionResult = functionProvider.apply(in);
            if (functionResult.isError()) {
                return functionResult.cast();
            }
            Function<A, B> func = functionResult.get();
            Input<I> in2 = functionResult.next();
            Result<I, A> valueResult = valueParser.apply(in2);
            if (valueResult.isError()) {
                return valueResult.cast();
            }
            return valueResult.map(func);
        });
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
        return new NoCheckParser<>(in -> {
            Result<I, A> ra = pa.apply(in);
            if (ra.isError()) {
                return ra.cast();
            }
            Result<I, B> rb = pb.apply(ra.next());
            if (rb.isError()) {
                return rb.cast();
            }
            return Result.success(rb.next(), f.apply(ra.get(), rb.get()));
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