# parseWorks
<img src="./resources/parseWorks.png" alt="parseWorks logo" title="parseWorks" width="300" align="right">

Build parsers in Java with composable parser combinators. Use parseWorks to express grammars in code, get clear error messages, and keep your parsers fast and testable.

## Key features

- Compose parsers with a small set of powerful combinators.
- Get precise, readable error messages for parse failures.
- Write thread-safe code with immutable parsers and inputs.
- Keep dependencies minimal (only JUnit in tests).
- Avoid left-recursion pitfalls with built-in protection.
- Detect infinite loops on empty input.

## Install parseWorks

Requirements: Java 17 or higher.

Maven (latest release):
```xml
<dependency>
   <groupId>io.github.parseworks</groupId>
   <artifactId>parseworks</artifactId>
   <version>2.2.0</version>
</dependency>
```

Use the current SNAPSHOT (optional):

Maven:
```xml
<repositories>
  <repository>
    <id>sonatype-snapshots</id>
    <url>https://central.sonatype.com/repository/maven-snapshots/</url>
  </repository>
</repositories>

<dependency>
  <groupId>io.github.parseworks</groupId>
  <artifactId>parseworks</artifactId>
  <version>2.2.1-SNAPSHOT</version>
</dependency>
```

Gradle:
```groovy
repositories {
  maven { url 'https://central.sonatype.com/repository/maven-snapshots/' }
}

dependencies {
  implementation 'io.github.parseworks:parseworks:2.2.1-SNAPSHOT'
}
```

## Write your first parser

Use parser combinators to build a simple addition parser:

```java
import io.github.parseworks.parsers.Lexical;

import static io.github.parseworks.parsers.Numeric.*;

// Define a parser for a simple addition expression
Parser<Character, Integer> sum =
    number.thenSkip(Lexical.chr('+')).then(number).map(Integer::sum);

    // Parse the input "1+2"
    int value = sum.parse(Input.of("1+2")).get();
assert value ==3;

    // Handle a parsing error
    String message = sum.parse(Input.of("1+z")).handle(
        success -> "Match: " + success,
        failure -> "Error: " + failure.error()
    );
// Example output contains: "Error: Parse error at line 1 position 3"
```

## Learn more

- Read the [User guide](docs/user-guide.md) to get started.
- Explore the [Advanced user guide](docs/advanced-user-guide.md) for complex patterns and performance tips.
- Follow the [Parser design guide](docs/parser-design-guide.md) to plan and implement robust parsers.

## License

parseWorks is available under the [MIT License](LICENSE).
