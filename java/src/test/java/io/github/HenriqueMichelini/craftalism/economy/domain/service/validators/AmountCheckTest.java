package io.github.HenriqueMichelini.craftalism.economy.domain.service.validators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AmountCheck Tests")
class AmountCheckTest {

    private AmountCheck amountCheck;

    @BeforeEach
    void setUp() {
        amountCheck = new AmountCheck();
    }

    @Test
    @DisplayName("Should return true for positive amounts")
    void shouldReturnTrueForPositiveAmounts() {
        assertTrue(amountCheck.isPositive(1L));
        assertTrue(amountCheck.isPositive(100L));
        assertTrue(amountCheck.isPositive(1000000L));
        assertTrue(amountCheck.isPositive(Long.MAX_VALUE));
    }

    @Test
    @DisplayName("Should return false for zero")
    void shouldReturnFalseForZero() {
        assertFalse(amountCheck.isPositive(0L));
    }

    @Test
    @DisplayName("Should return false for negative amounts")
    void shouldReturnFalseForNegativeAmounts() {
        assertFalse(amountCheck.isPositive(-1L));
        assertFalse(amountCheck.isPositive(-100L));
        assertFalse(amountCheck.isPositive(-1000000L));
        assertFalse(amountCheck.isPositive(Long.MIN_VALUE));
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 10, 100, 1000, 10000, 100000, Long.MAX_VALUE})
    @DisplayName("Should validate positive amounts using parameterized test")
    void shouldValidatePositiveAmountsParameterized(long amount) {
        assertTrue(amountCheck.isPositive(amount));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, -10, -100, -1000, Long.MIN_VALUE})
    @DisplayName("Should invalidate non-positive amounts using parameterized test")
    void shouldInvalidateNonPositiveAmountsParameterized(long amount) {
        assertFalse(amountCheck.isPositive(amount));
    }

    @Test
    @DisplayName("Should detect overflow when adding to Long.MAX_VALUE")
    void shouldDetectOverflowAtMaxValue() {
        assertTrue(amountCheck.willOverflowOnAdd(Long.MAX_VALUE, 1L));
        assertTrue(amountCheck.willOverflowOnAdd(Long.MAX_VALUE, 100L));
        assertTrue(amountCheck.willOverflowOnAdd(Long.MAX_VALUE, Long.MAX_VALUE));
    }

    @Test
    @DisplayName("Should detect overflow when sum exceeds Long.MAX_VALUE")
    void shouldDetectOverflowWhenSumExceedsMax() {
        assertTrue(amountCheck.willOverflowOnAdd(Long.MAX_VALUE - 10, 11L));
        assertTrue(amountCheck.willOverflowOnAdd(Long.MAX_VALUE - 100, 101L));
        assertTrue(amountCheck.willOverflowOnAdd(Long.MAX_VALUE / 2 + 1, Long.MAX_VALUE / 2 + 1));
    }

    @Test
    @DisplayName("Should not detect overflow when sum equals Long.MAX_VALUE")
    void shouldNotDetectOverflowWhenSumEqualsMax() {
        assertFalse(amountCheck.willOverflowOnAdd(Long.MAX_VALUE - 1, 1L));
        assertFalse(amountCheck.willOverflowOnAdd(Long.MAX_VALUE / 2, Long.MAX_VALUE / 2));
    }

    @Test
    @DisplayName("Should not detect overflow for small amounts")
    void shouldNotDetectOverflowForSmallAmounts() {
        assertFalse(amountCheck.willOverflowOnAdd(100L, 200L));
        assertFalse(amountCheck.willOverflowOnAdd(1000L, 5000L));
        assertFalse(amountCheck.willOverflowOnAdd(0L, 100L));
    }

    @Test
    @DisplayName("Should not detect overflow when adding zero")
    void shouldNotDetectOverflowWhenAddingZero() {
        assertFalse(amountCheck.willOverflowOnAdd(Long.MAX_VALUE, 0L));
        assertFalse(amountCheck.willOverflowOnAdd(0L, Long.MAX_VALUE));
        assertFalse(amountCheck.willOverflowOnAdd(0L, 0L));
    }

    @Test
    @DisplayName("Should detect edge case overflow scenarios")
    void shouldDetectEdgeCaseOverflow() {
        long almostMax = Long.MAX_VALUE - 1;
        assertFalse(amountCheck.willOverflowOnAdd(almostMax, 1L));
        assertTrue(amountCheck.willOverflowOnAdd(almostMax, 2L));

        long largeNum = Long.MAX_VALUE / 2 + 1;
        assertTrue(amountCheck.willOverflowOnAdd(largeNum, largeNum));
    }

    @Test
    @DisplayName("Should handle commutative property of overflow check")
    void shouldHandleCommutativePropertyOfOverflow() {
        assertTrue(amountCheck.willOverflowOnAdd(Long.MAX_VALUE - 5, 10L));
        assertTrue(amountCheck.willOverflowOnAdd(10L, Long.MAX_VALUE - 5));

        assertFalse(amountCheck.willOverflowOnAdd(1000L, 2000L));
        assertFalse(amountCheck.willOverflowOnAdd(2000L, 1000L));
    }

    @Test
    @DisplayName("Should validate typical transaction scenarios")
    void shouldValidateTypicalTransactionScenarios() {
        long balance = 1000_0000L;
        long payment = 50_0000L;

        assertTrue(amountCheck.isPositive(payment));
        assertFalse(amountCheck.willOverflowOnAdd(balance, payment));
    }
}