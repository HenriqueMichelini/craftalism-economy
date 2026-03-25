package io.github.HenriqueMichelini.craftalism.economy.application.service;

import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AsyncExceptionResolver tests")
class AsyncExceptionResolverTest {

    @Test
    @DisplayName("should unwrap nested async exceptions")
    void shouldUnwrapNestedAsyncExceptions() {
        RuntimeException root = new RuntimeException("root");
        Throwable wrapped = new CompletionException(new ExecutionException(root));

        Throwable result = AsyncExceptionResolver.unwrap(wrapped);

        assertSame(root, result);
    }

    @Test
    @DisplayName("should detect exception in cause chain")
    void shouldDetectExceptionInCauseChain() {
        Throwable error = new CompletionException(new IllegalStateException(new NotFoundException()));

        assertTrue(AsyncExceptionResolver.isCausedBy(error, NotFoundException.class));
    }

    @Test
    @DisplayName("should return false when exception type is absent")
    void shouldReturnFalseWhenExceptionTypeIsAbsent() {
        Throwable error = new CompletionException(new IllegalStateException("not found"));

        assertFalse(AsyncExceptionResolver.isCausedBy(error, NotFoundException.class));
    }
}
