package io.github.HenriqueMichelini.craftalism.economy.application.service;

import io.github.HenriqueMichelini.craftalism.economy.domain.model.Player;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.PlayerResponseDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.NotFoundException;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.repository.PlayerCacheRepository;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.PlayerApiService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PlayerApplicationService Tests")
class PlayerApplicationServiceTest {

    @Mock
    private PlayerApiService playerApiService;
    @Mock
    private PlayerCacheRepository cacheRepository;
    private PlayerApplicationService service;

    private UUID testUuid;
    private String testName;
    private Instant testCreatedAt;
    private PlayerResponseDTO testPlayerDTO;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        service = new PlayerApplicationService(playerApiService, cacheRepository);

        testUuid = UUID.randomUUID();
        testName = "TestPlayer";
        testCreatedAt = Instant.now();
        testPlayerDTO = new PlayerResponseDTO(testUuid, testName, testCreatedAt);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    @DisplayName("Should load player on join and cache it")
    void shouldLoadPlayerOnJoinAndCache() throws ExecutionException, InterruptedException {
        when(playerApiService.getOrCreatePlayer(testUuid, testName))
                .thenReturn(CompletableFuture.completedFuture(testPlayerDTO));

        CompletableFuture<Player> result = service.loadPlayerOnJoin(testUuid, testName);
        Player player = result.get();

        assertNotNull(player);
        assertEquals(testUuid, player.getUuid());
        assertEquals(testName, player.getName());
        assertEquals(testCreatedAt, player.getCreatedAt());

        verify(playerApiService).getOrCreatePlayer(testUuid, testName);
        verify(cacheRepository).save(argThat(p ->
                p.getUuid().equals(testUuid) &&
                        p.getName().equals(testName)
        ));
    }

