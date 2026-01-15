package io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions;

public class ServerException extends ApiException {
    public ServerException() {
        super();
    }

    public ServerException(String message) {
        super(message);
    }

    public ServerException(String message, Throwable cause) {
        super(message, cause);
    }
}