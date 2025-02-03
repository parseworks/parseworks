package io.github.parseworks.impl.parser;

import io.github.parseworks.Input;
import io.github.parseworks.Parser;
import io.github.parseworks.Result;

import java.util.function.Function;

/**
 * <p>AcceptsEmptyParser class.</p>
 *
 * @author jason bailey
 * @version $Id: $Id
 */
public class AcceptsEmptyParser<I,A> extends Parser<I,A> {

    /**
     * <p>Constructor for AcceptsEmptyParser.</p>
     *
     * @param applyHandler a {@link java.util.function.Function} object
     */
    public AcceptsEmptyParser(Function<Input<I>, Result<I, A>> applyHandler) {
        super(applyHandler);
    }

}
