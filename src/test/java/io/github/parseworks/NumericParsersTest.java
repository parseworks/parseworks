package io.github.parseworks;

import org.junit.jupiter.api.Test;

import static io.github.parseworks.parsers.Numeric.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Numeric class.
 * These tests verify that the numeric parsers correctly parse different types of numeric values.
 */
public class NumericParsersTest {

    @Test
    public void testNonZeroDigit() {
        // Test successful parsing of non-zero digits
        for (char c = '1'; c <= '9'; c++) {
            Result<Character, Character> result = nonZeroDigit.parse(String.valueOf(c));
            assertTrue(result.matches());
            assertEquals(c, result.value());
        }

        // Test failure for zero
        Result<Character, Character> zeroResult = nonZeroDigit.parse("0");
        assertTrue(!zeroResult.matches());

        // Test failure for non-digit
        Result<Character, Character> nonDigitResult = nonZeroDigit.parse("a");
        assertTrue(!nonDigitResult.matches());
    }

    @Test
    public void testNumeric() {
        // Test successful parsing of all digits
        for (char c = '0'; c <= '9'; c++) {
            Result<Character, Character> result = numeric.parse(String.valueOf(c));
            assertTrue(result.matches());
            assertEquals(c, result.value());
        }

        // Test failure for non-digit
        Result<Character, Character> nonDigitResult = numeric.parse("a");
        assertTrue(!nonDigitResult.matches());
    }

    @Test
    public void testSign() {
        // Test positive sign
        Result<Character, Boolean> plusResult = sign.parse("+");
        assertTrue(plusResult.matches());
        assertTrue(plusResult.value());

        // Test negative sign
        Result<Character, Boolean> minusResult = sign.parse("-");
        assertTrue(minusResult.matches());
        assertFalse(minusResult.value());

        // Test no sign (default to positive)
        Result<Character, Boolean> noSignResult = sign.parse("123");
        assertTrue(noSignResult.matches());
        assertTrue(noSignResult.value());
    }

    @Test
    public void testUnsignedInteger() {
        // Test zero
        Result<Character, Integer> zeroResult = unsignedInteger.parse("0");
        assertTrue(zeroResult.matches());
        assertEquals(0, zeroResult.value());

        // Test single digit
        Result<Character, Integer> singleDigitResult = unsignedInteger.parse("5");
        assertTrue(singleDigitResult.matches());
        assertEquals(5, singleDigitResult.value());

        // Test multiple digits
        Result<Character, Integer> multiDigitResult = unsignedInteger.parse("123");
        assertTrue(multiDigitResult.matches());
        assertEquals(123, multiDigitResult.value());

        // Test leading zero followed by other digits (should only parse the zero)
        Result<Character, Integer> leadingZeroResult = unsignedInteger.parse("0123");
        assertTrue(leadingZeroResult.matches());
        assertEquals(0, leadingZeroResult.value());

        // Test failure for non-digit
        Result<Character, Integer> nonDigitResult = unsignedInteger.parse("a");
        assertTrue(!nonDigitResult.matches());
    }

    @Test
    public void testInteger() {
        // Test positive integer without sign
        Result<Character, Integer> positiveNoSignResult = integer.parse("123");
        assertTrue(positiveNoSignResult.matches());
        assertEquals(123, positiveNoSignResult.value());

        // Test positive integer with sign
        Result<Character, Integer> positiveWithSignResult = integer.parse("+123");
        assertTrue(positiveWithSignResult.matches());
        assertEquals(123, positiveWithSignResult.value());

        // Test negative integer
        Result<Character, Integer> negativeResult = integer.parse("-123");
        assertTrue(negativeResult.matches());
        assertEquals(-123, negativeResult.value());

        // Test zero
        Result<Character, Integer> zeroResult = integer.parse("0");
        assertTrue(zeroResult.matches());
        assertEquals(0, zeroResult.value());

        // Test negative zero
        Result<Character, Integer> negativeZeroResult = integer.parse("-0");
        assertTrue(negativeZeroResult.matches());
        assertEquals(0, negativeZeroResult.value());

        // Test failure for non-digit
        Result<Character, Integer> nonDigitResult = integer.parse("a");
        assertTrue(!nonDigitResult.matches());
    }

