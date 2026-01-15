package io.github.HenriqueMichelini.craftalism.economy.application.service;

import io.github.HenriqueMichelini.craftalism.economy.application.dto.SetBalanceExecutionResult;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.enums.SetBalanceStatus;
import io.github.HenriqueMichelini.craftalism.economy.domain.model.Player;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.NotFoundException;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.BalanceApiService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("SetBalanceCommandApplicationService Tests")
class SetBalanceCommandApplicationServiceTest {

    @Mock
    private BalanceApiService balanceApi;
    @Mock
    private PlayerApplicationService playerService;

    private SetBalanceCommandApplicationService service;

    private String testPlayerName;
    private UUID testPlayerUuid;
    private Player testPlayer;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        service = new SetBalanceCommandApplicationService(balanceApi, playerService);

        testPlayerName = "TestPlayer";
        testPlayerUuid = UUID.randomUUID();
        testPlayer = new Player(testPlayerUuid, testPlayerName, Instant.now());
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    @DisplayName("Should set balance successfully")
    void shouldSetBalanceSuccessfully() throws ExecutionException, InterruptedException {
        long newBalance = 1000_0000L;

        when(playerService.getPlayerByName(testPlayerName))
                .thenReturn(CompletableFuture.completedFuture(testPlayer));
        when(balanceApi.updateBalance(testPlayerUuid, newBalance))
                .thenReturn(CompletableFuture.completedFuture(null));

        SetBalanceExecutionResult result = service.execute(testPlayerName, newBalance).get();

        assertTrue(result.isSuccess());
        assertEquals(SetBalanceStatus.SUCCESS, result.getStatus());

        verify(playerService).getPlayerByName(testPlayerName);
        verify(balanceApi).updateBalance(testPlayerUuid, newBalance);
    }

    @Test
    @DisplayName("Should set balance to zero")
    void shouldSetBalanceToZero() throws ExecutionException, InterruptedException {
        when(playerService.getPlayerByName(testPlayerName))
                .thenReturn(CompletableFuture.completedFuture(testPlayer));
        when(balanceApi.updateBalance(testPlayerUuid, 0L))
                .thenReturn(CompletableFuture.completedFuture(null));

        SetBalanceExecutionResult result = service.execute(testPlayerName, 0L).get();

        assertTrue(result.isSuccess());
        verify(balanceApi).updateBalance(testPlayerUuid, 0L);
    }

    @Test
    @DisplayName("Should set very large balance")
    void shouldSetVeryLargeBalance() throws ExecutionException, InterruptedException {
        long largeBalance = Long.MAX_VALUE / 2;

        when(playerService.getPlayerByName(testPlayerName))
                .thenReturn(CompletableFuture.completedFuture(testPlayer));
        when(balanceApi.updateBalance(testPlayerUuid, largeBalance))
                .thenReturn(CompletableFuture.completedFuture(null));

        SetBalanceExecutionResult result = service.execute(testPlayerName, largeBalance).get();

        assertTrue(result.isSuccess());
        verify(balanceApi).updateBalance(testPlayerUuid, largeBalance);
    }

