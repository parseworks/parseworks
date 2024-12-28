package io.github.parseworks;


import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.parseworks.Utils.LTRUE;
import static io.github.parseworks.Utils.failureEof;


public class Parser<I, A> {

    Supplier<Boolean> acceptsEmpty;
    Function<Input<I>, Trampoline<Result<I, A>>> applyHandler;

    public Parser(Supplier<Boolean> acceptsEmpty, Function<Input<I>, Trampoline<Result<I, A>>> applyHandler) {
        this.acceptsEmpty = acceptsEmpty;
        this.applyHandler = applyHandler;
    }

    public Parser(Supplier<Boolean> acceptsEmpty) {
        this(acceptsEmpty, in -> Trampoline.done(Utils.failure(in)));
    }

    public Parser(Function<Input<I>, Trampoline<Result<I, A>>> applyHandler) {
        this(() -> false, applyHandler);
    }

    public Parser() {
    }

    public Result<I, A> parse(Input<I> in) {
        final Parser<I, A> parserAndEof = this.andL(Combinators.eof());
        if (acceptsEOF()) {
            return parserAndEof.apply(in).get();
        } else if (in.isEof()) {
            return failureEof(this, in);
        }
        return parserAndEof.apply(in).get();
    }

    public boolean acceptsEOF() {
        if (applyHandler == null) {
            throw new RuntimeException("Uninitialised Parser");
        }
        return acceptsEmpty.get();
    }

    public Trampoline<Result<I, A>> apply(Input<I> in) {
        if (applyHandler == null) {
            throw new RuntimeException("Uninitialised Parser");
        }
        return applyHandler.apply(in);
    }

    public <B> Parser<I, Unit> not(Parser<I, B> p) {
        return new Parser<>(in -> {
            Result<I, B> result = p.apply(in).get();
            if (result.isSuccess()) {
                return Trampoline.done(Result.failure(in, "Unexpected success"));
            }
            return Trampoline.done(Result.success(Unit.UNIT, in));
        });
    }

    public <B> Parser<I, B> lookahead(Parser<I, B> p) {
        return new Parser<>(in -> {
            Result<I, B> result = p.apply(in).get();
            if (result.isSuccess()) {
                return Trampoline.done(Result.success(result.getOrThrow(), in));
            }
            return Trampoline.done(result);
        });
    }

    public <SEP> Parser<I, IList<A>> terminatedBy(Parser<I, SEP> sep) {
        return this.andL(sep).many();
    }

    public <SEP> Parser<I, IList<A>> terminatedBy1(Parser<I, SEP> sep) {
        return this.andL(sep).many1();
    }

    public static <I, A> Ref<I, A> ref() {
        return new Ref<>();
    }

    public static <I, A> Parser<I, A> pure(A a) {
        if (a == null) {
            throw new IllegalArgumentException("The provided value cannot be null");
        }
        return new Parser<>(LTRUE, in -> {
            return Trampoline.done(Result.success(a, in));
        });
    }

    public static <I, A, B> Parser<I, B> ap(Parser<I, Function<A, B>> pf, Parser<I, A> pa) {
        return new Parser<>(() -> pf.acceptsEOF() && pa.acceptsEOF()) {
            @Override
            public Trampoline<Result<I, B>> apply(Input<I> in) {
                return Trampoline.more(() -> {
                    Result<I, Function<A, B>> r = pf.apply(in).get();
                    if (!r.isSuccess()) {
                        return Trampoline.done(r.cast());
                    }
                    Input<I> next = r.next();
                    if (!pa.acceptsEOF() && next.isEof()) {
                        return Trampoline.done(Result.failureEof(next, "Unexpected end of input"));
                    }
                    Result<I, A> r2 = pa.apply(next).get();
                    if (!r2.isSuccess()) {
                        return Trampoline.done(r2.cast());
                    }
                    return Trampoline.done(r2.map(r.getOrThrow()));
                });
            }
        };
    }

    public static <I, A, B> Parser<I, B> ap(Function<A, B> f, Parser<I, A> pa) {
        return ap(pure(f), pa);
    }

    public static <I, T, U> Parser<I, IList<U>> traverse(IList<T> lt, Function<T, Parser<I, U>> f) {
        return lt.foldRight(pure(IList.empty()),
                (t, plu) -> ap(plu.map(lu -> lu::add), f.apply(t))
        );
    }

    public static <I, T> Parser<I, IList<T>> sequence(IList<Parser<I, T>> lpt) {
        return lpt.foldRight(
                pure(IList.empty()),
                (pt, plt) -> ap(plt.map(lt -> lt::add), pt)
        );
    }

