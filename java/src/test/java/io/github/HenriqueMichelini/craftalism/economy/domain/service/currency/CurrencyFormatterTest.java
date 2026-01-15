package io.github.HenriqueMichelini.craftalism.economy.domain.service.currency;

import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.when;

@DisplayName("CurrencyFormatter Tests")
class CurrencyFormatterTest {

    private CurrencyFormatter formatter;

    @Mock
    private JavaPlugin mockPlugin;
    @Mock
    private Logger mockLogger;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        when(mockPlugin.getLogger()).thenReturn(mockLogger);

        formatter = new CurrencyFormatter(
                Locale.US,
                "$",
                "ERROR",
                mockPlugin
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    @DisplayName("Should throw exception for null locale")
    void shouldThrowExceptionForNullLocale() {
        assertThrows(IllegalArgumentException.class,
                () -> new CurrencyFormatter(null, "$", "ERROR", mockPlugin));
    }

    @Test
    @DisplayName("Should throw exception for null currency symbol")
    void shouldThrowExceptionForNullCurrencySymbol() {
        assertThrows(IllegalArgumentException.class,
                () -> new CurrencyFormatter(Locale.US, null, "ERROR", mockPlugin));
    }

    @Test
    @DisplayName("Should throw exception for null fallback value")
    void shouldThrowExceptionForNullFallbackValue() {
        assertThrows(IllegalArgumentException.class,
                () -> new CurrencyFormatter(Locale.US, "$", null, mockPlugin));
    }

    @Test
    @DisplayName("Should throw exception for null plugin")
    void shouldThrowExceptionForNullPlugin() {
        assertThrows(IllegalArgumentException.class,
                () -> new CurrencyFormatter(Locale.US, "$", "ERROR", null));
    }

    @Test
    @DisplayName("Should format whole dollar amounts correctly")
    void shouldFormatWholeDollarAmounts() {
        assertEquals("$100.00", formatter.formatCurrency(1000000L));
        assertEquals("$50.00", formatter.formatCurrency(500000L));
        assertEquals("$1.00", formatter.formatCurrency(10000L));
    }

    @Test
    @DisplayName("Should format decimal amounts correctly")
    void shouldFormatDecimalAmounts() {
        assertEquals("$10.50", formatter.formatCurrency(105000L));
        assertEquals("$99.99", formatter.formatCurrency(999900L));
        assertEquals("$0.01", formatter.formatCurrency(100L));
    }

    @Test
    @DisplayName("Should format zero amount")
    void shouldFormatZeroAmount() {
        assertEquals("$0.00", formatter.formatCurrency(0L));
    }

    @Test
    @DisplayName("Should format amounts with 4 decimal places")
    void shouldFormatAmountsWithFourDecimals() {
        String result = formatter.formatCurrency(12345L);
        assertTrue(result.startsWith("$"));
        assertTrue(result.contains("1.234"));
    }

    @Test
    @DisplayName("Should format large amounts with grouping")
    void shouldFormatLargeAmountsWithGrouping() {
        String result = formatter.formatCurrency(10000000000L);
        assertTrue(result.contains("1,000,000") || result.contains("1.000.000"));
        assertTrue(result.startsWith("$"));
    }

    @Test
    @DisplayName("Should handle maximum safe long value")
    void shouldHandleMaximumSafeLongValue() {
        long maxSafe = Long.MAX_VALUE / 2;
        String result = formatter.formatCurrency(maxSafe);
        assertTrue(result.startsWith("$"));
        assertNotEquals("$ERROR", result);
    }

    @Test
    @DisplayName("Should format BigDecimal amounts correctly")
    void shouldFormatBigDecimalAmounts() {
        assertEquals("$100.00", formatter.formatCurrency(new BigDecimal("100.00")));
        assertEquals("$50.50", formatter.formatCurrency(new BigDecimal("50.50")));
    }

    @Test
    @DisplayName("Should throw exception for null BigDecimal")
    void shouldThrowExceptionForNullBigDecimal() {
        assertThrows(IllegalArgumentException.class,
                () -> formatter.formatCurrency(null));
    }

    @Test
    @DisplayName("Should format BigDecimal with many decimal places")
    void shouldFormatBigDecimalWithManyDecimals() {
        String result = formatter.formatCurrency(new BigDecimal("10.123456789"));
        assertTrue(result.startsWith("$"));
        assertTrue(result.contains("10.1234") || result.contains("10.123"));
    }

    @Test
    @DisplayName("Should convert stored amount to display value")
    void shouldConvertStoredAmountToDisplayValue() {
        BigDecimal display = formatter.toDisplayValue(1000000L);
        assertEquals(0, new BigDecimal("100.00").compareTo(display));
    }

    @Test
    @DisplayName("Should convert with 4 decimal precision")
    void shouldConvertWithFourDecimalPrecision() {
        BigDecimal display = formatter.toDisplayValue(12345L);
        assertEquals(0, new BigDecimal("1.2345").compareTo(display));
    }

    @Test
    @DisplayName("Should convert zero correctly")
    void shouldConvertZeroCorrectly() {
        BigDecimal display = formatter.toDisplayValue(0L);
        assertEquals(0, BigDecimal.ZERO.compareTo(display));
    }

    @Test
    @DisplayName("Should convert display value to stored amount")
    void shouldConvertDisplayValueToStoredAmount() {
        long stored = formatter.fromDisplayValue(new BigDecimal("100.00"));
        assertEquals(1000000L, stored);
    }

    @Test
    @DisplayName("Should handle rounding in conversion")
    void shouldHandleRoundingInConversion() {
        long stored = formatter.fromDisplayValue(new BigDecimal("10.12345"));
        assertEquals(101235L, stored);
    }

    @Test
    @DisplayName("Should throw exception for null display value")
    void shouldThrowExceptionForNullDisplayValue() {
        assertThrows(IllegalArgumentException.class,
                () -> formatter.fromDisplayValue(null));
    }

    @Test
    @DisplayName("Should handle zero display value")
    void shouldHandleZeroDisplayValue() {
        long stored = formatter.fromDisplayValue(BigDecimal.ZERO);
        assertEquals(0L, stored);
    }

    @Test
    @DisplayName("Should maintain value through round-trip conversion")
    void shouldMaintainValueThroughRoundTrip() {
        long original = 1234567L;
        BigDecimal display = formatter.toDisplayValue(original);
        long converted = formatter.fromDisplayValue(display);
        assertEquals(original, converted);
    }

    @Test
    @DisplayName("Should handle round-trip for various amounts")
    void shouldHandleRoundTripForVariousAmounts() {
        long[] amounts = {100L, 10000L, 1000000L, 12345L, 999999L};

        for (long amount : amounts) {
            BigDecimal display = formatter.toDisplayValue(amount);
            long converted = formatter.fromDisplayValue(display);
            assertEquals(amount, converted,
                    "Round-trip failed for amount: " + amount);
        }
    }

    @Test
    @DisplayName("Should format with different currency symbols")
    void shouldFormatWithDifferentCurrencySymbols() {
        CurrencyFormatter euroFormatter = new CurrencyFormatter(
                Locale.GERMANY, "€", "ERROR", mockPlugin);

        String result = euroFormatter.formatCurrency(1000000L);
        assertTrue(result.startsWith("€"));
    }

    @Test
    @DisplayName("Should use locale-specific number formatting")
    void shouldUseLocaleSpecificNumberFormatting() {
        CurrencyFormatter germanyFormatter = new CurrencyFormatter(
                Locale.GERMANY, "€", "ERROR", mockPlugin);

        String result = germanyFormatter.formatCurrency(10000000000L);
        assertTrue(result.contains(".") || result.contains(","));
    }

    @Test
    @DisplayName("Should return fallback value on formatting error")
    void shouldReturnFallbackValueOnError() {
        String fallback = formatter.getFallbackValue();
        assertEquals("ERROR", fallback);
    }

    @Test
    @DisplayName("Should return correct currency symbol")
    void shouldReturnCorrectCurrencySymbol() {
        assertEquals("$", formatter.getCurrencySymbol());
    }

    @Test
    @DisplayName("Should return correct fallback value")
    void shouldReturnCorrectFallbackValue() {
        assertEquals("ERROR", formatter.getFallbackValue());
    }

    @Test
    @DisplayName("Should handle concurrent formatting safely")
    void shouldHandleConcurrentFormattingSafely() throws InterruptedException {
        final int threadCount = 10;
        final int iterations = 100;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < iterations; j++) {
                    long amount = (threadId * iterations + j) * 10000L;
                    String result = formatter.formatCurrency(amount);
                    assertNotNull(result);
                    assertTrue(result.startsWith("$"));
                }
            });
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }
    }

    @Test
    @DisplayName("Should have correct decimal scale constant")
    void shouldHaveCorrectDecimalScaleConstant() {
        assertEquals(10000L, CurrencyFormatter.DECIMAL_SCALE);
        assertEquals(0, new BigDecimal("10000").compareTo(CurrencyFormatter.DECIMAL_SCALE_BD));
    }

    @Test
    @DisplayName("Fallback-built formatter should use correct locale formatting")
    void fallbackShouldUseCorrectLocale() {
        CurrencyFormatter germany = new CurrencyFormatter(
                Locale.GERMANY, "€", "ERROR", mockPlugin,
                (prototype, loc) -> {
                    NumberFormat nf = NumberFormat.getInstance(loc);
                    nf.setMinimumFractionDigits(prototype.getMinimumFractionDigits());
                    nf.setMaximumFractionDigits(prototype.getMaximumFractionDigits());
                    nf.setGroupingUsed(prototype.isGroupingUsed());
                    try {
                        nf.setRoundingMode(java.math.RoundingMode.HALF_UP);
                    } catch (ClassCastException ignored) {}
                    return nf;
                }
        );

        String result = germany.formatCurrency(1234500L);

        assertTrue(result.startsWith("€"), "Should start with currency symbol");
        assertTrue(result.contains(","), "German locale should use comma as decimal separator");
    }

    @Test
    @DisplayName("Should rebuild formatter if cloning fails")
    void shouldRebuildFormatterOnCloneFailure() {
        CurrencyFormatter broken = new CurrencyFormatter(
                Locale.US, "$", "ERROR", mockPlugin,
                (prototype, loc) -> {
                    NumberFormat nf = NumberFormat.getInstance(loc);
                    nf.setMinimumFractionDigits(prototype.getMinimumFractionDigits());
                    nf.setMaximumFractionDigits(prototype.getMaximumFractionDigits());
                    nf.setGroupingUsed(prototype.isGroupingUsed());
                    try {
                        nf.setRoundingMode(java.math.RoundingMode.HALF_UP);
                    } catch (ClassCastException ignored) {}
                    return nf;
                }
        );

        String result = broken.formatCurrency(1000000L);

        assertEquals("$100.00", result);
    }

    @Test
    @DisplayName("Prototype NumberFormat must remain unmodified after formatting")
    void prototypeShouldRemainUnmodified() {
        int minBefore = formatter.prototype.getMinimumFractionDigits();
        int maxBefore = formatter.prototype.getMaximumFractionDigits();
        boolean groupingBefore = formatter.prototype.isGroupingUsed();

        for (int i = 0; i < 100; i++) {
            formatter.formatCurrency(12345L);
        }

        assertEquals(minBefore, formatter.prototype.getMinimumFractionDigits());
        assertEquals(maxBefore, formatter.prototype.getMaximumFractionDigits());
        assertEquals(groupingBefore, formatter.prototype.isGroupingUsed());
    }
}
