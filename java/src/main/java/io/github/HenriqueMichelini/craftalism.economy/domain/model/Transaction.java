package io.github.HenriqueMichelini.craftalism.economy.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Transaction {
    Long id;
    UUID fromUuid;
    UUID toUuid;
    Long amount;
    Instant createdAt;

    public Transaction() {}

    public Transaction(Long id, UUID fromUuid, UUID toUuid, Long amount, Instant createdAt) {
        this.id = id;
        this.fromUuid = fromUuid;
        this.toUuid = toUuid;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getFromUuid() {
        return fromUuid;
    }

    public void setFromUuid(UUID fromUuid) {
        this.fromUuid = fromUuid;
    }

    public UUID getToUuid() {
        return toUuid;
    }

    public void setToUuid(UUID toUuid) {
        this.toUuid = toUuid;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}