    @SafeVarargs
    public static <I, A> Parser<I, IList<A>> sequence(Parser<I, A>... parsers) {
        return new Parser<>(() -> {
            for (Parser<I, A> parser : parsers) {
                if (!parser.acceptsEOF()) {
                    return false;
                }
            }
            return true;
        }, in -> {
            IList<A> results = IList.empty();
            Input<I> currentInput = in;
            for (Parser<I, A> parser : parsers) {
                Result<I, A> result = parser.apply(currentInput).get();
                if (!result.isSuccess()) {
                    return Trampoline.done(Result.failure(currentInput, "Parsing failed"));
                }
                results = results.add(result.getOrThrow());
                currentInput = result.next();
            }
            return Trampoline.done(Result.success(results.reverse(), currentInput));
        });
    }

    public <B> Parser<I, B> map(Function<A, B> f) {
        Supplier<Boolean> acceptsEmpty = this::acceptsEOF;
        return new Parser<>(acceptsEmpty) {
            @Override
            public Trampoline<Result<I, B>> apply(Input<I> in) {
                return Parser.this.apply(in).map(result -> result.map(f));
            }
        };
    }

    public <B extends A> Parser<I, A> or(Parser<I, B> rhs) {
        return new Parser<>(() -> Parser.this.acceptsEOF() || rhs.acceptsEOF()) {
            @Override
            public Trampoline<Result<I, A>> apply(Input<I> in) {
                if (in.isEof()) {
                    if (Parser.this.acceptsEOF()) {
                        return Parser.this.apply(in);
                    } else if (rhs.acceptsEOF()) {
                        return rhs.apply(in).map(result -> result.cast());
                    }
                    return Trampoline.done(failureEof(this, in));
                }
                Result<I, A> result = Parser.this.apply(in).get();
                if (result.isSuccess()) {
                    return Trampoline.done(result);
                }
                return rhs.apply(in).map(r -> r.cast());
            }
        };
    }

    public <B> ApplyBuilder.ApplyBuilder2<I, A, B> and(Parser<I, B> pb) {
        return new ApplyBuilder.ApplyBuilder2<>(this, pb);
    }

    public <B> Parser<I, A> andL(Parser<I, B> pb) {
        return this.and(pb).map((a, b) -> a);
    }

    public <B> Parser<I, B> andR(Parser<I, B> pb) {
        return this.and(pb).map((a, b) -> b);
    }

    public Parser<I, IList<A>> many() {
        if (Utils.ifRefClass(this).map(Ref::isInitialised).orElse(true) && acceptsEOF()) {
            throw new RuntimeException("Cannot construct a many parser from one that accepts empty");
        }

        return new Parser<>(LTRUE, in -> Trampoline.more(() -> {
            Input<I> currentInput = in;
            IList<A> accumulator = IList.empty();
            while (!currentInput.isEof()) {
                Result<I, A> result = Parser.this.apply(currentInput).get();
                if (result.isSuccess()) {
                    Success<I, A> success = (Success<I, A>) result;
                    accumulator = accumulator.add(success.getOrThrow());
                    currentInput = success.next();
                } else {
                    if (result.getPosition() > in.position()) {
                        return Trampoline.done(Result.failure(result.next(), "result.getError()"));
                    }
                    break;
                }
            }
            return Trampoline.done(Result.success(accumulator.reverse(), currentInput));
        }));
    }