    @Test
    public void testUnsignedLong() {
        // Test zero
        Result<Character, Long> zeroResult = unsignedLong.parse("0");
        assertTrue(zeroResult.matches());
        assertEquals(0L, zeroResult.value());

        // Test single digit
        Result<Character, Long> singleDigitResult = unsignedLong.parse("5");
        assertTrue(singleDigitResult.matches());
        assertEquals(5L, singleDigitResult.value());

        // Test multiple digits
        Result<Character, Long> multiDigitResult = unsignedLong.parse("123456789");
        assertTrue(multiDigitResult.matches());
        assertEquals(123456789L, multiDigitResult.value());

        // Test large number
        Result<Character, Long> largeNumberResult = unsignedLong.parse("9223372036854775807"); // Long.MAX_VALUE
        assertTrue(largeNumberResult.matches());
        assertEquals(Long.MAX_VALUE, largeNumberResult.value());

        // Test leading zero followed by other digits (should only parse the zero)
        Result<Character, Long> leadingZeroResult = unsignedLong.parse("0123");
        assertTrue(leadingZeroResult.matches());
        assertEquals(0L, leadingZeroResult.value());

        // Test failure for non-digit
        Result<Character, Long> nonDigitResult = unsignedLong.parse("a");
        assertTrue(!nonDigitResult.matches());
    }

    @Test
    public void testLongValue() {
        // Test positive long without sign
        Result<Character, Long> positiveNoSignResult = longValue.parse("123");
        assertTrue(positiveNoSignResult.matches());
        assertEquals(123L, positiveNoSignResult.value());

        // Test positive long with sign
        Result<Character, Long> positiveWithSignResult = longValue.parse("+123");
        assertTrue(positiveWithSignResult.matches());
        assertEquals(123L, positiveWithSignResult.value());

        // Test negative long
        Result<Character, Long> negativeResult = longValue.parse("-123");
        assertTrue(negativeResult.matches());
        assertEquals(-123L, negativeResult.value());

        // Test large positive number
        Result<Character, Long> largePositiveResult = longValue.parse("9223372036854775807"); // Long.MAX_VALUE
        assertTrue(largePositiveResult.matches());
        assertEquals(Long.MAX_VALUE, largePositiveResult.value());

        // Test large negative number
        Result<Character, Long> largeNegativeResult = longValue.parse("-9223372036854775808"); // Long.MIN_VALUE
        assertTrue(largeNegativeResult.matches());
        assertEquals(Long.MIN_VALUE, largeNegativeResult.value());

        // Test zero
        Result<Character, Long> zeroResult = longValue.parse("0");
        assertTrue(zeroResult.matches());
        assertEquals(0L, zeroResult.value());

        // Test negative zero
        Result<Character, Long> negativeZeroResult = longValue.parse("-0");
        assertTrue(negativeZeroResult.matches());
        assertEquals(0L, negativeZeroResult.value());

        // Test failure for non-digit
        Result<Character, Long> nonDigitResult = longValue.parse("a");
        assertTrue(!nonDigitResult.matches());
    }

