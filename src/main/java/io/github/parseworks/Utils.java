package io.github.parseworks;

import java.util.List;
import java.util.function.BinaryOperator;

public class Utils {

    public static <I, A> Result<I, A> failure(Input<I> input) {
        return Result.failure(input, "Parsing failed at position " + input.position());
    }
}