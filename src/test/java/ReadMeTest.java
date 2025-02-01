import io.github.parseworks.Input;
import io.github.parseworks.Parser;
import org.junit.jupiter.api.Test;

import static io.github.parseworks.Text.*;

public class ReadMeTest {

    @Test
    public void summ() {
        // This is a test class for the README.md file.
        // It is not intended to be run.
        // It is only here to ensure that the code snippets in the README.md file compile.
        Parser<Character, Integer> sum =
                number.thenSkip(chr('+')).then(number).map(Integer::sum);

        int result = sum.parse(Input.of("1+2")).getOrThrow();
        System.out.println(result); // 3

        try {
            sum.parse(Input.of("1+z")).getOrThrow();
        } catch (Exception e) {
            System.out.println(e.getMessage()); // Failure at position 2, saw 'z', expected <number>
        }
    }

}
