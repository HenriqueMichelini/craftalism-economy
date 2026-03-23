package io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions;

public class ApiServerException extends ServerException {

    public ApiServerException() {
        super("Server error");
    }

    public ApiServerException(String message) {
        super(message);
    }

    public ApiServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