    @Test
    @DisplayName("Should set balance with special characters in player name")
    void shouldSetBalanceWithSpecialCharacters() throws ExecutionException, InterruptedException {
        String specialName = "Player_123";
        UUID specialUuid = UUID.randomUUID();
        Player specialPlayer = new Player(specialUuid, specialName, Instant.now());

        when(playerService.getPlayerByName(specialName))
                .thenReturn(CompletableFuture.completedFuture(specialPlayer));
        when(balanceApi.updateBalance(specialUuid, 500_0000L))
                .thenReturn(CompletableFuture.completedFuture(null));

        SetBalanceExecutionResult result = service.execute(specialName, 500_0000L).get();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should reject negative amount")
    void shouldRejectNegativeAmount() throws ExecutionException, InterruptedException {
        SetBalanceExecutionResult result = service.execute(testPlayerName, -100L).get();

        assertFalse(result.isSuccess());
        assertEquals(SetBalanceStatus.INVALID_AMOUNT, result.getStatus());

        verify(playerService, never()).getPlayerByName(any());
        verify(balanceApi, never()).updateBalance(any(), anyLong());
    }

    @Test
    @DisplayName("Should reject very negative amount")
    void shouldRejectVeryNegativeAmount() throws ExecutionException, InterruptedException {
        SetBalanceExecutionResult result = service.execute(testPlayerName, Long.MIN_VALUE).get();

        assertEquals(SetBalanceStatus.INVALID_AMOUNT, result.getStatus());
    }

    @Test
    @DisplayName("Should reject -1 amount")
    void shouldRejectMinusOneAmount() throws ExecutionException, InterruptedException {
        SetBalanceExecutionResult result = service.execute(testPlayerName, -1L).get();

        assertEquals(SetBalanceStatus.INVALID_AMOUNT, result.getStatus());
    }

    @Test
    @DisplayName("Should return player not found when player does not exist")
    void shouldReturnPlayerNotFoundWhenPlayerDoesNotExist() throws ExecutionException, InterruptedException {
        when(playerService.getPlayerByName(testPlayerName))
                .thenReturn(CompletableFuture.failedFuture(new NotFoundException("Player not found")));

        SetBalanceExecutionResult result = service.execute(testPlayerName, 1000_0000L).get();

        assertEquals(SetBalanceStatus.PLAYER_NOT_FOUND, result.getStatus());

        verify(playerService).getPlayerByName(testPlayerName);
        verify(balanceApi, never()).updateBalance(any(), anyLong());
    }

    @Test
    @DisplayName("Should handle player not found for non-existent username")
    void shouldHandlePlayerNotFoundForNonExistentUsername() throws ExecutionException, InterruptedException {
        String nonExistentPlayer = "NonExistent";

        when(playerService.getPlayerByName(nonExistentPlayer))
                .thenReturn(CompletableFuture.failedFuture(new NotFoundException()));

        SetBalanceExecutionResult result = service.execute(nonExistentPlayer, 100_0000L).get();

        assertEquals(SetBalanceStatus.PLAYER_NOT_FOUND, result.getStatus());
    }

    @Test
    @DisplayName("Should return update failed when balance API fails")
    void shouldReturnUpdateFailedWhenBalanceApiFails() throws ExecutionException, InterruptedException {
        when(playerService.getPlayerByName(testPlayerName))
                .thenReturn(CompletableFuture.completedFuture(testPlayer));
        when(balanceApi.updateBalance(testPlayerUuid, 1000_0000L))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Database error")));

        SetBalanceExecutionResult result = service.execute(testPlayerName, 1000_0000L).get();

        assertEquals(SetBalanceStatus.UPDATE_FAILED, result.getStatus());

        verify(balanceApi).updateBalance(testPlayerUuid, 1000_0000L);
    }

    @Test
    @DisplayName("Should handle network timeout during balance update")
    void shouldHandleNetworkTimeoutDuringUpdate() throws ExecutionException, InterruptedException {
        when(playerService.getPlayerByName(testPlayerName))
                .thenReturn(CompletableFuture.completedFuture(testPlayer));
        when(balanceApi.updateBalance(testPlayerUuid, 500_0000L))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Connection timeout")));

        SetBalanceExecutionResult result = service.execute(testPlayerName, 500_0000L).get();

        assertEquals(SetBalanceStatus.UPDATE_FAILED, result.getStatus());
    }

    @Test
    @DisplayName("Should handle HTTP 500 error during balance update")
    void shouldHandleHttp500ErrorDuringUpdate() throws ExecutionException, InterruptedException {
        when(playerService.getPlayerByName(testPlayerName))
                .thenReturn(CompletableFuture.completedFuture(testPlayer));
        when(balanceApi.updateBalance(testPlayerUuid, 250_0000L))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("HTTP 500")));

        SetBalanceExecutionResult result = service.execute(testPlayerName, 250_0000L).get();

