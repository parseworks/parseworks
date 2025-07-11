package io.github.parseworks;

import org.junit.jupiter.api.Test;

import static io.github.parseworks.parsers.NumericParsers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the NumericParsers class.
 * These tests verify that the numeric parsers correctly parse different types of numeric values.
 */
public class NumericParsersTest {

    @Test
    public void testNonZeroDigit() {
        // Test successful parsing of non-zero digits
        for (char c = '1'; c <= '9'; c++) {
            Result<Character, Character> result = nonZeroDigit.parse(String.valueOf(c));
            assertTrue(result.isSuccess());
            assertEquals(c, result.get());
        }

        // Test failure for zero
        Result<Character, Character> zeroResult = nonZeroDigit.parse("0");
        assertTrue(zeroResult.isError());

        // Test failure for non-digit
        Result<Character, Character> nonDigitResult = nonZeroDigit.parse("a");
        assertTrue(nonDigitResult.isError());
    }

    @Test
    public void testNumeric() {
        // Test successful parsing of all digits
        for (char c = '0'; c <= '9'; c++) {
            Result<Character, Character> result = numeric.parse(String.valueOf(c));
            assertTrue(result.isSuccess());
            assertEquals(c, result.get());
        }

        // Test failure for non-digit
        Result<Character, Character> nonDigitResult = numeric.parse("a");
        assertTrue(nonDigitResult.isError());
    }

    @Test
    public void testSign() {
        // Test positive sign
        Result<Character, Boolean> plusResult = sign.parse("+");
        assertTrue(plusResult.isSuccess());
        assertTrue(plusResult.get());

        // Test negative sign
        Result<Character, Boolean> minusResult = sign.parse("-");
        assertTrue(minusResult.isSuccess());
        assertFalse(minusResult.get());

        // Test no sign (default to positive)
        Result<Character, Boolean> noSignResult = sign.parse("123");
        assertTrue(noSignResult.isSuccess());
        assertTrue(noSignResult.get());
    }

    @Test
    public void testUnsignedInteger() {
        // Test zero
        Result<Character, Integer> zeroResult = unsignedInteger.parse("0");
        assertTrue(zeroResult.isSuccess());
        assertEquals(0, zeroResult.get());

        // Test single digit
        Result<Character, Integer> singleDigitResult = unsignedInteger.parse("5");
        assertTrue(singleDigitResult.isSuccess());
        assertEquals(5, singleDigitResult.get());

        // Test multiple digits
        Result<Character, Integer> multiDigitResult = unsignedInteger.parse("123");
        assertTrue(multiDigitResult.isSuccess());
        assertEquals(123, multiDigitResult.get());

        // Test leading zero followed by other digits (should only parse the zero)
        Result<Character, Integer> leadingZeroResult = unsignedInteger.parse("0123");
        assertTrue(leadingZeroResult.isSuccess());
        assertEquals(0, leadingZeroResult.get());

        // Test failure for non-digit
        Result<Character, Integer> nonDigitResult = unsignedInteger.parse("a");
        assertTrue(nonDigitResult.isError());
    }

    @Test
    public void testInteger() {
        // Test positive integer without sign
        Result<Character, Integer> positiveNoSignResult = integer.parse("123");
        assertTrue(positiveNoSignResult.isSuccess());
        assertEquals(123, positiveNoSignResult.get());

        // Test positive integer with sign
        Result<Character, Integer> positiveWithSignResult = integer.parse("+123");
        assertTrue(positiveWithSignResult.isSuccess());
        assertEquals(123, positiveWithSignResult.get());

        // Test negative integer
        Result<Character, Integer> negativeResult = integer.parse("-123");
        assertTrue(negativeResult.isSuccess());
        assertEquals(-123, negativeResult.get());

        // Test zero
        Result<Character, Integer> zeroResult = integer.parse("0");
        assertTrue(zeroResult.isSuccess());
        assertEquals(0, zeroResult.get());

        // Test negative zero
        Result<Character, Integer> negativeZeroResult = integer.parse("-0");
        assertTrue(negativeZeroResult.isSuccess());
        assertEquals(0, negativeZeroResult.get());

        // Test failure for non-digit
        Result<Character, Integer> nonDigitResult = integer.parse("a");
        assertTrue(nonDigitResult.isError());
    }

