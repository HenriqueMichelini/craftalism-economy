package io.github.HenriqueMichelini.craftalism.economy.application.service;

import io.github.HenriqueMichelini.craftalism.economy.application.service.BaltopCommandApplicationService.BaltopEntry;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.BalanceResponseDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.PlayerResponseDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.BalanceApiService;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.PlayerApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("BaltopCommandApplicationService Tests")
class BaltopCommandApplicationServiceTest {

    private BalanceApiService balanceApi;
    private PlayerApiService playerApi;
    private BaltopCommandApplicationService service;

    @BeforeEach
    void setUp() {
        balanceApi = mock(BalanceApiService.class);
        playerApi = mock(PlayerApiService.class);
        service = new BaltopCommandApplicationService(balanceApi, playerApi);
    }

    // getTop10() tests
    @Test
    @DisplayName("Should get top 10 players successfully")
    void shouldGetTop10PlayersSuccessfully() throws ExecutionException, InterruptedException {
        // Arrange
        List<BalanceResponseDTO> balances = createBalanceList(10);
        when(balanceApi.getTopBalances(10))
                .thenReturn(CompletableFuture.completedFuture(balances));

        // Mock player API responses
        for (BalanceResponseDTO balance : balances) {
            PlayerResponseDTO player = new PlayerResponseDTO(
                    balance.uuid(),
                    "Player" + balance.uuid().toString().substring(0, 8),
                    Instant.now()
            );
            when(playerApi.getPlayerByUuid(balance.uuid()))
                    .thenReturn(CompletableFuture.completedFuture(player));
        }

        // Act
        List<BaltopEntry> result = service.getTop10().get();

        // Assert
        assertNotNull(result);
        assertEquals(10, result.size());
        verify(balanceApi).getTopBalances(10);
    }

    @Test
    @DisplayName("Should get top 10 with correct player names")
    void shouldGetTop10WithCorrectPlayerNames() throws ExecutionException, InterruptedException {
        // Arrange
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();

        List<BalanceResponseDTO> balances = List.of(
                new BalanceResponseDTO(uuid1, 1000_0000L),
                new BalanceResponseDTO(uuid2, 500_0000L),
                new BalanceResponseDTO(uuid3, 250_0000L)
        );

        when(balanceApi.getTopBalances(10))
                .thenReturn(CompletableFuture.completedFuture(balances));

        when(playerApi.getPlayerByUuid(uuid1))
                .thenReturn(CompletableFuture.completedFuture(
                        new PlayerResponseDTO(uuid1, "RichPlayer", Instant.now())));
        when(playerApi.getPlayerByUuid(uuid2))
                .thenReturn(CompletableFuture.completedFuture(
                        new PlayerResponseDTO(uuid2, "MediumPlayer", Instant.now())));
        when(playerApi.getPlayerByUuid(uuid3))
                .thenReturn(CompletableFuture.completedFuture(
                        new PlayerResponseDTO(uuid3, "PoorPlayer", Instant.now())));

        // Act
        List<BaltopEntry> result = service.getTop10().get();

        // Assert
        assertEquals(3, result.size());
        assertEquals("RichPlayer", result.get(0).getPlayerName());
        assertEquals(1000_0000L, result.get(0).getBalance());
        assertEquals("MediumPlayer", result.get(1).getPlayerName());
        assertEquals(500_0000L, result.get(1).getBalance());
        assertEquals("PoorPlayer", result.get(2).getPlayerName());
        assertEquals(250_0000L, result.get(2).getBalance());
    }

