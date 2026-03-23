package io.github.HenriqueMichelini.craftalism.economy.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Player {
    UUID uuid;
    String name;
    Instant createdAt;

    public Player(UUID uuid, String name, Instant createdAt) {
        this.uuid = uuid;
        this.name = name;
        this.createdAt = createdAt;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}