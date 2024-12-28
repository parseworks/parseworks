package io.github.parseworks;

import java.util.Optional;
import java.util.function.Supplier;

public class Utils {
    public static final Supplier<Boolean> LTRUE = () -> true;

    public static <I, A> Result<I, A> failure(Input<I> input) {
        return Result.failure(input, "Parsing failed");
    }

    public static <I, A> Result<I, A> failureEof(Parser<I, A> parser, Input<I> input) {
        return Result.failureEof(input, "Unexpected end of input");
    }

    public static <I, A> A reduce(Tuple<A, IList<Tuple<A, A>>> tuple) {
        // Implement reduce logic
        return null;
    }

    public static <I, A> Optional<Ref<I, A>> ifRefClass(Parser<I, A> parser) {
        if (parser instanceof Ref) {
            return Optional.of((Ref<I, A>) parser);
        }
        return Optional.empty();
    }
}