    public Parser<I, IList<A>> repeat(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("The number of repetitions cannot be negative");
        }
        return new Parser<>(() -> n == 0, in -> Trampoline.more(() -> {
            IList<A> accumulator = IList.empty();
            Input<I> currentInput = in;
            for (int i = 0; i < n; i++) {
                if (currentInput.isEof()) {
                    return Trampoline.done(Result.failureEof(currentInput, "Unexpected end of input"));
                }
                Result<I, A> result = this.apply(currentInput).get();
                if (result.isSuccess()) {
                    Success<I, A> success = (Success<I, A>) result;
                    accumulator = accumulator.add(success.getOrThrow());
                    currentInput = success.next();
                } else {
                    return Trampoline.done(Result.failure(currentInput, "Parsing failed before reaching the required number of repetitions"));
                }
            }
            return Trampoline.done(Result.success(accumulator.reverse(), currentInput));
        }));
    }

    public Parser<I, IList<A>> repeat(int min, int max) {
        if (min < 0 || max < 0) {
            throw new IllegalArgumentException("The number of repetitions cannot be negative");
        }
        if (min > max) {
            throw new IllegalArgumentException("The minimum number of repetitions cannot be greater than the maximum");
        }
        return new Parser<>(() -> min == 0, in -> Trampoline.more(() -> {
            IList<A> accumulator = IList.empty();
            Input<I> currentInput = in;
            int count = 0;
            while (count < max) {
                if (currentInput.isEof()) {
                    if (count >= min) {
                        return Trampoline.done(Result.success(accumulator.reverse(), currentInput));
                    } else {
                        return Trampoline.done(Result.failureEof(currentInput, "Unexpected end of input"));
                    }
                }
                Result<I, A> result = this.apply(currentInput).get();
                if (result.isSuccess()) {
                    Success<I, A> success = (Success<I, A>) result;
                    accumulator = accumulator.add(success.getOrThrow());
                    currentInput = success.next();
                    count++;
                } else {
                    if (count >= min) {
                        return Trampoline.done(Result.success(accumulator.reverse(), currentInput));
                    } else {
                        return Trampoline.done(Result.failure(currentInput, "Parsing failed before reaching the required number of repetitions"));
                    }
                }
            }
            return Trampoline.done(Result.success(accumulator.reverse(), currentInput));
        }));
    }

    public <B> Parser<I, IList<A>> manyTill(Parser<I, B> end) {
        return new Parser<>(end::acceptsEOF, in -> Trampoline.more(() -> {
            Input<I> currentInput = in;
            IList<A> accumulator = IList.empty();
            while (true) {
                if (currentInput.isEof()) {
                    return Trampoline.done(Result.failureEof(currentInput, "Unexpected end of input"));
                }
                Result<I, B> endResult = end.apply(currentInput).get().cast();
                if (endResult.isSuccess()) {
                    return Trampoline.done(Result.success(accumulator.reverse(), ((Success<I, B>) endResult).next()));
                }
                Result<I, A> result = Parser.this.apply(in).get();
                if (result.isSuccess()) {
                    Success<I, A> success = (Success<I, A>) result;
                    accumulator = accumulator.add(success.getOrThrow());
                    currentInput = success.next();
                } else {
                    return Trampoline.done(Result.failure(in, "Parsing failed before reaching the end parser"));
                }
            }
        }));
    }

    public Parser<I, IList<A>> many1() {
        return this.and(this.many()).map((a, l) -> l.add(a));
    }

    public Parser<I, Unit> skipMany() {
        return this.many()
                .map(u -> Unit.UNIT);
    }

    public <SEP> Parser<I, IList<A>> sepBy(Parser<I, SEP> sep) {
        return this.sepBy1(sep).map(l -> l)
                .or(pure(IList.empty()));
    }

    public <SEP> Parser<I, IList<A>> sepBy1(Parser<I, SEP> sep) {
        return this.and(sep.andR(this).many())
                .map((a, l) -> l.add(a));
    }

    public Parser<I, Optional<A>> optional() {
        return this.map(Optional::of)
                .or(pure(Optional.empty()));
    }

    public <OPEN, CLOSE> Parser<I, A> between(Parser<I, OPEN> open, Parser<I, CLOSE> close) {
        return open.andR(this).andL(close);
    }

    public Parser<I, A> chainr(Parser<I, BinaryOperator<A>> op, A a) {
        return this.chainr1(op).or(pure(a));
    }

    public Parser<I, A> chainr1(Parser<I, BinaryOperator<A>> op) {
        return this.and(
                op.and(this)
                        .map(Tuple::of)
                        .many()
        ).map((a, lopA) -> {
            A result = a;
            while (lopA.hasNext()) {
                Tuple<BinaryOperator<A>, A> tuple = lopA.next();
                result = tuple.getFirst().apply(result, tuple.getSecond());
            }
            return result;
        });
    }

    public Parser<I, A> chainl(Parser<I, BinaryOperator<A>> op, A a) {
        return this.chainl1(op).or(pure(a));
    }

    public Parser<I, A> chainl1(Parser<I, BinaryOperator<A>> op) {
        final Parser<I, UnaryOperator<A>> plo =
                op.and(this)
                        .map((f, y) -> x -> f.apply(x, y));
        return this.and(plo.many())
                .map((a, lf) -> lf.foldLeft(a, (acc, f) -> f.apply(acc)));
    }

    public static Parser<Character, String> regex(String regex) {
        Pattern pattern = Pattern.compile(regex);
        return new Parser<>(() -> false, in -> Trampoline.more(() -> {
            StringBuilder sb = new StringBuilder();
            Input<Character> currentInput = in;
            while (!currentInput.isEof()) {
                sb.append(currentInput.get());
                Matcher matcher = pattern.matcher(sb.toString());
                if (matcher.matches()) {
                    currentInput = currentInput.next();
                } else if (matcher.hitEnd()) {
                    currentInput = currentInput.next();
                } else {
                    sb.deleteCharAt(sb.length() - 1);
                    break;
                }
            }
            Matcher finalMatcher = pattern.matcher(sb.toString());
            if (finalMatcher.matches()) {
                return Trampoline.done(Result.success(sb.toString(), currentInput));
            } else {
                return Trampoline.done(Result.failure(in, "Input does not match the regular expression"));
            }
        }));
    }

}