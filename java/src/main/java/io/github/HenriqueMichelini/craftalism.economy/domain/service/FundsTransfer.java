package io.github.HenriqueMichelini.craftalism.economy.domain.service;

import io.github.HenriqueMichelini.craftalism.economy.domain.model.Balance;

public class FundsTransfer {
    public FundsTransfer() {
    }

    public void transfer(Balance from, Balance to, Long amount) {
        from.setAmount(from.getAmount() - amount);
        to.setAmount(to.getAmount() + amount);
    }
}