        assertEquals(SetBalanceStatus.UPDATE_FAILED, result.getStatus());
    }

    @Test
    @DisplayName("Should return exception for unexpected errors")
    void shouldReturnExceptionForUnexpectedErrors() throws ExecutionException, InterruptedException {
        when(playerService.getPlayerByName(testPlayerName))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Unexpected error")));

        SetBalanceExecutionResult result = service.execute(testPlayerName, 1000_0000L).get();

        assertEquals(SetBalanceStatus.EXCEPTION, result.getStatus());
    }

    @Test
    @DisplayName("Should handle null pointer exception")
    void shouldHandleNullPointerException() throws ExecutionException, InterruptedException {
        when(playerService.getPlayerByName(testPlayerName))
                .thenReturn(CompletableFuture.failedFuture(new NullPointerException()));

        SetBalanceExecutionResult result = service.execute(testPlayerName, 100_0000L).get();

        assertEquals(SetBalanceStatus.EXCEPTION, result.getStatus());
    }

    @Test
    @DisplayName("Should create success result correctly")
    void shouldCreateSuccessResultCorrectly() {
        SetBalanceExecutionResult result = SetBalanceExecutionResult.success(10L, UUID.randomUUID());

        assertTrue(result.isSuccess());
        assertEquals(SetBalanceStatus.SUCCESS, result.getStatus());
    }

    @Test
    @DisplayName("Should create invalid amount result correctly")
    void shouldCreateInvalidAmountResultCorrectly() {
        SetBalanceExecutionResult result = SetBalanceExecutionResult.invalidAmount();

        assertFalse(result.isSuccess());
        assertEquals(SetBalanceStatus.INVALID_AMOUNT, result.getStatus());
    }

    @Test
    @DisplayName("Should create player not found result correctly")
    void shouldCreatePlayerNotFoundResultCorrectly() {
        SetBalanceExecutionResult result = SetBalanceExecutionResult.playerNotFound();

        assertFalse(result.isSuccess());
        assertEquals(SetBalanceStatus.PLAYER_NOT_FOUND, result.getStatus());
    }

    @Test
    @DisplayName("Should create update failed result correctly")
    void shouldCreateUpdateFailedResultCorrectly() {
        SetBalanceExecutionResult result = SetBalanceExecutionResult.updateFailed();

        assertFalse(result.isSuccess());
        assertEquals(SetBalanceStatus.UPDATE_FAILED, result.getStatus());
    }

    @Test
    @DisplayName("Should create exception result correctly")
    void shouldCreateExceptionResultCorrectly() {
        SetBalanceExecutionResult result = SetBalanceExecutionResult.exception();

        assertFalse(result.isSuccess());
        assertEquals(SetBalanceStatus.EXCEPTION, result.getStatus());
    }

    @Test
    @DisplayName("Should handle empty player name")
    void shouldHandleEmptyPlayerName() throws ExecutionException, InterruptedException {
        String emptyName = "";

        when(playerService.getPlayerByName(emptyName))
                .thenReturn(CompletableFuture.failedFuture(new NotFoundException()));

        SetBalanceExecutionResult result = service.execute(emptyName, 100_0000L).get();

        assertEquals(SetBalanceStatus.PLAYER_NOT_FOUND, result.getStatus());
    }

    @Test
    @DisplayName("Should handle very long player name")
    void shouldHandleVeryLongPlayerName() throws ExecutionException, InterruptedException {
        String longName = "A".repeat(100);
        UUID longNameUuid = UUID.randomUUID();
        Player longNamePlayer = new Player(longNameUuid, longName, Instant.now());

        when(playerService.getPlayerByName(longName))
                .thenReturn(CompletableFuture.completedFuture(longNamePlayer));
        when(balanceApi.updateBalance(longNameUuid, 100_0000L))
                .thenReturn(CompletableFuture.completedFuture(null));

        SetBalanceExecutionResult result = service.execute(longName, 100_0000L).get();

        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle multiple concurrent set balance operations")
    void shouldHandleMultipleConcurrentOperations() throws ExecutionException, InterruptedException {
        String player1 = "Player1";
        String player2 = "Player2";
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        when(playerService.getPlayerByName(player1))
                .thenReturn(CompletableFuture.completedFuture(new Player(uuid1, player1, Instant.now())));
        when(playerService.getPlayerByName(player2))
                .thenReturn(CompletableFuture.completedFuture(new Player(uuid2, player2, Instant.now())));
        when(balanceApi.updateBalance(uuid1, 1000L))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(balanceApi.updateBalance(uuid2, 2000L))
                .thenReturn(CompletableFuture.completedFuture(null));

        CompletableFuture<SetBalanceExecutionResult> future1 = service.execute(player1, 1000L);
        CompletableFuture<SetBalanceExecutionResult> future2 = service.execute(player2, 2000L);

        assertTrue(future1.get().isSuccess());
        assertTrue(future2.get().isSuccess());
    }

    @Test
    @DisplayName("Should not call balance API when amount is negative")
    void shouldNotCallBalanceApiWhenAmountIsNegative() throws ExecutionException, InterruptedException {
        service.execute(testPlayerName, -500L).get();

        verify(playerService, never()).getPlayerByName(any());
        verify(balanceApi, never()).updateBalance(any(), anyLong());
    }

    @Test
    @DisplayName("Should call balance API only once for successful operation")
    void shouldCallBalanceApiOnlyOnceForSuccessfulOperation() throws ExecutionException, InterruptedException {
        when(playerService.getPlayerByName(testPlayerName))
                .thenReturn(CompletableFuture.completedFuture(testPlayer));
        when(balanceApi.updateBalance(testPlayerUuid, 1000_0000L))
                .thenReturn(CompletableFuture.completedFuture(null));

        service.execute(testPlayerName, 1000_0000L).get();

        verify(playerService, times(1)).getPlayerByName(testPlayerName);
        verify(balanceApi, times(1)).updateBalance(testPlayerUuid, 1000_0000L);
    }
}