package io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions;

public class BadRequestException extends ClientException {

    public BadRequestException() {
        super("Bad request");
    }

    public BadRequestException(String message) {
        super(message);
    }
}