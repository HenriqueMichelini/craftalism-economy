package io.github.HenriqueMichelini.craftalism.economy.domain.service.currency;

import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages.CurrencyMessages;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CurrencyParser Tests")
class CurrencyParserTest {

    private CurrencyParser parser;
    private CurrencyMessages messages;

    @Mock
    private Player player;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        messages = mock(CurrencyMessages.class);
        parser = new CurrencyParser(messages);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    @DisplayName("Should parse valid decimal amount correctly")
    void shouldParseValidDecimalAmount() {
        Optional<Long> result = parser.parseAmount(player, "100.50");

        assertTrue(result.isPresent());
        assertEquals(1005000L, result.get()); // 100.50 * 10000
        verifyNoInteractions(messages);
    }

    @Test
    @DisplayName("Should parse integer amount correctly")
    void shouldParseIntegerAmount() {
        Optional<Long> result = parser.parseAmount(player, "250");

        assertTrue(result.isPresent());
        assertEquals(2500000L, result.get()); // 250 * 10000
    }

    @Test
    @DisplayName("Should parse amount with 4 decimal places")
    void shouldParseAmountWithFourDecimals() {
        Optional<Long> result = parser.parseAmount(player, "10.1234");

        assertTrue(result.isPresent());
        assertEquals(101234L, result.get());
    }

    @Test
    @DisplayName("Should reject empty input")
    void shouldRejectEmptyInput() {
        Optional<Long> result = parser.parseAmount(player, "");

        assertFalse(result.isPresent());
        verify(messages).sendAmountEmpty(player);
    }

    @Test
    @DisplayName("Should reject whitespace-only input")
    void shouldRejectWhitespaceOnlyInput() {
        Optional<Long> result = parser.parseAmount(player, "   ");

        assertFalse(result.isPresent());
        verify(messages).sendAmountEmpty(player);
    }

    @Test
    @DisplayName("Should trim and parse amount with whitespace")
    void shouldTrimAndParseAmountWithWhitespace() {
        Optional<Long> result = parser.parseAmount(player, "  50.25  ");

        assertTrue(result.isPresent());
        assertEquals(502500L, result.get());
    }

    @Test
    @DisplayName("Should remove currency symbols and parse")
    void shouldRemoveCurrencySymbolsAndParse() {
        assertTrue(parser.parseAmount(player, "$100.00").isPresent());
        assertTrue(parser.parseAmount(player, "€50.00").isPresent());
        assertTrue(parser.parseAmount(player, "£75.50").isPresent());
        assertTrue(parser.parseAmount(player, "¥1000").isPresent());
        assertTrue(parser.parseAmount(player, "₹500.25").isPresent());
        assertTrue(parser.parseAmount(player, "₽200").isPresent());
        assertTrue(parser.parseAmount(player, "₩5000").isPresent());
        assertTrue(parser.parseAmount(player, "¢99").isPresent());
    }

    @Test
    @DisplayName("Should reject input with only currency symbols")
    void shouldRejectOnlyCurrencySymbols() {
        Optional<Long> result = parser.parseAmount(player, "$$$");

        assertFalse(result.isPresent());
        verify(messages).sendInvalidFormat(player);
    }

    @Test
    @DisplayName("Should reject zero amount")
    void shouldRejectZeroAmount() {
        Optional<Long> result = parser.parseAmount(player, "0");

        assertFalse(result.isPresent());
        verify(messages).sendNonPositive(player);
    }

    @Test
    @DisplayName("Should reject negative amount")
    void shouldRejectNegativeAmount() {
        Optional<Long> result = parser.parseAmount(player, "-50.00");

        assertFalse(result.isPresent());
        verify(messages).sendNonPositive(player);
    }

    @Test
    @DisplayName("Should reject amount exceeding maximum allowed value")
    void shouldRejectAmountExceedingMax() {
        BigDecimal maxValue = parser.getMaxAllowedValue();
        String tooLarge = maxValue.add(BigDecimal.ONE).toString();

        Optional<Long> result = parser.parseAmount(player, tooLarge);

        assertFalse(result.isPresent());
        verify(messages).sendTooLarge(player);
    }

    @Test
    @DisplayName("Should accept amount at maximum allowed value")
    void shouldAcceptAmountAtMaxValue() {
        BigDecimal maxValue = parser.getMaxAllowedValue();

        Optional<Long> result = parser.parseAmount(player, maxValue.toString());

        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("Should reject invalid number format")
    void shouldRejectInvalidNumberFormat() {
        Optional<Long> result = parser.parseAmount(player, "abc");

        assertFalse(result.isPresent());
        verify(messages).sendInvalidFormat(player);
    }

    @Test
    @DisplayName("Should reject amount with more than 4 decimal places")
    void shouldRejectMoreThanFourDecimals() {
        // 10.12345 has 5 decimal places
        Optional<Long> result = parser.parseAmount(player, "10.12345");

        assertFalse(result.isPresent());
        verify(messages).sendInvalidAmount(player);
    }

    @Test
    @DisplayName("Should accept exactly 4 decimal places")
    void shouldAcceptExactlyFourDecimals_() {
        Optional<Long> result = parser.parseAmount(player, "10.5555");

        // 10.5555 * 10000 = 105555 (no fractional part), so it's valid
        assertTrue(result.isPresent());
        assertEquals(105555L, result.get());
    }

    @Test
    @DisplayName("Should reject 5 decimal places that create fractional parts")
    void shouldRejectFiveDecimalsWithFraction() {
        // 10.55555 * 10000 = 105555.5 (has fractional part after scaling)
        Optional<Long> result = parser.parseAmount(player, "10.55555");

        assertFalse(result.isPresent());
        verify(messages).sendInvalidAmount(player);
    }

    @ParameterizedTest
    @ValueSource(strings = {"100", "50.5", "0.01", "999.9999", "1000000"})
    @DisplayName("Should parse various valid formats")
    void shouldParseVariousValidFormats(String input) {
        Optional<Long> result = parser.parseAmount(player, input);
        assertTrue(result.isPresent());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "-1", "0", "abc", "10.123456", "999999999999999999"})
    @DisplayName("Should reject various invalid formats")
    void shouldRejectVariousInvalidFormats(String input) {
        Optional<Long> result = parser.parseAmount(player, input);
        assertFalse(result.isPresent());
    }

