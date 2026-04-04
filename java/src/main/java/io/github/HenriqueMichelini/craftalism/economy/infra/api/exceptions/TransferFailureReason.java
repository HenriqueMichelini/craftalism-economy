package io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions;

public enum TransferFailureReason {
    INSUFFICIENT_FUNDS,
    SENDER_NOT_FOUND,
    RECEIVER_NOT_FOUND,
    INVALID_REQUEST,
    DUPLICATE_REQUEST,
    GENERIC_FAILURE
}
