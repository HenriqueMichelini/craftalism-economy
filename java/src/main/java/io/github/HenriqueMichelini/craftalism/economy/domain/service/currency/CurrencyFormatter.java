package io.github.HenriqueMichelini.craftalism.economy.domain.service.currency;

import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyFormatter {
    @FunctionalInterface
    public interface FormatterFactory {
        NumberFormat create(NumberFormat prototype, Locale locale);
    }

    private final String currencySymbol;
    private final String fallbackValue;
    private final JavaPlugin plugin;

    final NumberFormat prototype;
    private final Locale locale;
    private final FormatterFactory factory;

    public static final long DECIMAL_SCALE = 10000;
    public static final BigDecimal DECIMAL_SCALE_BD = BigDecimal.valueOf(DECIMAL_SCALE);

    public CurrencyFormatter(Locale locale, String currencySymbol, String fallbackValue, JavaPlugin plugin) {
        this(locale, currencySymbol, fallbackValue, plugin,
                (prototype, loc) -> {
                    try {
                        return (NumberFormat) prototype.clone();
                    } catch (Exception e) {
                        NumberFormat nf = NumberFormat.getInstance(loc);
                        nf.setMinimumFractionDigits(prototype.getMinimumFractionDigits());
                        nf.setMaximumFractionDigits(prototype.getMaximumFractionDigits());
                        nf.setGroupingUsed(prototype.isGroupingUsed());
                        try {
                            nf.setRoundingMode(RoundingMode.HALF_UP);
                        } catch (ClassCastException ignored) {}
                        return nf;
                    }
                }
        );
    }

    public CurrencyFormatter(Locale locale,
                             String currencySymbol,
                             String fallbackValue,
                             JavaPlugin plugin,
                             FormatterFactory factory) {

        if (locale == null) throw new IllegalArgumentException("Locale cannot be null");
        if (currencySymbol == null) throw new IllegalArgumentException("Currency symbol cannot be null");
        if (fallbackValue == null) throw new IllegalArgumentException("Fallback value cannot be null");
        if (plugin == null) throw new IllegalArgumentException("Plugin cannot be null");
        if (factory == null) throw new IllegalArgumentException("FormatterFactory cannot be null");

        this.locale = locale;
        this.currencySymbol = currencySymbol;
        this.fallbackValue = fallbackValue;
        this.plugin = plugin;
        this.factory = factory;

        NumberFormat nf = NumberFormat.getInstance(locale);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(4);
        nf.setGroupingUsed(true);
        try {
            nf.setRoundingMode(RoundingMode.HALF_UP);
        } catch (ClassCastException ignored) {}

        this.prototype = nf;
    }

    private NumberFormat newFormatter() {
        return factory.create(prototype, locale);
    }

    public String formatCurrency(long amount) {
        try {
            BigDecimal displayValue = toDisplayValue(amount); // PUT THIS BACK
            NumberFormat fmt = newFormatter();
            return currencySymbol + fmt.format(displayValue);
        } catch (Exception e) {
            plugin.getLogger().warning("Error formatting currency amount " + amount + ": " + e.getMessage());
            return currencySymbol + fallbackValue;
        }
    }

    public String formatCurrency(BigDecimal amount) {
        if (amount == null) throw new IllegalArgumentException("Amount cannot be null");
        try {
            NumberFormat fmt = newFormatter();
            return currencySymbol + fmt.format(amount);
        } catch (Exception e) {
            plugin.getLogger().warning("Error formatting BigDecimal currency amount " + amount + ": " + e.getMessage());
            return currencySymbol + fallbackValue;
        }
    }

    public BigDecimal toDisplayValue(long amount) {
        return BigDecimal.valueOf(amount).divide(DECIMAL_SCALE_BD, 4, RoundingMode.HALF_UP);
    }

    public long fromDisplayValue(BigDecimal displayValue) {
        if (displayValue == null) throw new IllegalArgumentException("Display value cannot be null");
        return displayValue.multiply(DECIMAL_SCALE_BD)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public String getFallbackValue() {
        return fallbackValue;
    }
}
