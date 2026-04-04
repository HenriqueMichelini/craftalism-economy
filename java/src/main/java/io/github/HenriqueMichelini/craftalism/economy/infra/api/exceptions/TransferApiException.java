package io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions;

public class TransferApiException extends ApiException {

    private final TransferFailureReason reason;

    public TransferApiException(TransferFailureReason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public TransferFailureReason getReason() {
        return reason;
    }
}
