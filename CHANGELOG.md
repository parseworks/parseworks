## 3.0.0

**Breaking Changes**
- Refactored Utility Parsers into separate files for better organization.
- Removed all static utility parsers from Parser class.
- Removed Character specific parsers from Parser class.
- Renamed methods in Parser class to be more descriptive and consistent.
- Removed `not` from Parser class due to confusion over not's meaning
- Implemented `onlyIf` and `ifThen` parsers
  - `onlyIf` applies a condition to a parser at that parsers position
  - `ifThen` applies a condition to a parser on what follows


**Enhancements**
- Improved overall parsing performance

**New Features**
- Plugin feature to provide specific implementations of a Parser
