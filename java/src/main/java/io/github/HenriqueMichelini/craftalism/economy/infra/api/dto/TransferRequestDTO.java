package io.github.HenriqueMichelini.craftalism.economy.infra.api.dto;

import java.util.UUID;

public record TransferRequestDTO(UUID fromPlayerUuid, UUID toPlayerUuid, long amount) {}
