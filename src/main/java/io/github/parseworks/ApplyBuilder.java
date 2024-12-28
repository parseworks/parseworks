package io.github.parseworks;



import java.util.function.BiFunction;
import java.util.function.Function;

public class ApplyBuilder {

    public static class ApplyBuilder2<I, A, B> {
        private final Parser<I, A> pa;
        private final Parser<I, B> pb;

        ApplyBuilder2(Parser<I, A> pa, Parser<I, B> pb) {
            this.pa = pa;
            this.pb = pb;
        }

        public <R> Parser<I, R> map(Function<A, Function<B, R>> f) {
            return new Parser<>(() -> pa.acceptsEOF() && pb.acceptsEOF(), in ->
                    Trampoline.more(() -> pa.apply(in).flatMap(ra ->
                            Trampoline.more(() -> pb.apply(ra.next()).map(rb ->
                                    Result.success(f.apply(ra.getOrThrow()).apply(rb.getOrThrow()), rb.next())
                            ))
                    ))
            );
        }

        public <R> Parser<I, R> map(BiFunction<A, B, R> f) {
            return map(a -> b -> f.apply(a, b));
        }

        public <C> ApplyBuilder2<I, A, B> andL(Parser<I, C> pc) {
            return new ApplyBuilder2<>(pa, pb.andL(pc));
        }

        public <C> ApplyBuilder2<I, A, B> skipRight(Parser<I, C> pc) {
            return new ApplyBuilder2<>(pa, pb.andL(pc));
        }

        public <C> ApplyBuilder3<C> and(Parser<I, C> pc) {
            return new ApplyBuilder3<>(pc);
        }

        public class ApplyBuilder3<C> {
            private final Parser<I, C> pc;

            private ApplyBuilder3(Parser<I, C> pc) {
                this.pc = pc;
            }

            public <R> Parser<I, R> map(Function<A, Function<B, Function<C, R>>> f) {
                return new Parser<>(() -> pa.acceptsEOF() && pb.acceptsEOF() && pc.acceptsEOF(), in ->
                        Trampoline.more(() -> pa.apply(in).flatMap(ra ->
                                Trampoline.more(() -> pb.apply(ra.next()).flatMap(rb ->
                                        Trampoline.more(() -> pc.apply(rb.next()).map(rc ->
                                                Result.success(f.apply(ra.getOrThrow()).apply(rb.getOrThrow()).apply(rc.getOrThrow()), rc.next())
                                        ))
                                ))
                        ))
                );
            }

            public <R> Parser<I, R> map(Functions.Func3<A, B, C, R> f) {
                return map(a -> b -> c -> f.apply(a, b, c));
            }

            public <D> ApplyBuilder3<C> andL(Parser<I, D> pd) {
                return new ApplyBuilder3<>(pc.andL(pd));
            }

            public <D> ApplyBuilder4<D> and(Parser<I, D> pd) {
                return new ApplyBuilder4<>(pd);
            }

            public class ApplyBuilder4<D> {
                private final Parser<I, D> pd;

                private ApplyBuilder4(Parser<I, D> pd) {
                    this.pd = pd;
                }

                public <R> Parser<I, R> map(Function<A, Function<B, Function<C, Function<D, R>>>> f) {
                    return new Parser<>(() -> pa.acceptsEOF() && pb.acceptsEOF() && pc.acceptsEOF() && pd.acceptsEOF(), in ->
                            Trampoline.more(() -> pa.apply(in).flatMap(ra ->
                                    Trampoline.more(() -> pb.apply(ra.next()).flatMap(rb ->
                                            Trampoline.more(() -> pc.apply(rb.next()).flatMap(rc ->
                                                    Trampoline.more(() -> pd.apply(rc.next()).map(rd ->
                                                            Result.success(f.apply(ra.getOrThrow()).apply(rb.getOrThrow()).apply(rc.getOrThrow()).apply(rd.getOrThrow()), rd.next())
                                                    ))
                                            ))
                                    ))
                            ))
                    );
                }

                public <R> Parser<I, R> map(Functions.Func4<A, B, C, D, R> f) {
                    return map(a -> b -> c -> d -> f.apply(a, b, c, d));
                }

                public <E> ApplyBuilder4<D> andL(Parser<I, E> pe) {
                    return new ApplyBuilder4<>(pd.andL(pe));
                }

