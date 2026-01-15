package io.github.HenriqueMichelini.craftalism.economy.domain.service.validators;

public class AmountCheck {
    public AmountCheck() {}
    public boolean isPositive(long amount) {
        return amount > 0;
    }

    public boolean willOverflowOnAdd(long amount1, long amount2) {
        return amount1 > Long.MAX_VALUE - amount2;
    }
}