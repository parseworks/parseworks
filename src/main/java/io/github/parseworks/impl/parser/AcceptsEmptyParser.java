package io.github.parseworks.impl.parser;

import io.github.parseworks.Input;
import io.github.parseworks.Parser;
import io.github.parseworks.Result;

import java.util.function.Function;

public class AcceptsEmptyParser<I,A> extends Parser<I,A> {

    public AcceptsEmptyParser(Function<Input<I>, Result<I, A>> applyHandler) {
        super(applyHandler);
    }

}
