package io.github.HenriqueMichelini.craftalism.economy.infra.api.dto;

import java.util.UUID;

public record PlayerRequestDTO(UUID uuid, String name) {}