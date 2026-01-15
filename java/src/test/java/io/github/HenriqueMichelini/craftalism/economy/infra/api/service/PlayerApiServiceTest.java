package io.github.HenriqueMichelini.craftalism.economy.infra.api.service;

import com.google.gson.Gson;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.client.HttpClientService;
import io.github.HenriqueMichelini.craftalism.economy.infra.config.GsonFactory;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.*;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.NotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PlayerApiService Tests")
class PlayerApiServiceTest {

    @Mock
    private HttpClientService httpClient;
    private PlayerApiService service;
    private Gson gson;

    private UUID testUuid;
    private String testName;
    private Instant testCreatedAt;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        gson = GsonFactory.createGson();

        service = new PlayerApiService(httpClient, gson);

        testUuid = UUID.randomUUID();
        testName = "TestPlayer";
        testCreatedAt = Instant.now();
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    @DisplayName("Should get player by UUID successfully")
    void shouldGetPlayerByUuidSuccessfully() throws ExecutionException, InterruptedException {
        PlayerResponseDTO expectedDTO = new PlayerResponseDTO(testUuid, testName, testCreatedAt);
        String jsonResponse = gson.toJson(expectedDTO);

        HttpResponse<String> mockResponse = createMockResponse(200, jsonResponse);
        when(httpClient.get("/players/" + testUuid))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        PlayerResponseDTO result = service.getPlayerByUuid(testUuid).get();

        assertNotNull(result);
        assertEquals(testUuid, result.uuid());
        assertEquals(testName, result.name());
        assertEquals(testCreatedAt, result.createdAt());

        verify(httpClient).get("/players/" + testUuid);
    }

    @Test
    @DisplayName("Should throw NotFoundException when player not found by UUID")
    void shouldThrowNotFoundExceptionWhenPlayerNotFoundByUuid() {
        HttpResponse<String> mockResponse = createMockResponse(404, "{}");
        when(httpClient.get("/players/" + testUuid))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        CompletableFuture<PlayerResponseDTO> result = service.getPlayerByUuid(testUuid);

        ExecutionException exception = assertThrows(ExecutionException.class, result::get);
        assertInstanceOf(NotFoundException.class, exception.getCause());
    }

    @Test
    @DisplayName("Should handle HTTP error on get player by UUID")
    void shouldHandleHttpErrorOnGetPlayerByUuid() {
        when(httpClient.get("/players/" + testUuid))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Network Error")));

        CompletableFuture<PlayerResponseDTO> result = service.getPlayerByUuid(testUuid);

        ExecutionException exception = assertThrows(ExecutionException.class, result::get);
        assertEquals("Network Error", exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Should handle malformed JSON on get player by UUID")
    void shouldHandleMalformedJsonOnGetPlayerByUuid() {
        HttpResponse<String> mockResponse = createMockResponse(200, "{invalid json}");
        when(httpClient.get("/players/" + testUuid))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        assertThrows(ExecutionException.class,
                () -> service.getPlayerByUuid(testUuid).get());
    }

    @Test
    @DisplayName("Should get player by name successfully")
    void shouldGetPlayerByNameSuccessfully() throws ExecutionException, InterruptedException {
        PlayerResponseDTO expectedDTO = new PlayerResponseDTO(testUuid, testName, testCreatedAt);
        String jsonResponse = gson.toJson(expectedDTO);

        HttpResponse<String> mockResponse = createMockResponse(200, jsonResponse);
        when(httpClient.get("/players/" + testName))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        PlayerResponseDTO result = service.getPlayerByName(testName).get();

        assertNotNull(result);
        assertEquals(testUuid, result.uuid());
        assertEquals(testName, result.name());
        assertEquals(testCreatedAt, result.createdAt());

        verify(httpClient).get("/players/" + testName);
    }

    @Test
    @DisplayName("Should throw NotFoundException when player not found by name")
    void shouldThrowNotFoundExceptionWhenPlayerNotFoundByName() {
        HttpResponse<String> mockResponse = createMockResponse(404, "{}");
        when(httpClient.get("/players/" + testName))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        CompletableFuture<PlayerResponseDTO> result = service.getPlayerByName(testName);

        ExecutionException exception = assertThrows(ExecutionException.class, result::get);
        assertInstanceOf(NotFoundException.class, exception.getCause());
    }

    @Test
    @DisplayName("Should handle special characters in player name")
    void shouldHandleSpecialCharactersInPlayerName() throws ExecutionException, InterruptedException {
        String specialName = "Player_123-XYZ";
        PlayerResponseDTO expectedDTO = new PlayerResponseDTO(testUuid, specialName, testCreatedAt);
        String jsonResponse = gson.toJson(expectedDTO);

        HttpResponse<String> mockResponse = createMockResponse(200, jsonResponse);
        when(httpClient.get("/players/" + specialName))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        PlayerResponseDTO result = service.getPlayerByName(specialName).get();

        assertEquals(specialName, result.name());
    }

    @Test
    @DisplayName("Should handle long player names")
    void shouldHandleLongPlayerNames() throws ExecutionException, InterruptedException {
        String longName = "A".repeat(50);
        PlayerResponseDTO expectedDTO = new PlayerResponseDTO(testUuid, longName, testCreatedAt);
        String jsonResponse = gson.toJson(expectedDTO);

        HttpResponse<String> mockResponse = createMockResponse(200, jsonResponse);
        when(httpClient.get("/players/" + longName))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        PlayerResponseDTO result = service.getPlayerByName(longName).get();

        assertEquals(longName, result.name());
    }

    @Test
    @DisplayName("Should create player successfully")
    void shouldCreatePlayerSuccessfully() throws ExecutionException, InterruptedException {
        PlayerRequestDTO requestDTO = new PlayerRequestDTO(testUuid, testName);
        String expectedRequestJson = gson.toJson(requestDTO);

        PlayerResponseDTO responseDTO = new PlayerResponseDTO(testUuid, testName, testCreatedAt);
        String responseJson = gson.toJson(responseDTO);

        HttpResponse<String> mockResponse = createMockResponse(201, responseJson);
        when(httpClient.post("/players", expectedRequestJson))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        PlayerResponseDTO result = service.createPlayer(testUuid, testName).get();

        assertNotNull(result);
        assertEquals(testUuid, result.uuid());
        assertEquals(testName, result.name());
        assertNotNull(result.createdAt());

        verify(httpClient).post("/players", expectedRequestJson);
    }

    @Test
    @DisplayName("Should create player with empty name")
    void shouldCreatePlayerWithEmptyName() throws ExecutionException, InterruptedException {
        String emptyName = "";
        PlayerRequestDTO requestDTO = new PlayerRequestDTO(testUuid, emptyName);
        String expectedRequestJson = gson.toJson(requestDTO);

        PlayerResponseDTO responseDTO = new PlayerResponseDTO(testUuid, emptyName, testCreatedAt);
        String responseJson = gson.toJson(responseDTO);

        HttpResponse<String> mockResponse = createMockResponse(201, responseJson);
        when(httpClient.post("/players", expectedRequestJson))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        PlayerResponseDTO result = service.createPlayer(testUuid, emptyName).get();

        assertEquals(emptyName, result.name());
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> createMockResponse(int statusCode, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        return response;
    }
}