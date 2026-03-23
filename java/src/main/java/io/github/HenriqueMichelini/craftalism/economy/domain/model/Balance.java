package io.github.HenriqueMichelini.craftalism.economy.domain.model;

import java.util.UUID;

public class Balance {
    UUID uuid;
    Long amount;

    public Balance(UUID uuid, Long amount) {
        this.uuid = uuid;
        this.amount = amount;
    }

    public Balance() {}

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }
}