                public <E> ApplyBuilder5<E> and(Parser<I, E> pe) {
                    return new ApplyBuilder5<>(pe);
                }

                public class ApplyBuilder5<E> {
                    private final Parser<I, E> pe;

                    private ApplyBuilder5(Parser<I, E> pe) {
                        this.pe = pe;
                    }

                    public <R> Parser<I, R> map(Function<A, Function<B, Function<C, Function<D, Function<E, R>>>>> f) {
                        return new Parser<>(() -> pa.acceptsEOF() && pb.acceptsEOF() && pc.acceptsEOF() && pd.acceptsEOF() && pe.acceptsEOF(), in ->
                                Trampoline.more(() -> pa.apply(in).flatMap(ra ->
                                        Trampoline.more(() -> pb.apply(ra.next()).flatMap(rb ->
                                                Trampoline.more(() -> pc.apply(rb.next()).flatMap(rc ->
                                                        Trampoline.more(() -> pd.apply(rc.next()).flatMap(rd ->
                                                                Trampoline.more(() -> pe.apply(rd.next()).map(re ->
                                                                        Result.success(f.apply(ra.getOrThrow()).apply(rb.getOrThrow()).apply(rc.getOrThrow()).apply(rd.getOrThrow()).apply(re.getOrThrow()), re.next())
                                                                ))
                                                        ))
                                                ))
                                        ))
                                ))
                        );
                    }

                    public <R> Parser<I, R> map(Functions.Func5<A, B, C, D, E, R> f) {
                        return map(a -> b -> c -> d -> e -> f.apply(a, b, c, d, e));
                    }

                    public <G> ApplyBuilder5<E> andL(Parser<I, G> pg) {
                        return new ApplyBuilder5<>(pe.andL(pg));
                    }

                    public <G> ApplyBuilder6<G> and(Parser<I, G> pg) {
                        return new ApplyBuilder6<>(pg);
                    }

                    public class ApplyBuilder6<G> {
                        private final Parser<I, G> pg;

                        private ApplyBuilder6(Parser<I, G> pg) {
                            this.pg = pg;
                        }

                        public <R> Parser<I, R> map(Function<A, Function<B, Function<C, Function<D, Function<E, Function<G, R>>>>>> f) {
                            return new Parser<>(() -> pa.acceptsEOF() && pb.acceptsEOF() && pc.acceptsEOF() && pd.acceptsEOF() && pe.acceptsEOF() && pg.acceptsEOF(), in ->
                                    Trampoline.more(() -> pa.apply(in).flatMap(ra ->
                                            Trampoline.more(() -> pb.apply(ra.next()).flatMap(rb ->
                                                    Trampoline.more(() -> pc.apply(rb.next()).flatMap(rc ->
                                                            Trampoline.more(() -> pd.apply(rc.next()).flatMap(rd ->
                                                                    Trampoline.more(() -> pe.apply(rd.next()).flatMap(re ->
                                                                            Trampoline.more(() -> pg.apply(re.next()).map(rg ->
                                                                                    Result.success(f.apply(ra.getOrThrow()).apply(rb.getOrThrow()).apply(rc.getOrThrow()).apply(rd.getOrThrow()).apply(re.getOrThrow()).apply(rg.getOrThrow()), rg.next())
                                                                            ))
                                                                    ))
                                                            ))
                                                    ))
                                            ))
                                    ))
                            );
                        }

                        public <R> Parser<I, R> map(Functions.Func6<A, B, C, D, E, G, R> f) {
                            return map(a -> b -> c -> d -> e -> g -> f.apply(a, b, c, d, e, g));
                        }

                        public <H> ApplyBuilder6<G> andL(Parser<I, H> ph) {
                            return new ApplyBuilder6<>(pg.andL(ph));
                        }

                        public <H> ApplyBuilder7<H> and(Parser<I, H> ph) {
                            return new ApplyBuilder7<>(ph);
                        }

                        public class ApplyBuilder7<H> {
                            private final Parser<I, H> ph;

                            private ApplyBuilder7(Parser<I, H> ph) {
                                this.ph = ph;
                            }