    @Test
    public void testUnsignedLong() {
        // Test zero
        Result<Character, Long> zeroResult = unsignedLong.parse("0");
        assertTrue(zeroResult.isSuccess());
        assertEquals(0L, zeroResult.get());

        // Test single digit
        Result<Character, Long> singleDigitResult = unsignedLong.parse("5");
        assertTrue(singleDigitResult.isSuccess());
        assertEquals(5L, singleDigitResult.get());

        // Test multiple digits
        Result<Character, Long> multiDigitResult = unsignedLong.parse("123456789");
        assertTrue(multiDigitResult.isSuccess());
        assertEquals(123456789L, multiDigitResult.get());

        // Test large number
        Result<Character, Long> largeNumberResult = unsignedLong.parse("9223372036854775807"); // Long.MAX_VALUE
        assertTrue(largeNumberResult.isSuccess());
        assertEquals(Long.MAX_VALUE, largeNumberResult.get());

        // Test leading zero followed by other digits (should only parse the zero)
        Result<Character, Long> leadingZeroResult = unsignedLong.parse("0123");
        assertTrue(leadingZeroResult.isSuccess());
        assertEquals(0L, leadingZeroResult.get());

        // Test failure for non-digit
        Result<Character, Long> nonDigitResult = unsignedLong.parse("a");
        assertTrue(nonDigitResult.isError());
    }

    @Test
    public void testLongValue() {
        // Test positive long without sign
        Result<Character, Long> positiveNoSignResult = longValue.parse("123");
        assertTrue(positiveNoSignResult.isSuccess());
        assertEquals(123L, positiveNoSignResult.get());

        // Test positive long with sign
        Result<Character, Long> positiveWithSignResult = longValue.parse("+123");
        assertTrue(positiveWithSignResult.isSuccess());
        assertEquals(123L, positiveWithSignResult.get());

        // Test negative long
        Result<Character, Long> negativeResult = longValue.parse("-123");
        assertTrue(negativeResult.isSuccess());
        assertEquals(-123L, negativeResult.get());

        // Test large positive number
        Result<Character, Long> largePositiveResult = longValue.parse("9223372036854775807"); // Long.MAX_VALUE
        assertTrue(largePositiveResult.isSuccess());
        assertEquals(Long.MAX_VALUE, largePositiveResult.get());

        // Test large negative number
        Result<Character, Long> largeNegativeResult = longValue.parse("-9223372036854775808"); // Long.MIN_VALUE
        assertTrue(largeNegativeResult.isSuccess());
        assertEquals(Long.MIN_VALUE, largeNegativeResult.get());

        // Test zero
        Result<Character, Long> zeroResult = longValue.parse("0");
        assertTrue(zeroResult.isSuccess());
        assertEquals(0L, zeroResult.get());

        // Test negative zero
        Result<Character, Long> negativeZeroResult = longValue.parse("-0");
        assertTrue(negativeZeroResult.isSuccess());
        assertEquals(0L, negativeZeroResult.get());

        // Test failure for non-digit
        Result<Character, Long> nonDigitResult = longValue.parse("a");
        assertTrue(nonDigitResult.isError());
    }