    @Test
    @DisplayName("Should handle empty baltop list")
    void shouldHandleEmptyBaltopList() throws ExecutionException, InterruptedException {
        // Arrange
        when(balanceApi.getTopBalances(10))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        // Act
        List<BaltopEntry> result = service.getTop10().get();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle baltop with less than 10 players")
    void shouldHandleBaltopWithLessThan10Players() throws ExecutionException, InterruptedException {
        // Arrange
        List<BalanceResponseDTO> balances = createBalanceList(5);
        when(balanceApi.getTopBalances(10))
                .thenReturn(CompletableFuture.completedFuture(balances));

        for (BalanceResponseDTO balance : balances) {
            PlayerResponseDTO player = new PlayerResponseDTO(
                    balance.uuid(),
                    "Player" + balance.uuid().toString().substring(0, 8),
                    Instant.now()
            );
            when(playerApi.getPlayerByUuid(balance.uuid()))
                    .thenReturn(CompletableFuture.completedFuture(player));
        }

        // Act
        List<BaltopEntry> result = service.getTop10().get();

        // Assert
        assertEquals(5, result.size());
    }

    // getTopPlayers(int limit) tests
    @Test
    @DisplayName("Should get top players with custom limit")
    void shouldGetTopPlayersWithCustomLimit() throws ExecutionException, InterruptedException {
        // Arrange
        int customLimit = 25;
        List<BalanceResponseDTO> balances = createBalanceList(customLimit);
        when(balanceApi.getTopBalances(customLimit))
                .thenReturn(CompletableFuture.completedFuture(balances));

        for (BalanceResponseDTO balance : balances) {
            PlayerResponseDTO player = new PlayerResponseDTO(
                    balance.uuid(),
                    "Player" + balance.uuid().toString().substring(0, 8),
                    Instant.now()
            );
            when(playerApi.getPlayerByUuid(balance.uuid()))
                    .thenReturn(CompletableFuture.completedFuture(player));
        }

        // Act
        List<BaltopEntry> result = service.getTopPlayers(customLimit).get();

        // Assert
        assertEquals(customLimit, result.size());
        verify(balanceApi).getTopBalances(customLimit);
    }

    @Test
    @DisplayName("Should get top 5 players")
    void shouldGetTop5Players() throws ExecutionException, InterruptedException {
        // Arrange
        List<BalanceResponseDTO> balances = createBalanceList(5);
        when(balanceApi.getTopBalances(5))
                .thenReturn(CompletableFuture.completedFuture(balances));

        for (BalanceResponseDTO balance : balances) {
            PlayerResponseDTO player = new PlayerResponseDTO(
                    balance.uuid(),
                    "Player" + balance.uuid().toString().substring(0, 8),
                    Instant.now()
            );
            when(playerApi.getPlayerByUuid(balance.uuid()))
                    .thenReturn(CompletableFuture.completedFuture(player));
        }

        // Act
        List<BaltopEntry> result = service.getTopPlayers(5).get();

        // Assert
        assertEquals(5, result.size());
    }

    @Test
    @DisplayName("Should handle limit of 1")
    void shouldHandleLimitOfOne() throws ExecutionException, InterruptedException {
        // Arrange
        UUID topUuid = UUID.randomUUID();
        List<BalanceResponseDTO> balances = List.of(
                new BalanceResponseDTO(topUuid, 9999_0000L)
        );
        when(balanceApi.getTopBalances(1))
                .thenReturn(CompletableFuture.completedFuture(balances));

        when(playerApi.getPlayerByUuid(topUuid))
                .thenReturn(CompletableFuture.completedFuture(
                        new PlayerResponseDTO(topUuid, "TopPlayer", Instant.now())));

        // Act
        List<BaltopEntry> result = service.getTopPlayers(1).get();

        // Assert
        assertEquals(1, result.size());
        assertEquals("TopPlayer", result.getFirst().getPlayerName());
    }

    // Error handling tests
    @Test
    @DisplayName("Should handle balance API failure")
    void shouldHandleBalanceApiFailure() {
        // Arrange
        when(balanceApi.getTopBalances(10))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API Error")));

        // Act
        CompletableFuture<List<BaltopEntry>> result = service.getTop10();

        // Assert
        ExecutionException exception = assertThrows(ExecutionException.class, result::get);
        assertEquals("API Error", exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Should use Unknown for player when player API fails")
    void shouldUseUnknownForPlayerWhenPlayerApiFails() throws ExecutionException, InterruptedException {
        // Arrange
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        List<BalanceResponseDTO> balances = List.of(
                new BalanceResponseDTO(uuid1, 1000_0000L),
                new BalanceResponseDTO(uuid2, 500_0000L)
        );

        when(balanceApi.getTopBalances(10))
                .thenReturn(CompletableFuture.completedFuture(balances));

        // First player succeeds
        when(playerApi.getPlayerByUuid(uuid1))
                .thenReturn(CompletableFuture.completedFuture(
                        new PlayerResponseDTO(uuid1, "KnownPlayer", Instant.now())));

        // Second player fails
        when(playerApi.getPlayerByUuid(uuid2))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Player not found")));

        // Act
        List<BaltopEntry> result = service.getTop10().get();

        // Assert
        assertEquals(2, result.size());
        assertEquals("KnownPlayer", result.get(0).getPlayerName());
        assertEquals("Unknown", result.get(1).getPlayerName());
        assertEquals(500_0000L, result.get(1).getBalance());
    }

    @Test
    @DisplayName("Should handle all players with Unknown when all player APIs fail")
    void shouldHandleAllPlayersWithUnknownWhenAllFail() throws ExecutionException, InterruptedException {
        // Arrange
        List<BalanceResponseDTO> balances = createBalanceList(3);
        when(balanceApi.getTopBalances(10))
                .thenReturn(CompletableFuture.completedFuture(balances));

        // All player lookups fail
        for (BalanceResponseDTO balance : balances) {
            when(playerApi.getPlayerByUuid(balance.uuid()))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Not found")));
        }

        // Act
        List<BaltopEntry> result = service.getTop10().get();

        // Assert
        assertEquals(3, result.size());
        result.forEach(entry -> {
            assertEquals("Unknown", entry.getPlayerName());
            assertNotNull(entry.getUuid());
            assertTrue(entry.getBalance() > 0);
        });
    }

    @Test
    @DisplayName("Should preserve balance amounts when player lookup fails")
    void shouldPreserveBalanceAmountsWhenPlayerLookupFails() throws ExecutionException, InterruptedException {
        // Arrange
        UUID uuid = UUID.randomUUID();
        long specificBalance = 12345_6789L;

        List<BalanceResponseDTO> balances = List.of(
                new BalanceResponseDTO(uuid, specificBalance)
        );

        when(balanceApi.getTopBalances(10))
                .thenReturn(CompletableFuture.completedFuture(balances));

        when(playerApi.getPlayerByUuid(uuid))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Not found")));

        // Act
        List<BaltopEntry> result = service.getTop10().get();

        // Assert
        assertEquals(1, result.size());
        assertEquals("Unknown", result.getFirst().getPlayerName());
        assertEquals(specificBalance, result.getFirst().getBalance());
        assertEquals(uuid, result.getFirst().getUuid());
    }

    // BaltopEntry tests
    @Test
    @DisplayName("Should create BaltopEntry correctly")
    void shouldCreateBaltopEntryCorrectly() {
        // Arrange
        UUID uuid = UUID.randomUUID();
        String name = "TestPlayer";
        long balance = 1000_0000L;

        // Act
        BaltopEntry entry = new BaltopEntry(name, balance, uuid);

        // Assert
        assertEquals(name, entry.getPlayerName());
        assertEquals(balance, entry.getBalance());
        assertEquals(uuid, entry.getUuid());
    }

    @Test
    @DisplayName("Should handle BaltopEntry with zero balance")
    void shouldHandleBaltopEntryWithZeroBalance() {
        // Arrange
        UUID uuid = UUID.randomUUID();

        // Act
        BaltopEntry entry = new BaltopEntry("Player", 0L, uuid);

        // Assert
        assertEquals(0L, entry.getBalance());
    }

    @Test
    @DisplayName("Should handle BaltopEntry with very large balance")
    void shouldHandleBaltopEntryWithVeryLargeBalance() {
        // Arrange
        UUID uuid = UUID.randomUUID();
        long largeBalance = Long.MAX_VALUE / 2;

        // Act
        BaltopEntry entry = new BaltopEntry("RichPlayer", largeBalance, uuid);

        // Assert
        assertEquals(largeBalance, entry.getBalance());
    }

    @Test
    @DisplayName("Should handle BaltopEntry with empty name")
    void shouldHandleBaltopEntryWithEmptyName() {
        // Arrange
        UUID uuid = UUID.randomUUID();

        // Act
        BaltopEntry entry = new BaltopEntry("", 1000L, uuid);

        // Assert
        assertEquals("", entry.getPlayerName());
    }

    // Performance and concurrency tests
    @Test
    @DisplayName("Should handle concurrent player API calls")
    void shouldHandleConcurrentPlayerApiCalls() throws ExecutionException, InterruptedException {
        // Arrange
        List<BalanceResponseDTO> balances = createBalanceList(10);
        when(balanceApi.getTopBalances(10))
                .thenReturn(CompletableFuture.completedFuture(balances));

        // All player API calls succeed concurrently
        for (BalanceResponseDTO balance : balances) {
            PlayerResponseDTO player = new PlayerResponseDTO(
                    balance.uuid(),
                    "Player" + balance.uuid().toString().substring(0, 8),
                    Instant.now()
            );
            when(playerApi.getPlayerByUuid(balance.uuid()))
                    .thenReturn(CompletableFuture.completedFuture(player));
        }

        // Act
        List<BaltopEntry> result = service.getTop10().get();

        // Assert
        assertEquals(10, result.size());
        // Verify all player APIs were called
        for (BalanceResponseDTO balance : balances) {
            verify(playerApi).getPlayerByUuid(balance.uuid());
        }
    }

    @Test
    @DisplayName("Should maintain order of balances from API")
    void shouldMaintainOrderOfBalancesFromApi() throws ExecutionException, InterruptedException {
        // Arrange - Create balances in descending order
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();

        List<BalanceResponseDTO> balances = List.of(
                new BalanceResponseDTO(uuid1, 3000_0000L), // Highest
                new BalanceResponseDTO(uuid2, 2000_0000L), // Middle
                new BalanceResponseDTO(uuid3, 1000_0000L)  // Lowest
        );

        when(balanceApi.getTopBalances(10))
                .thenReturn(CompletableFuture.completedFuture(balances));

        when(playerApi.getPlayerByUuid(uuid1))
                .thenReturn(CompletableFuture.completedFuture(
                        new PlayerResponseDTO(uuid1, "First", Instant.now())));
        when(playerApi.getPlayerByUuid(uuid2))
                .thenReturn(CompletableFuture.completedFuture(
                        new PlayerResponseDTO(uuid2, "Second", Instant.now())));
        when(playerApi.getPlayerByUuid(uuid3))
                .thenReturn(CompletableFuture.completedFuture(
                        new PlayerResponseDTO(uuid3, "Third", Instant.now())));

        // Act
        List<BaltopEntry> result = service.getTop10().get();

        // Assert - Order should be maintained
        assertEquals(3, result.size());
        assertEquals("First", result.get(0).getPlayerName());
        assertEquals(3000_0000L, result.get(0).getBalance());
        assertEquals("Second", result.get(1).getPlayerName());
        assertEquals(2000_0000L, result.get(1).getBalance());
        assertEquals("Third", result.get(2).getPlayerName());
        assertEquals(1000_0000L, result.get(2).getBalance());
    }

    // Helper method to create balance list
    private List<BalanceResponseDTO> createBalanceList(int count) {
        List<BalanceResponseDTO> balances = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            UUID uuid = UUID.randomUUID();
            long balance = (count - i) * 1000_0000L; // Descending balances
            balances.add(new BalanceResponseDTO(uuid, balance));
        }
        return balances;
    }
}
