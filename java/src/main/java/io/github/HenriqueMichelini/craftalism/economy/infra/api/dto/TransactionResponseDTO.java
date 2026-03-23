package io.github.HenriqueMichelini.craftalism.economy.infra.api.dto;

import java.time.Instant;
import java.util.UUID;

public record TransactionResponseDTO(Long id, UUID fromPlayerUuid, UUID toPlayerUuid, Long amount, Instant createdAt) {}