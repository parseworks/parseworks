## Unreleased (2.2.1-SNAPSHOT)

- Enhanced error messaging system:
  - Introduced TextInput with line/column/snippet utilities (CharArrayInput, CharSequenceInput, ReaderInput implement it)
  - Failure improvements: custom messages and fullErrorMessage formatting
  - Parser recovery APIs: recover, recoverWith; error aggregation via AggregateFailure and Parser.collectErrors
- Documentation updates and examples aligned with current APIs:
  - Correct use of Numeric.* parsers (replacing NumericParsers.*)
  - Correct trim usage via TextParsers.trim(parser)
  - Replaced nonexistent until() with manyUntil()/zeroOrManyUntil()
  - Clarified chain associativity via Chains.chain(...) and chainLeft/chainRight
  - Fixed API reference wording for zeroOrMany (zero-or-more)
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

**New Features**
- Plugin feature to provide specific implementations of a Parser
- 