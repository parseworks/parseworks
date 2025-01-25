package io.github.parseworks.impl;

import io.github.parseworks.Input;
import io.github.parseworks.Parser;
import io.github.parseworks.Result;

import java.util.function.Function;

public class NoCheckParser<I, A> extends Parser<I, A> {

    public NoCheckParser(Function<Input<I>, Result<I, A>> applyHandler) {
        super(applyHandler);
    }

    @Override
    public Result<I, A> apply(Input<I> in) {
        return applyHandler.apply(in);
    }

}
