package io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions;

public class RateLimitException extends ClientException {

    public RateLimitException() {
        super("Rate limit exceeded");
    }

    public RateLimitException(String message) {
        super(message);
    }
}