                            public <R> Parser<I, R> map(Function<A, Function<B, Function<C, Function<D, Function<E, Function<G, Function<H, R>>>>>>> f) {
                                return new Parser<>(() -> pa.acceptsEOF() && pb.acceptsEOF() && pc.acceptsEOF() && pd.acceptsEOF() && pe.acceptsEOF() && pg.acceptsEOF() && ph.acceptsEOF(), in ->
                                        Trampoline.more(() -> pa.apply(in).flatMap(ra ->
                                                Trampoline.more(() -> pb.apply(ra.next()).flatMap(rb ->
                                                        Trampoline.more(() -> pc.apply(rb.next()).flatMap(rc ->
                                                                Trampoline.more(() -> pd.apply(rc.next()).flatMap(rd ->
                                                                        Trampoline.more(() -> pe.apply(rd.next()).flatMap(re ->
                                                                                Trampoline.more(() -> pg.apply(re.next()).flatMap(rg ->
                                                                                        Trampoline.more(() -> ph.apply(rg.next()).map(rh ->
                                                                                                Result.success(f.apply(ra.getOrThrow()).apply(rb.getOrThrow()).apply(rc.getOrThrow()).apply(rd.getOrThrow()).apply(re.getOrThrow()).apply(rg.getOrThrow()).apply(rh.getOrThrow()), rh.next())
                                                                                        ))
                                                                                ))
                                                                        ))
                                                                ))
                                                        ))
                                                ))
                                        ))
                                );
                            }

                            public <R> Parser<I, R> map(Functions.Func7<A, B, C, D, E, G, H, R> f) {
                                return map(a -> b -> c -> d -> e -> g -> h -> f.apply(a, b, c, d, e, g, h));
                            }

                            public <J> ApplyBuilder7<H> andL(Parser<I, J> pj) {
                                return new ApplyBuilder7<>(ph.andL(pj));
                            }

                            public <J> ApplyBuilder8<J> and(Parser<I, J> pj) {
                                return new ApplyBuilder8<>(pj);
                            }

                            public class ApplyBuilder8<J> {
                                private final Parser<I, J> pj;

                                private ApplyBuilder8(Parser<I, J> pj) {
                                    this.pj = pj;
                                }

                                public <R> Parser<I, R> map(Function<A, Function<B, Function<C, Function<D, Function<E, Function<G, Function<H, Function<J, R>>>>>>>> f) {
                                    return new Parser<>(() -> pa.acceptsEOF() && pb.acceptsEOF() && pc.acceptsEOF() && pd.acceptsEOF() && pe.acceptsEOF() && pg.acceptsEOF() && ph.acceptsEOF() && pj.acceptsEOF(), in ->
                                            Trampoline.more(() -> pa.apply(in).flatMap(ra ->
                                                    Trampoline.more(() -> pb.apply(ra.next()).flatMap(rb ->
                                                            Trampoline.more(() -> pc.apply(rb.next()).flatMap(rc ->
                                                                    Trampoline.more(() -> pd.apply(rc.next()).flatMap(rd ->
                                                                            Trampoline.more(() -> pe.apply(rd.next()).flatMap(re ->
                                                                                    Trampoline.more(() -> pg.apply(re.next()).flatMap(rg ->
                                                                                            Trampoline.more(() -> ph.apply(rg.next()).flatMap(rh ->
                                                                                                    Trampoline.more(() -> pj.apply(rh.next()).map(rj ->
                                                                                                            Result.success(f.apply(ra.getOrThrow()).apply(rb.getOrThrow()).apply(rc.getOrThrow()).apply(rd.getOrThrow()).apply(re.getOrThrow()).apply(rg.getOrThrow()).apply(rh.getOrThrow()).apply(rj.getOrThrow()), rj.next())
                                                                                                    ))
                                                                                            ))
                                                                                    ))
                                                                            ))
                                                                    ))
                                                            ))
                                                    ))
                                            ))
                                    );
                                }

                                public <R> Parser<I, R> map(Functions.Func8<A, B, C, D, E, G, H, J, R> f) {
                                    return map(a -> b -> c -> d -> e -> g -> h -> j -> f.apply(a, b, c, d, e, g, h, j));
                                }

                                public <K> ApplyBuilder8<J> andL(Parser<I, K> pk) {
                                    return new ApplyBuilder8<>(pj.andL(pk));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}