package io.github.parseworks.parsers;

import java.util.List;

public interface TestParser {
    String getName();
    List<List<String>> parseCSV(String input);
}
