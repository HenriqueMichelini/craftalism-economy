package io.github.HenriqueMichelini.craftalism.economy.domain.service.currency;

import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages.CurrencyMessages;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public class CurrencyParser {

    private static final BigDecimal MAX_ALLOWED_VALUE = new BigDecimal(Long.MAX_VALUE)
            .divide(CurrencyFormatter.DECIMAL_SCALE_BD, 4, RoundingMode.DOWN);

    private final CurrencyMessages messages;

    public CurrencyParser(CurrencyMessages messages) {
        this.messages = messages;
    }

    public Optional<Long> parseAmount(@NotNull Player player, @NotNull String input) {
        try {
            String trimmed = input.trim();

            if (trimmed.isEmpty()) {
                messages.sendAmountEmpty(player);
                return Optional.empty();
            }

            String cleaned = removeCurrencySymbols(trimmed);

            if (cleaned.isEmpty()) {
                messages.sendInvalidFormat(player);
                return Optional.empty();
            }

            BigDecimal decimal = new BigDecimal(cleaned);

            if (decimal.signum() <= 0) {
                messages.sendNonPositive(player);
                return Optional.empty();
            }

            if (decimal.compareTo(MAX_ALLOWED_VALUE) > 0) {
                messages.sendTooLarge(player);
                return Optional.empty();
            }

            BigDecimal scaled = decimal.multiply(CurrencyFormatter.DECIMAL_SCALE_BD);

            if (scaled.scale() > 0 && scaled.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                messages.sendInvalidAmount(player);
                return Optional.empty();
            }

            long amount = scaled.setScale(0, RoundingMode.HALF_UP).longValueExact();
            return Optional.of(amount);

        } catch (NumberFormatException e) {
            messages.sendInvalidFormat(player);
            return Optional.empty();

        } catch (ArithmeticException e) {
            messages.sendTooLarge(player);
            return Optional.empty();
        }
    }

    public Optional<Long> parseAmount(Player player, String input, boolean sendMessages) {
        if (!sendMessages) {
            return parseAmountSilently(input);
        }
        return parseAmount(player, input);
    }

    public Optional<Long> parseAmountSilently(String input) {
        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }

        try {
            String trimmed = input.trim();

            if (trimmed.isEmpty()) {
                return Optional.empty();
            }

            String cleaned = removeCurrencySymbols(trimmed);

            if (cleaned.isEmpty()) {
                return Optional.empty();
            }

            BigDecimal decimal = new BigDecimal(cleaned);

            if (decimal.signum() <= 0) {
                return Optional.empty();
            }

            if (decimal.compareTo(MAX_ALLOWED_VALUE) > 0) {
                return Optional.empty();
            }

            BigDecimal scaled = decimal.multiply(CurrencyFormatter.DECIMAL_SCALE_BD);

            if (scaled.scale() > 0 && scaled.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
                return Optional.empty();
            }

            long amount = scaled.setScale(0, RoundingMode.HALF_UP).longValueExact();
            return Optional.of(amount);

        } catch (NumberFormatException | ArithmeticException e) {
            return Optional.empty();
        }
    }

    public boolean isValidAmount(String input) {
        return parseAmountSilently(input).isPresent();
    }

    public BigDecimal getMaxAllowedValue() {
        return MAX_ALLOWED_VALUE;
    }

    private String removeCurrencySymbols(String input) {
        return input.replaceAll("[$€£¥₹₽₩¢]", "").trim();
    }

}