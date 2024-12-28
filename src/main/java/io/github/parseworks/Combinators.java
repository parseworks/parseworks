package io.github.parseworks;

import java.util.function.Predicate;

public class Combinators {
    public static <I> Parser<I, Unit> eof() {
        return new Parser<>(() -> true, in -> {
            if (in.isEof()) {
                return Trampoline.done(Result.success(Unit.UNIT, in));
            } else {
                return Trampoline.done(Result.failure(in, "Expected end of input"));
            }
        });
    }

    public static <I> Parser<I, Unit> fail(String message) {
        return new Parser<>(() -> true, in -> Trampoline.done(Result.failure(in, message)));
    }

    public static <I, A> Parser<I, A> pure(A a) {
        return new Parser<>(() -> true, in -> Trampoline.done(Result.success(a, in)));
    }

    public static <I, A> Parser<I, A> satisfy(String errorMessage,Predicate<I> predicate) {
        return new Parser<>(() -> false, in -> {
            if (in.isEof()) {
                return Trampoline.done(Result.failure(in, "End of input"));
            }
            I i = in.get();
            if (predicate.test(i)) {
                return Trampoline.done((Result<I, A>) Result.success(i, in.next()));
            } else {
                return Trampoline.done(Result.failure(in, "Unexpected input: " + i));
            }
        });
    }
}