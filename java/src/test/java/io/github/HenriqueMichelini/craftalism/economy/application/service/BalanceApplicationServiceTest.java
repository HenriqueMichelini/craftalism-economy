package io.github.HenriqueMichelini.craftalism.economy.application.service;

import io.github.HenriqueMichelini.craftalism.economy.domain.model.Balance;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.BalanceResponseDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.NotFoundException;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.repository.BalanceCacheRepository;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.BalanceApiService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BalanceApplicationServiceTest {

    @Mock
    private BalanceApiService api;
    @Mock
    private BalanceCacheRepository cache;

    private BalanceApplicationService service;

    private final UUID playerUuid = UUID.randomUUID();

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        service = new BalanceApplicationService(api, cache);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void getBalance_ShouldReturnBalance_WhenFound() {
        Long amount = 1500000L;
        BalanceResponseDTO dto = new BalanceResponseDTO(playerUuid, amount);
        when(api.getBalance(playerUuid))
                .thenReturn(CompletableFuture.completedFuture(dto));

        Optional<Balance> result = service.getBalance(playerUuid).join();

        assertTrue(result.isPresent());
        assertEquals(playerUuid, result.get().getUuid());
        assertEquals(amount, result.get().getAmount());
    }

    @Test
    void getBalance_ShouldReturnEmpty_WhenNotFound() {
        when(api.getBalance(playerUuid))
                .thenReturn(CompletableFuture.failedFuture(new NotFoundException("Not found")));

        Optional<Balance> result = service.getBalance(playerUuid).join();

        assertTrue(result.isEmpty());
    }

    @Test
    void getBalance_ShouldThrowException_WhenOtherErrorOccurs() {
        when(api.getBalance(playerUuid))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API Error")));

        assertThrows(Exception.class, () -> service.getBalance(playerUuid).join());
    }

    @Test
    void getOrCreateBalance_ShouldReturnExisting_WhenBalanceExists() {
        Long amount = 2000000L;
        BalanceResponseDTO dto = new BalanceResponseDTO(playerUuid, amount);
        when(api.getBalance(playerUuid))
                .thenReturn(CompletableFuture.completedFuture(dto));

        Balance result = service.getOrCreateBalance(playerUuid).join();

        assertEquals(playerUuid, result.getUuid());
        assertEquals(amount, result.getAmount());
        verify(api, never()).createBalance(any());
    }

    @Test
    void getOrCreateBalance_ShouldCreateNew_WhenBalanceNotFound() {
        Long amount = 0L;
        BalanceResponseDTO dto = new BalanceResponseDTO(playerUuid, amount);

        when(api.getBalance(playerUuid))
                .thenReturn(CompletableFuture.failedFuture(new NotFoundException("Not found")));
        when(api.createBalance(playerUuid))
                .thenReturn(CompletableFuture.completedFuture(dto));

        Balance result = service.getOrCreateBalance(playerUuid).join();

        assertEquals(playerUuid, result.getUuid());
        assertEquals(amount, result.getAmount());
        verify(api).createBalance(playerUuid);
    }

    @Test
    void loadBalanceOnJoin_ShouldCacheBalance_WhenLoaded() {
        Long amount = 3000000L;
        BalanceResponseDTO dto = new BalanceResponseDTO(playerUuid, amount);
        when(api.getBalance(playerUuid))
                .thenReturn(CompletableFuture.completedFuture(dto));

        Balance result = service.loadBalanceOnJoin(playerUuid).join();

        assertEquals(amount, result.getAmount());
        verify(cache).save(result);
    }

    @Test
    void syncBalance_ShouldUpdateCache_WhenSynced() {
        Long amount = 4500000L;
        BalanceResponseDTO dto = new BalanceResponseDTO(playerUuid, amount);
        when(api.getBalance(playerUuid))
                .thenReturn(CompletableFuture.completedFuture(dto));

        Balance result = service.syncBalance(playerUuid).join();

        assertEquals(amount, result.getAmount());
        verify(cache).save(result);
    }

    @Test
    void getCachedOrFetch_ShouldReturnCached_WhenAvailable() {
        Long amount = 6000000L;
        Balance cachedBalance = new Balance(playerUuid, amount);
        when(cache.find(playerUuid)).thenReturn(Optional.of(cachedBalance));

        Balance result = service.getCachedOrFetch(playerUuid).join();

        assertEquals(amount, result.getAmount());
        verifyNoInteractions(api);
    }

    @Test
    void getCachedOrFetch_ShouldFetchAndCache_WhenNotCached() {
        Long amount = 7000000L;
        BalanceResponseDTO dto = new BalanceResponseDTO(playerUuid, amount);

        when(cache.find(playerUuid)).thenReturn(Optional.empty());
        when(api.getBalance(playerUuid))
                .thenReturn(CompletableFuture.completedFuture(dto));

        Balance result = service.getCachedOrFetch(playerUuid).join();

        assertEquals(amount, result.getAmount());
        verify(cache).save(any(Balance.class));
    }
}