    // parseAmountSilently() tests
    @Test
    @DisplayName("Should parse silently without sending messages")
    void shouldParseSilentlyWithoutMessages() {
        Optional<Long> result = parser.parseAmountSilently("100.00");

        assertTrue(result.isPresent());
        assertEquals(1000000L, result.get());
        verifyNoInteractions(messages);
    }

    @Test
    @DisplayName("Should throw exception for null input in silent parse")
    void shouldThrowExceptionForNullInputInSilentParse() {
        assertThrows(IllegalArgumentException.class,
                () -> parser.parseAmountSilently(null));
    }

    @Test
    @DisplayName("Should return empty for invalid input in silent parse")
    void shouldReturnEmptyForInvalidInputInSilentParse() {
        assertFalse(parser.parseAmountSilently("").isPresent());
        assertFalse(parser.parseAmountSilently("invalid").isPresent());
        assertFalse(parser.parseAmountSilently("-50").isPresent());
        verifyNoInteractions(messages);
    }

    // parseAmount with sendMessages flag
    @Test
    @DisplayName("Should send messages when flag is true")
    void shouldSendMessagesWhenFlagIsTrue() {
        parser.parseAmount(player, "invalid", true);
        verify(messages).sendInvalidFormat(player);
    }

    @Test
    @DisplayName("Should not send messages when flag is false")
    void shouldNotSendMessagesWhenFlagIsFalse() {
        Optional<Long> result = parser.parseAmount(player, "invalid", false);

        assertFalse(result.isPresent());
        verifyNoInteractions(messages);
    }

    // isValidAmount() tests
    @Test
    @DisplayName("Should validate correct amounts")
    void shouldValidateCorrectAmounts() {
        assertTrue(parser.isValidAmount("100"));
        assertTrue(parser.isValidAmount("50.25"));
        assertTrue(parser.isValidAmount("$100.00"));
        assertTrue(parser.isValidAmount("0.01"));
    }

    @Test
    @DisplayName("Should invalidate incorrect amounts")
    void shouldInvalidateIncorrectAmounts() {
        assertFalse(parser.isValidAmount(""));
        assertFalse(parser.isValidAmount("invalid"));
        assertFalse(parser.isValidAmount("-50"));
        assertFalse(parser.isValidAmount("0"));
        assertFalse(parser.isValidAmount("10.123456"));
    }

    // Edge cases
    @Test
    @DisplayName("Should handle very small valid amounts")
    void shouldHandleVerySmallValidAmounts() {
        Optional<Long> result = parser.parseAmount(player, "0.0001");

        assertTrue(result.isPresent());
        assertEquals(1L, result.get()); // 0.0001 * 10000 = 1
    }

    @Test
    @DisplayName("Should accept exactly 4 decimal places")
    void shouldAcceptExactlyFourDecimals() {
        Optional<Long> result = parser.parseAmount(player, "10.5555");

        // 10.5555 * 10000 = 105555 (no fractional part), so it's valid
        assertTrue(result.isPresent());
        assertEquals(105555L, result.get());
    }

    @Test
    @DisplayName("Should handle multiple currency symbols")
    void shouldHandleMultipleCurrencySymbols() {
        Optional<Long> result = parser.parseAmount(player, "$€£100.00");

        assertTrue(result.isPresent());
        assertEquals(1000000L, result.get());
    }

    @Test
    @DisplayName("Should accept valid scientific notation within limits")
    void shouldAcceptValidScientificNotation() {
        Optional<Long> result = parser.parseAmount(player, "1e2");

        // 1e2 = 100, which is valid and within max value
        assertTrue(result.isPresent());
        assertEquals(1000000L, result.get()); // 100 * 10000
    }

    @Test
    @DisplayName("Should reject scientific notation exceeding max value")
    void shouldRejectScientificNotationExceedingMax() {
        // 1e20 is way beyond the max allowed value
        Optional<Long> result = parser.parseAmount(player, "1e20");

        assertFalse(result.isPresent());
        verify(messages).sendTooLarge(player);
    }

    @Test
    @DisplayName("Should calculate max allowed value correctly")
    void shouldCalculateMaxAllowedValueCorrectly() {
        BigDecimal maxAllowed = parser.getMaxAllowedValue();

        assertNotNull(maxAllowed);
        assertTrue(maxAllowed.compareTo(BigDecimal.ZERO) > 0);
        assertTrue(maxAllowed.compareTo(new BigDecimal(Long.MAX_VALUE)) < 0);
    }
}