    @Test
    @DisplayName("Should propagate API failure on load player join")
    void shouldPropagateApiFailureOnLoadPlayerJoin() {
        RuntimeException apiError = new RuntimeException("API Error");
        when(playerApiService.getOrCreatePlayer(testUuid, testName))
                .thenReturn(CompletableFuture.failedFuture(apiError));

        CompletableFuture<Player> result = service.loadPlayerOnJoin(testUuid, testName);

        ExecutionException exception = assertThrows(ExecutionException.class, result::get);
        assertEquals("API Error", exception.getCause().getMessage());
        verify(cacheRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle new player creation on join")
    void shouldHandleNewPlayerCreationOnJoin() throws ExecutionException, InterruptedException {
        UUID newPlayerUuid = UUID.randomUUID();
        String newPlayerName = "NewPlayer";
        Instant now = Instant.now();
        PlayerResponseDTO newPlayerDTO = new PlayerResponseDTO(newPlayerUuid, newPlayerName, now);

        when(playerApiService.getOrCreatePlayer(newPlayerUuid, newPlayerName))
                .thenReturn(CompletableFuture.completedFuture(newPlayerDTO));

        Player player = service.loadPlayerOnJoin(newPlayerUuid, newPlayerName).get();

        assertEquals(newPlayerUuid, player.getUuid());
        assertEquals(newPlayerName, player.getName());
        assertEquals(now, player.getCreatedAt());
        verify(cacheRepository).save(any(Player.class));
    }

    @Test
    @DisplayName("Should sync player from API and update cache")
    void shouldSyncPlayerFromApiAndUpdateCache() throws ExecutionException, InterruptedException {
        String updatedName = "UpdatedName";
        PlayerResponseDTO updatedDTO = new PlayerResponseDTO(testUuid, updatedName, testCreatedAt);

        when(playerApiService.getPlayerByUuid(testUuid))
                .thenReturn(CompletableFuture.completedFuture(updatedDTO));

        Player player = service.syncPlayer(testUuid).get();

        assertNotNull(player);
        assertEquals(testUuid, player.getUuid());
        assertEquals(updatedName, player.getName());
        assertEquals(testCreatedAt, player.getCreatedAt());

        verify(playerApiService).getPlayerByUuid(testUuid);
        verify(cacheRepository).save(argThat(p ->
                p.getUuid().equals(testUuid) &&
                        p.getName().equals(updatedName)
        ));
    }

    @Test
    @DisplayName("Should propagate API failure on sync player")
    void shouldPropagateApiFailureOnSyncPlayer() {
        when(playerApiService.getPlayerByUuid(testUuid))
                .thenReturn(CompletableFuture.failedFuture(new NotFoundException("Player not found")));

        CompletableFuture<Player> result = service.syncPlayer(testUuid);

        ExecutionException exception = assertThrows(ExecutionException.class, result::get);
        assertInstanceOf(NotFoundException.class, exception.getCause());
        verify(cacheRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should return existing player when found")
    void shouldReturnExistingPlayerWhenFound() throws ExecutionException, InterruptedException {
        when(playerApiService.getPlayerByUuid(testUuid))
                .thenReturn(CompletableFuture.completedFuture(testPlayerDTO));

        PlayerResponseDTO result = service.getOrCreatePlayer(testUuid, testName).get();

        assertNotNull(result);
        assertEquals(testUuid, result.uuid());
        assertEquals(testName, result.name());

        verify(playerApiService).getPlayerByUuid(testUuid);
        verify(playerApiService, never()).createPlayer(any(), any());
    }

    @Test
    @DisplayName("Should create player when not found")
    void shouldCreatePlayerWhenNotFound() throws ExecutionException, InterruptedException {
        when(playerApiService.getPlayerByUuid(testUuid))
                .thenReturn(CompletableFuture.failedFuture(new NotFoundException("Not found")));
        when(playerApiService.createPlayer(testUuid, testName))
                .thenReturn(CompletableFuture.completedFuture(testPlayerDTO));

        PlayerResponseDTO result = service.getOrCreatePlayer(testUuid, testName).get();

        assertNotNull(result);
        assertEquals(testUuid, result.uuid());
        assertEquals(testName, result.name());

        verify(playerApiService).getPlayerByUuid(testUuid);
        verify(playerApiService).createPlayer(testUuid, testName);
    }

    @Test
    @DisplayName("Should propagate non-NotFoundException exceptions")
    void shouldPropagateNonNotFoundExceptions() {
        RuntimeException otherException = new RuntimeException("Other error");
        when(playerApiService.getPlayerByUuid(testUuid))
                .thenReturn(CompletableFuture.failedFuture(otherException));

        CompletableFuture<PlayerResponseDTO> result = service.getOrCreatePlayer(testUuid, testName);

        ExecutionException exception = assertThrows(ExecutionException.class, result::get);
        assertEquals("Other error", exception.getCause().getMessage());
        verify(playerApiService, never()).createPlayer(any(), any());
    }

    @Test
    @DisplayName("Should return cached player when available")
    void shouldReturnCachedPlayerWhenAvailable() throws ExecutionException, InterruptedException {
        Player cachedPlayer = new Player(testUuid, testName, testCreatedAt);
        when(cacheRepository.find(testUuid)).thenReturn(Optional.of(cachedPlayer));

        Player result = service.getCachedOrFetch(testUuid, testName).get();

        assertNotNull(result);
        assertEquals(testUuid, result.getUuid());
        assertEquals(testName, result.getName());

        verify(cacheRepository).find(testUuid);
        verify(playerApiService, never()).getOrCreatePlayer(any(), any());
    }

    @Test
    @DisplayName("Should fetch and cache player when not in cache")
    void shouldFetchAndCachePlayerWhenNotInCache() throws ExecutionException, InterruptedException {
        when(cacheRepository.find(testUuid)).thenReturn(Optional.empty());
        when(playerApiService.getOrCreatePlayer(testUuid, testName))
                .thenReturn(CompletableFuture.completedFuture(testPlayerDTO));

        Player result = service.getCachedOrFetch(testUuid, testName).get();

        assertNotNull(result);
        assertEquals(testUuid, result.getUuid());
        assertEquals(testName, result.getName());
        assertEquals(testCreatedAt, result.getCreatedAt());

        verify(cacheRepository).find(testUuid);
        verify(playerApiService).getOrCreatePlayer(testUuid, testName);
        verify(cacheRepository).save(argThat(p ->
                p.getUuid().equals(testUuid) &&
                        p.getName().equals(testName)
        ));
    }

    @Test
    @DisplayName("Should propagate API failure when fetching uncached player")
    void shouldPropagateApiFailureWhenFetchingUncachedPlayer() {
        when(cacheRepository.find(testUuid)).thenReturn(Optional.empty());
        when(playerApiService.getOrCreatePlayer(testUuid, testName))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API Error")));

        CompletableFuture<Player> result = service.getCachedOrFetch(testUuid, testName);

        ExecutionException exception = assertThrows(ExecutionException.class, result::get);
        assertEquals("API Error", exception.getCause().getMessage());
        verify(cacheRepository, times(1)).find(testUuid);
        verify(cacheRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should handle cache miss and successful API fetch")
    void shouldHandleCacheMissAndSuccessfulApiFetch() throws ExecutionException, InterruptedException {
        when(cacheRepository.find(testUuid)).thenReturn(Optional.empty());
        when(playerApiService.getOrCreatePlayer(testUuid, testName))
                .thenReturn(CompletableFuture.completedFuture(testPlayerDTO));

        Player result = service.getCachedOrFetch(testUuid, testName).get();

        assertEquals(testUuid, result.getUuid());
        assertEquals(testName, result.getName());

        verify(cacheRepository).find(testUuid);
        verify(playerApiService).getOrCreatePlayer(testUuid, testName);
        verify(cacheRepository).save(any(Player.class));
    }

    @Test
    @DisplayName("Should handle multiple concurrent cache lookups")
    void shouldHandleMultipleConcurrentCacheLookups() throws ExecutionException, InterruptedException {
        Player cachedPlayer = new Player(testUuid, testName, testCreatedAt);
        when(cacheRepository.find(testUuid)).thenReturn(Optional.of(cachedPlayer));

        CompletableFuture<Player> result1 = service.getCachedOrFetch(testUuid, testName);
        CompletableFuture<Player> result2 = service.getCachedOrFetch(testUuid, testName);
        CompletableFuture<Player> result3 = service.getCachedOrFetch(testUuid, testName);

        assertNotNull(result1.get());
        assertNotNull(result2.get());
        assertNotNull(result3.get());

        verify(cacheRepository, times(3)).find(testUuid);
        verify(playerApiService, never()).getOrCreatePlayer(any(), any());
    }

    @Test
    @DisplayName("Should handle empty string player name")
    void shouldHandleEmptyStringPlayerName() throws ExecutionException, InterruptedException {
        String emptyName = "";
        PlayerResponseDTO emptyNameDTO = new PlayerResponseDTO(testUuid, emptyName, testCreatedAt);

        when(playerApiService.getOrCreatePlayer(testUuid, emptyName))
                .thenReturn(CompletableFuture.completedFuture(emptyNameDTO));

        Player result = service.loadPlayerOnJoin(testUuid, emptyName).get();

        assertEquals(emptyName, result.getName());
        verify(cacheRepository).save(any(Player.class));
    }

    @Test
    @DisplayName("Should handle very long player names")
    void shouldHandleVeryLongPlayerNames() throws ExecutionException, InterruptedException {
        String longName = "A".repeat(100);
        PlayerResponseDTO longNameDTO = new PlayerResponseDTO(testUuid, longName, testCreatedAt);

        when(playerApiService.getOrCreatePlayer(testUuid, longName))
                .thenReturn(CompletableFuture.completedFuture(longNameDTO));

        Player result = service.loadPlayerOnJoin(testUuid, longName).get();

        assertEquals(longName, result.getName());
    }
}
