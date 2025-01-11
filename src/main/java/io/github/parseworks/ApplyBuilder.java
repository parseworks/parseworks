package io.github.parseworks;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * {@code ApplyBuilder} combines parsers via successive calls to {@code and} and {@code andL}.
 * <p>
 * {@code ApplyBuilder} provides a fluent interface for combining parsers.
 * The left two parsers are combined by a calling {@link Parser#map Parser.and},
 * which returns an {@code ApplyBuilder} instance.
 * Each successive parser is incorporated by passing it to a call to {@code and} or {@code andL}.
 * The chain of calls is concluded by calling {@code map} with a handler for the parse results.
 * <p>
 * ApplyBuilder is a more readable way of using {@link Parser#ap Parser.ap}.
 * For example, {@code pa.and(pb).and(pc).map(f)} is equivalent to {@code ap(ap(ap(pa.map(f), pb), pc), pd)}.
 */
public class ApplyBuilder<I, A, B> {
    private final Parser<I, A> pa;
    private final Parser<I, B> pb;

    ApplyBuilder(Parser<I, A> pa, Parser<I, B> pb) {
        this.pa = pa;
        this.pb = pb;
    }

    public static <I, A, B> ApplyBuilder<I, A, B> of(Parser<I, A> pa, Parser<I, B> pb) {
        return new ApplyBuilder<>(pa, pb);
    }

    /**
     * Maps the results of the parsers to a new result using the provided function.
     *
     * @param f   the function to map the results
     * @param <R> the result type
     * @return a new parser with the mapped result
     */
    public <R> Parser<I, R> map(Function<A, Function<B, R>> f) {
        return Parser.ap(pa.map(f), pb);
    }

    /**
     * Maps the results of the parsers to a new result using the provided bi-function.
     *
     * @param f   the bi-function to map the results
     * @param <R> the result type
     * @return a new parser with the mapped result
     */
    public <R> Parser<I, R> map(BiFunction<A, B, R> f) {
        return map(a -> b -> f.apply(a, b));
    }


    public <C> ApplyBuilder<I, A, B> andL(Parser<I, C> pc) {
        return new ApplyBuilder<>(pa, pb.thenSkip(pc));
    }

    public <C> ApplyBuilder<I, A, B> skipRight(Parser<I, C> pc) {
        return new ApplyBuilder<>(pa, pb.thenSkip(pc));
    }

    public <C> ApplyBuilder3<C> then(Parser<I, C> pc) {
        return new ApplyBuilder3<>(pc);
    }

    public class ApplyBuilder3<C> {
        private final Parser<I, C> pc;

        private ApplyBuilder3(Parser<I, C> pc) {
            this.pc = pc;
        }

        public <R> Parser<I, R> map(Function<A, Function<B, Function<C, R>>> f) {
            return Parser.ap(ApplyBuilder.this.map(f), pc);
        }

        public <R> Parser<I, R> map(Functions.Func3<A, B, C, R> f) {
            return map(a -> b -> c -> f.apply(a, b, c));
        }

        public <D> ApplyBuilder3<C> andL(Parser<I, D> pd) {
            return new ApplyBuilder3<>(pc.thenSkip(pd));
        }

        public <D> ApplyBuilder4<D> then(Parser<I, D> pd) {
            return new ApplyBuilder4<>(pd);
        }

        public class ApplyBuilder4<D> {
            private final Parser<I, D> pd;

            private ApplyBuilder4(Parser<I, D> pd) {
                this.pd = pd;
            }

            public <R> Parser<I, R> map(Function<A, Function<B, Function<C, Function<D, R>>>> f) {
                return Parser.ap(ApplyBuilder3.this.map(f), pd);
            }

            public <R> Parser<I, R> map(Functions.Func4<A, B, C, D, R> f) {
                return map(a -> b -> c -> d -> f.apply(a, b, c, d));
            }

            public <E> ApplyBuilder4<D> andL(Parser<I, E> pe) {
                return new ApplyBuilder4<>(pd.thenSkip(pe));
            }

            public <E> ApplyBuilder5<E> then(Parser<I, E> pe) {
                return new ApplyBuilder5<>(pe);
            }

            public class ApplyBuilder5<E> {
                private final Parser<I, E> pe;

                private ApplyBuilder5(Parser<I, E> pe) {
                    this.pe = pe;
                }

                public <R> Parser<I, R> map(Function<A, Function<B, Function<C, Function<D, Function<E, R>>>>> f) {
                    return Parser.ap(ApplyBuilder3.ApplyBuilder4.this.map(f), pe);
                }

                public <R> Parser<I, R> map(Functions.Func5<A, B, C, D, E, R> f) {
                    return map(a -> b -> c -> d -> e -> f.apply(a, b, c, d, e));
                }

                public <G> ApplyBuilder5<E> andL(Parser<I, G> pg) {
                    return new ApplyBuilder5<>(pe.thenSkip(pg));
                }

                public <G> ApplyBuilder6<G> then(Parser<I, G> pg) {
                    return new ApplyBuilder6<>(pg);
                }

                public class ApplyBuilder6<G> {
                    private final Parser<I, G> pg;

                    private ApplyBuilder6(Parser<I, G> pg) {
                        this.pg = pg;
                    }

                    public <R> Parser<I, R> map(Function<A, Function<B, Function<C, Function<D, Function<E, Function<G, R>>>>>> f) {
                        return Parser.ap(ApplyBuilder3.ApplyBuilder4.ApplyBuilder5.this.map(f), pg);
                    }

                    public <R> Parser<I, R> map(Functions.Func6<A, B, C, D, E, G, R> f) {
                        return map(a -> b -> c -> d -> e -> g -> f.apply(a, b, c, d, e, g));
                    }

                    public <H> ApplyBuilder6<G> andL(Parser<I, H> ph) {
                        return new ApplyBuilder6<>(pg.thenSkip(ph));
                    }

                    public <H> ApplyBuilder7<H> then(Parser<I, H> ph) {
                        return new ApplyBuilder7<>(ph);
                    }

                    public class ApplyBuilder7<H> {
                        private final Parser<I, H> ph;

                        private ApplyBuilder7(Parser<I, H> ph) {
                            this.ph = ph;
                        }

                        public <R> Parser<I, R> map(Function<A, Function<B, Function<C, Function<D, Function<E, Function<G, Function<H, R>>>>>>> f) {
                            return Parser.ap(ApplyBuilder3.ApplyBuilder4.ApplyBuilder5.ApplyBuilder6.this.map(f), ph);
                        }

                        public <R> Parser<I, R> map(Functions.Func7<A, B, C, D, E, G, H, R> f) {
                            return map(a -> b -> c -> d -> e -> g -> h -> f.apply(a, b, c, d, e, g, h));
                        }

                        public <J> ApplyBuilder7<H> andL(Parser<I, J> pj) {
                            return new ApplyBuilder7<>(ph.thenSkip(pj));
                        }

                        public <J> ApplyBuilder8<J> then(Parser<I, J> pj) {
                            return new ApplyBuilder8<>(pj);
                        }

                        public class ApplyBuilder8<J> {
                            private final Parser<I, J> pj;

                            private ApplyBuilder8(Parser<I, J> pj) {
                                this.pj = pj;
                            }

                            public <R> Parser<I, R> map(Function<A, Function<B, Function<C, Function<D, Function<E, Function<G, Function<H, Function<J, R>>>>>>>> f) {
                                return Parser.ap(ApplyBuilder7.this.map(f), pj);
                            }

                            public <R> Parser<I, R> map(Functions.Func8<A, B, C, D, E, G, H, J, R> f) {
                                return map(a -> b -> c -> d -> e -> g -> h -> j -> f.apply(a, b, c, d, e, g, h, j));
                            }

                            public <K> ApplyBuilder8<J> andL(Parser<I, K> pk) {
                                return new ApplyBuilder8<>(pj.thenSkip(pk));
                            }
                        }
                    }
                }
            }
        }
    }
}
