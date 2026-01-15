package io.github.HenriqueMichelini.craftalism.economy.application.service;

import io.github.HenriqueMichelini.craftalism.economy.application.dto.BalanceExecutionResult;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.enums.BalanceStatus;
import io.github.HenriqueMichelini.craftalism.economy.domain.model.Balance;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BalanceCommandApplicationServiceTest {
    @Mock private PlayerApplicationService playerService;
    @Mock private BalanceApplicationService balanceService;

    private BalanceCommandApplicationService service;

    private final UUID playerUuid = UUID.randomUUID();

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new BalanceCommandApplicationService(playerService, balanceService);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void executeSelf_ShouldReturnSuccessSelf_WhenBalanceFound() {
        Long amount = 5000000L;
        Balance balance = new Balance(playerUuid, amount);
        when(balanceService.getOrCreateBalance(playerUuid))
                .thenReturn(CompletableFuture.completedFuture(balance));

        BalanceExecutionResult result = service.executeSelf(playerUuid).join();

        assertEquals(BalanceStatus.SUCCESS_SELF, result.status());
        assertEquals(amount, result.amount());
        verify(balanceService).getOrCreateBalance(playerUuid);
    }

    @Test
    void executeSelf_ShouldReturnError_WhenExceptionOccurs() {
        when(balanceService.getOrCreateBalance(playerUuid))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API Error")));

        BalanceExecutionResult result = service.executeSelf(playerUuid).join();

        assertEquals(BalanceStatus.ERROR, result.status());
        assertEquals(0L, result.amount());
    }

    @Test
    void executeOther_ShouldReturnSuccessOther_WhenPlayerAndBalanceExist() {
        String playerName = "TestPlayer";
        Long amount = 10000000L;
        Balance balance = new Balance(playerUuid, amount);

        when(playerService.getUuidByName(playerName))
                .thenReturn(CompletableFuture.completedFuture(playerUuid));
        when(balanceService.getBalance(playerUuid))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(balance)));

        BalanceExecutionResult result = service.executeOther(playerName).join();

        assertEquals(BalanceStatus.SUCCESS_OTHER, result.status());
        assertEquals(amount, result.amount());
    }

    @Test
    void executeOther_ShouldReturnNoBalance_WhenBalanceNotFound() {
        String playerName = "PlayerWithoutBalance";

        when(playerService.getUuidByName(playerName))
                .thenReturn(CompletableFuture.completedFuture(playerUuid));
        when(balanceService.getBalance(playerUuid))
                .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        BalanceExecutionResult result = service.executeOther(playerName).join();

        assertEquals(BalanceStatus.NO_BALANCE, result.status());
        assertEquals(0L, result.amount());
    }

    @Test
    void executeOther_ShouldReturnNotFound_WhenPlayerNotFound() {
        String playerName = "UnknownPlayer";

        when(playerService.getUuidByName(playerName))
                .thenReturn(CompletableFuture.failedFuture(new NotFoundException("Player not found")));

        BalanceExecutionResult result = service.executeOther(playerName).join();

        assertEquals(BalanceStatus.NOT_FOUND, result.status());
        assertEquals(0L, result.amount());
    }

    @Test
    void executeOther_ShouldReturnError_WhenUnexpectedExceptionOccurs() {
        String playerName = "TestPlayer";

        when(playerService.getUuidByName(playerName))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Database error")));

        BalanceExecutionResult result = service.executeOther(playerName).join();

        assertEquals(BalanceStatus.ERROR, result.status());
    }
}