    @Test
    public void testDoubleValue() {
        // Test integer part only
        Result<Character, Double> integerOnlyResult = doubleValue.parse("123");
        assertTrue(integerOnlyResult.matches());
        assertEquals(123.0, integerOnlyResult.value());

        // Test with decimal point
        Result<Character, Double> decimalResult = doubleValue.parse("123.45");
        assertTrue(decimalResult.matches());
        assertEquals(123.45, decimalResult.value());

        // Test with positive sign
        Result<Character, Double> positiveSignResult = doubleValue.parse("+123.45");
        assertTrue(positiveSignResult.matches());
        assertEquals(123.45, positiveSignResult.value());

        // Test with negative sign
        Result<Character, Double> negativeSignResult = doubleValue.parse("-123.45");
        assertTrue(negativeSignResult.matches());
        assertEquals(-123.45, negativeSignResult.value());

        // Test with exponent (positive)
        Result<Character, Double> positiveExponentResult = doubleValue.parse("1.23e2");
        assertTrue(positiveExponentResult.matches());
        assertEquals(123.0, positiveExponentResult.value());

        // Test with exponent (negative)
        Result<Character, Double> negativeExponentResult = doubleValue.parse("1.23e-2");
        assertTrue(negativeExponentResult.matches());
        assertEquals(0.0123, negativeExponentResult.value());

        // Test with uppercase exponent
        Result<Character, Double> uppercaseExponentResult = doubleValue.parse("1.23E2");
        assertTrue(uppercaseExponentResult.matches());
        assertEquals(123.0, uppercaseExponentResult.value());

        // Test zero
        Result<Character, Double> zeroResult = doubleValue.parse("0");
        assertTrue(zeroResult.matches());
        assertEquals(0.0, zeroResult.value());

        // Test negative zero
        Result<Character, Double> negativeZeroResult = doubleValue.parse("-0");
        assertTrue(negativeZeroResult.matches());
        assertEquals(-0.0, negativeZeroResult.value());

        // Test failure for non-numeric
        Result<Character, Double> nonNumericResult = doubleValue.parse("abc");
        assertTrue(!nonNumericResult.matches());
    }

    @Test
    public void testNumber() {
        // Test single digit
        Result<Character, Integer> singleDigitResult = number.parse("5");
        assertTrue(singleDigitResult.matches());
        assertEquals(5, singleDigitResult.value());

        // Test multiple digits
        Result<Character, Integer> multiDigitResult = number.parse("123");
        assertTrue(multiDigitResult.matches());
        assertEquals(123, multiDigitResult.value());

        // Test leading zero
        Result<Character, Integer> leadingZeroResult = number.parse("0123");
        assertTrue(leadingZeroResult.matches());
        assertEquals(123, leadingZeroResult.value());

        // Test zero
        Result<Character, Integer> zeroResult = number.parse("0");
        assertTrue(zeroResult.matches());
        assertEquals(0, zeroResult.value());

        // Test failure for non-digit
        Result<Character, Integer> nonDigitResult = number.parse("a");
        assertTrue(!nonDigitResult.matches());
    }

    @Test
    public void testHex() {
        // Test lowercase prefix
        Result<Character, Integer> lowercasePrefixResult = hex.parse("0x1a");
        assertTrue(lowercasePrefixResult.matches());
        assertEquals(26, lowercasePrefixResult.value());

        // Test uppercase prefix
        Result<Character, Integer> uppercasePrefixResult = hex.parse("0X1A");
        assertTrue(uppercasePrefixResult.matches());
        assertEquals(26, uppercasePrefixResult.value());

        // Test mixed case digits
        Result<Character, Integer> mixedCaseResult = hex.parse("0xaBcD");
        assertTrue(mixedCaseResult.matches());
        assertEquals(0xABCD, mixedCaseResult.value());

        // Test zero
        Result<Character, Integer> zeroResult = hex.parse("0x0");
        assertTrue(zeroResult.matches());
        assertEquals(0, zeroResult.value());

        // Test failure for invalid prefix
        Result<Character, Integer> invalidPrefixResult = hex.parse("0y1a");
        assertTrue(!invalidPrefixResult.matches());

        // Test failure for missing hex digits
        Result<Character, Integer> missingDigitsResult = hex.parse("0x");
        assertTrue(!missingDigitsResult.matches());
    }
}