    @Test
    public void testDoubleValue() {
        // Test integer part only
        Result<Character, Double> integerOnlyResult = doubleValue.parse("123");
        assertTrue(integerOnlyResult.isSuccess());
        assertEquals(123.0, integerOnlyResult.get());

        // Test with decimal point
        Result<Character, Double> decimalResult = doubleValue.parse("123.45");
        assertTrue(decimalResult.isSuccess());
        assertEquals(123.45, decimalResult.get());

        // Test with positive sign
        Result<Character, Double> positiveSignResult = doubleValue.parse("+123.45");
        assertTrue(positiveSignResult.isSuccess());
        assertEquals(123.45, positiveSignResult.get());

        // Test with negative sign
        Result<Character, Double> negativeSignResult = doubleValue.parse("-123.45");
        assertTrue(negativeSignResult.isSuccess());
        assertEquals(-123.45, negativeSignResult.get());

        // Test with exponent (positive)
        Result<Character, Double> positiveExponentResult = doubleValue.parse("1.23e2");
        assertTrue(positiveExponentResult.isSuccess());
        assertEquals(123.0, positiveExponentResult.get());

        // Test with exponent (negative)
        Result<Character, Double> negativeExponentResult = doubleValue.parse("1.23e-2");
        assertTrue(negativeExponentResult.isSuccess());
        assertEquals(0.0123, negativeExponentResult.get());

        // Test with uppercase exponent
        Result<Character, Double> uppercaseExponentResult = doubleValue.parse("1.23E2");
        assertTrue(uppercaseExponentResult.isSuccess());
        assertEquals(123.0, uppercaseExponentResult.get());

        // Test zero
        Result<Character, Double> zeroResult = doubleValue.parse("0");
        assertTrue(zeroResult.isSuccess());
        assertEquals(0.0, zeroResult.get());

        // Test negative zero
        Result<Character, Double> negativeZeroResult = doubleValue.parse("-0");
        assertTrue(negativeZeroResult.isSuccess());
        assertEquals(-0.0, negativeZeroResult.get());

        // Test failure for non-numeric
        Result<Character, Double> nonNumericResult = doubleValue.parse("abc");
        assertTrue(nonNumericResult.isError());
    }

    @Test
    public void testNumber() {
        // Test single digit
        Result<Character, Integer> singleDigitResult = number.parse("5");
        assertTrue(singleDigitResult.isSuccess());
        assertEquals(5, singleDigitResult.get());

        // Test multiple digits
        Result<Character, Integer> multiDigitResult = number.parse("123");
        assertTrue(multiDigitResult.isSuccess());
        assertEquals(123, multiDigitResult.get());

        // Test leading zero
        Result<Character, Integer> leadingZeroResult = number.parse("0123");
        assertTrue(leadingZeroResult.isSuccess());
        assertEquals(123, leadingZeroResult.get());

        // Test zero
        Result<Character, Integer> zeroResult = number.parse("0");
        assertTrue(zeroResult.isSuccess());
        assertEquals(0, zeroResult.get());

        // Test failure for non-digit
        Result<Character, Integer> nonDigitResult = number.parse("a");
        assertTrue(nonDigitResult.isError());
    }

    @Test
    public void testHex() {
        // Test lowercase prefix
        Result<Character, Integer> lowercasePrefixResult = hex.parse("0x1a");
        assertTrue(lowercasePrefixResult.isSuccess());
        assertEquals(26, lowercasePrefixResult.get());

        // Test uppercase prefix
        Result<Character, Integer> uppercasePrefixResult = hex.parse("0X1A");
        assertTrue(uppercasePrefixResult.isSuccess());
        assertEquals(26, uppercasePrefixResult.get());

        // Test mixed case digits
        Result<Character, Integer> mixedCaseResult = hex.parse("0xaBcD");
        assertTrue(mixedCaseResult.isSuccess());
        assertEquals(0xABCD, mixedCaseResult.get());

        // Test zero
        Result<Character, Integer> zeroResult = hex.parse("0x0");
        assertTrue(zeroResult.isSuccess());
        assertEquals(0, zeroResult.get());

        // Test failure for invalid prefix
        Result<Character, Integer> invalidPrefixResult = hex.parse("0y1a");
        assertTrue(invalidPrefixResult.isError());

        // Test failure for missing hex digits
        Result<Character, Integer> missingDigitsResult = hex.parse("0x");
        assertTrue(missingDigitsResult.isError());
    }
}