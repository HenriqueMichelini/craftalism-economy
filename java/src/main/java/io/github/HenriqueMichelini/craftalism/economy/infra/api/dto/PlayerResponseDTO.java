package io.github.HenriqueMichelini.craftalism.economy.infra.api.dto;

import java.time.Instant;
import java.util.UUID;

public record PlayerResponseDTO(UUID uuid, String name, Instant createdAt) {}