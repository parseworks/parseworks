package io.github.parseworks;

/**
 * <p>Utils class.</p>
 *
 * @author jason bailey
 * @version $Id: $Id
 */
public class Utils {

    /**
     * <p>failure.</p>
     *
     * @param input a {@link io.github.parseworks.Input} object
     * @param <I>   a I class
     * @param <A>   a A class
     * @return a {@link io.github.parseworks.Result} object
     */
    public static <I, A> Result<I, A> failure(Input<I> input) {
        return Result.failure(input, "Parsing failed at position " + input.position());
    }

    /**
     * <p>listToString.</p>
     *
     * @param input a {@link io.github.parseworks.FList} object
     * @return a {@link java.lang.String} object
     */
    public static String listToString(FList<Character> input) {
        StringBuilder sb = new StringBuilder();
        for (var c : input) {
            sb.append(c);
        }
        return sb.toString();
    }
}
