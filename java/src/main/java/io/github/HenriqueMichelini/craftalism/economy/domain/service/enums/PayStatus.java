package io.github.HenriqueMichelini.craftalism.economy.domain.service.enums;

public enum PayStatus {
    SUCCESS,
    TARGET_NOT_FOUND,
    NOT_ENOUGH_FUNDS,
    INVALID_AMOUNT,
    CANNOT_PAY_SELF,
    TRANSFER_DUPLICATE,
    TRANSFER_INVALID_REQUEST,
    TRANSFER_TEMPORARILY_UNAVAILABLE,
    TRANSFER_ENDPOINT_UNAVAILABLE,
    ERROR,
    NO_PERMISSION
}
