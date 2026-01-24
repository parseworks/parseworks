## Unreleased (2.2.1-SNAPSHOT)

- Breaking Changes:
  - Renamed `ResultType.PARSE` to `ResultType.MATCH`.
  - Removed `Parser.atomic()` in favor of `Combinators.attempt(Parser)`.
  - Removed `Result.validationError()`.
  - Changed `Parser.parse(Input, boolean)` to return a `PartialMatch` (which is a `Failure`) if the input is not fully consumed, instead of a generic error.
- Enhanced error messaging system:
  - Introduced `Failure` interface; `NoMatch` and `PartialMatch` now implement it.
  - Introduced `TextInput` interface with line/column/snippet utilities (`CharArrayInput`, `CharSequenceInput`, `ReaderInput` implement it).
  - Failure improvements: custom messages and improved formatting.
  - Parser recovery APIs: `recover`, `recoverWith`; error aggregation via `Parser.collectErrors`.
- Documentation updates and examples aligned with current APIs:
  - Updated `README.md` to use `Result.value()` instead of `Result.get()`.
  - Updated `Combinators.java` KDocs to remove nonexistent `fail(String, ErrorType)`.
  - Updated `docs/advanced-user-guide.md` to remove `failValidation` and `failSyntax`.
  - Correct use of `Numeric.*` parsers (replacing `NumericParsers.*`).
  - Correct trim usage via `TextParsers.trim(parser)`.
  - Replaced nonexistent `until()` with `manyUntil()`/`zeroOrManyUntil()`.
  - Clarified chain associativity via `Chains.chain(...)` and `chainLeft`/`chainRight`.
  - Fixed API reference wording for `zeroOrMany` (zero-or-more).
- Build: Java 17 baseline confirmed

## 3.0.0 (Planned - Unreleased)

**Breaking Changes**
- Refactored Utility Parsers into separate files for better organization.
- Removed all static utility parsers from Parser class.
- Removed Character specific parsers from Parser class.
- Renamed methods in Parser class to be more descriptive and consistent.
- Removed `not` from Parser class due to confusion over not's meaning
- Implemented `onlyIf` and `peek` parsers
  - `onlyIf` applies a condition to a parser at that parsers position
  - `peek` applies a condition to a parser on what follows


**Enhancements**
- Improved overall parsing performance
- Comprehensive Javadoc cleanup and example updates

**New Features**
- Plugin feature to provide specific implementations of a Parser
- 