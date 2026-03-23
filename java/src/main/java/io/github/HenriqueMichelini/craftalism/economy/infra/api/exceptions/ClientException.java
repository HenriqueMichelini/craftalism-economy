package io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions;

public class ClientException extends ApiException {
    public ClientException() {
        super();
    }

    public ClientException(String message) {
        super(message);
    }

    public ClientException(String message, Throwable cause) {
        super(message, cause);
    }
}