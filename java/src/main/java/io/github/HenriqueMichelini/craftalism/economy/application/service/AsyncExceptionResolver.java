package io.github.HenriqueMichelini.craftalism.economy.application.service;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

final class AsyncExceptionResolver {

    private AsyncExceptionResolver() {
    }

    static Throwable unwrap(Throwable exception) {
        Throwable cause = exception;
        while (cause != null
                && (cause instanceof CompletionException || cause instanceof ExecutionException)
                && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause == null ? exception : cause;
    }

    static boolean isCausedBy(Throwable exception, Class<? extends Throwable> expectedType) {
        Throwable cause = exception;
        while (cause != null) {
            if (expectedType.isInstance(cause)) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
