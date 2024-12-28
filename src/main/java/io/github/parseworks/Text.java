package io.github.parseworks;

import java.util.function.Predicate;

public class Text {

    public static Parser<Character, Character> chr(Predicate<Character> predicate) {
        return new Parser<>(input -> {
            if (input.isEof()) {
                return Trampoline.done(Result.failure(input, "End of input"));
            }
            char c = input.get();
            if (predicate.test(c)) {
                return Trampoline.done(Result.success(c, input.next()));
            } else {
                return Trampoline.done(Result.failure(input, "Unexpected character: " + c));
            }
        });
    }

    public static Parser<Character, Character> chr(char c) {
        return chr(ch -> ch == c);
    }

    public static Parser<Character, Character> digit() {
        return chr(Character::isDigit);
    }

    public static Parser<Character, Integer> number() {
        return digit().many1().map(chars -> {
            StringBuilder sb = new StringBuilder();
            while (chars.hasNext()) {
                Character c = chars.next();
                sb.append(c);
            }
            return Integer.parseInt(sb.toString());
        });
    }
}