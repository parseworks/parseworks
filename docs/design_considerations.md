### Phrasing
*   **Accessible Terminology**: One of the primary goals is to create a terminology that is clear and communicates purpose while being accessible to people who haven't studied parsing. This requires us to be wordier than other parser combinators. This is why we have `zeroOrMore` and `oneOrMore` methods rather than something more abstract like `many` or `many1`. We prefer descriptive names that explain exactly what they do.

### Results and Backtracking
*   **Immutable Input** solves a number of problems having to do with consumption. When we pass an input object to a parser and it fails, we generally don't have to worry about whether partial consumption has occurred, because the Input object we passed in doesn't change.
    *   *Note*: Some input sources (like `ReaderInput`) wrap mutable streams and are single-pass only. For full backtracking support and high-fidelity error reporting, random-access inputs like `CharSequenceInput` are preferred.
*   **Backtracking Control**: The library distinguishes between a total failure to match (`NO_MATCH`) and a failure that occurred after some input was already consumed (`PARTIAL`).
    *   Sequential combinators like `then`, `skipThen`, and `thenSkip` will return a `PARTIAL` failure if they fail after the first part has already succeeded.
    *   Parsers that attempt multiple options, such as `oneOf` or `or`, will stop and report a `PARTIAL` failure immediately if a branch fails after consuming input, assuming that if a parser started matching, it's the intended branch.
    *   If all branches in `oneOf` fail with `NO_MATCH` (no input consumed), it will collect all failures into a "Combined Failure" result which lists out all the branch failures.
*   **The `attempt` and `expecting` Methods**:
    *   `expecting(label)` provides a wrapper on the parser that replaces the default error message with a domain-specific label. This allows us to provide a cleaner series of error messages while preserving the original failure as the cause.
    *   The `attempt` combinator explicitly sets the input index of a failure to the initial input index so that it appears to have consumed no input from the perspective of the parent parser, thereby enabling backtracking for that specific operation.
*   **Predictability**: This design ensures that errors are reported at the deepest point of failure in the most promising branch, rather than falling back to less relevant alternatives when a grammar has already partially matched a specific construct.

### API Design
*   **Builder Pattern**: Complex sequences are handled via `ApplyBuilder`, providing a fluent API for combining multiple results (e.g., `p1.then(p2).then(p3).map((v1, v2, v3) -> ...)`). This avoids deeply nested tuples and keeps the code readable.
*   **Fluent Interface**: Most combinators are available as instance methods on the `Parser` class, allowing for a readable, "left-to-right" parsing specification (e.g., `parser.zeroOrMore().optional()`).
*   **Functional Core**: The `Parser` class itself is built around a single functional interface `in -> Result`, but provides a rich set of instance methods to facilitate composition without requiring manual implementation of that interface in most cases.