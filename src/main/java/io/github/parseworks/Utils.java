package io.github.parseworks;

import java.util.List;
import java.util.function.BinaryOperator;

public class Utils {

    public static <I, A> Result<I, A> failure(Input<I> input) {
        return Result.failure(input, "Parsing failed at position " + input.position());
    }

    public static String listToString(FList<Character> input) {
        StringBuilder sb = new StringBuilder();
        for (var c : input) {
            sb.append(c);
        }
        return sb.toString();
    }
}