package io.github.HenriqueMichelini.craftalism.economy.infra.api.dto;

import java.util.UUID;

public record TransactionRequestDTO(UUID fromPlayerUuid, UUID toPlayerUuid, Long amount) {}