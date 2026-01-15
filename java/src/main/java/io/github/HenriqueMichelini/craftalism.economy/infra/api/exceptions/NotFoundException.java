package io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions;

public class NotFoundException extends ClientException {
    public NotFoundException() {
        super();
    }

    public NotFoundException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
