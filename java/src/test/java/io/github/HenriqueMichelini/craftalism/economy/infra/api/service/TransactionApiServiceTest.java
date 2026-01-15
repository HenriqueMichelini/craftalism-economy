package io.github.HenriqueMichelini.craftalism.economy.infra.api.service;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.client.HttpClientService;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.TransactionRequestDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.TransactionResponseDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.config.GsonFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.*;

@DisplayName("TransactionApiService Tests")
class TransactionApiServiceTest {

    @Mock
    private HttpClientService httpClient;
    private TransactionApiService service;
    private Gson gson;

    private UUID fromUuid;
    private UUID toUuid;
    private Long testAmount;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        service = new TransactionApiService(httpClient);
        gson = GsonFactory.createGson();

        fromUuid = UUID.randomUUID();
        toUuid = UUID.randomUUID();
        testAmount = 100_0000L;
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    @DisplayName("Should register transaction successfully")
    void shouldRegisterTransactionSuccessfully() throws ExecutionException, InterruptedException {
        Long transactionId = 12345L;
        Instant createdAt = Instant.now();

        TransactionRequestDTO requestDTO = new TransactionRequestDTO(fromUuid, toUuid, testAmount);
        String expectedRequestJson = gson.toJson(requestDTO);

        TransactionResponseDTO responseDTO = new TransactionResponseDTO(
                transactionId, fromUuid, toUuid, testAmount, createdAt
        );
        String responseJson = gson.toJson(responseDTO);

        HttpResponse<String> mockResponse = createMockResponse(201, responseJson);
        when(httpClient.post("/transactions", expectedRequestJson))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        TransactionResponseDTO result = service.register(fromUuid, toUuid, testAmount).get();

        assertNotNull(result);
        assertEquals(transactionId, result.id());
        assertEquals(fromUuid, result.fromUuid());
        assertEquals(toUuid, result.toUuid());
        assertEquals(testAmount, result.amount());
        assertEquals(createdAt, result.createdAt());

        verify(httpClient).post("/transactions", expectedRequestJson);
    }

    @Test
    @DisplayName("Should register transaction with minimum amount")
    void shouldRegisterTransactionWithMinimumAmount() throws ExecutionException, InterruptedException {
        long minAmount = 1L;
        Long transactionId = 1L;
        Instant createdAt = Instant.now();

        TransactionRequestDTO requestDTO = new TransactionRequestDTO(fromUuid, toUuid, minAmount);
        String expectedRequestJson = gson.toJson(requestDTO);

        TransactionResponseDTO responseDTO = new TransactionResponseDTO(
                transactionId, fromUuid, toUuid, minAmount, createdAt
        );
        String responseJson = gson.toJson(responseDTO);

        HttpResponse<String> mockResponse = createMockResponse(201, responseJson);
        when(httpClient.post("/transactions", expectedRequestJson))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        TransactionResponseDTO result = service.register(fromUuid, toUuid, minAmount).get();

        assertEquals(minAmount, result.amount());
    }

    @Test
    @DisplayName("Should register transaction with very large amount")
    void shouldRegisterTransactionWithVeryLargeAmount() throws ExecutionException, InterruptedException {
        long largeAmount = 1_000_000_0000L; // $1,000,000.00
        Long transactionId = 99999L;
        Instant createdAt = Instant.now();

        TransactionRequestDTO requestDTO = new TransactionRequestDTO(fromUuid, toUuid, largeAmount);
        String expectedRequestJson = gson.toJson(requestDTO);

        TransactionResponseDTO responseDTO = new TransactionResponseDTO(
                transactionId, fromUuid, toUuid, largeAmount, createdAt
        );
        String responseJson = gson.toJson(responseDTO);

        HttpResponse<String> mockResponse = createMockResponse(201, responseJson);
        when(httpClient.post("/transactions", expectedRequestJson))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        TransactionResponseDTO result = service.register(fromUuid, toUuid, largeAmount).get();

        assertEquals(largeAmount, result.amount());
    }

    @Test
    @DisplayName("Should register transaction with same sender and receiver")
    void shouldRegisterTransactionWithSameSenderAndReceiver() throws ExecutionException, InterruptedException {
        UUID sameUuid = UUID.randomUUID();
        Long transactionId = 5L;
        Instant createdAt = Instant.now();

        TransactionRequestDTO requestDTO = new TransactionRequestDTO(sameUuid, sameUuid, testAmount);
        String expectedRequestJson = gson.toJson(requestDTO);

        TransactionResponseDTO responseDTO = new TransactionResponseDTO(
                transactionId, sameUuid, sameUuid, testAmount, createdAt
        );
        String responseJson = gson.toJson(responseDTO);

        HttpResponse<String> mockResponse = createMockResponse(201, responseJson);
        when(httpClient.post("/transactions", expectedRequestJson))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        TransactionResponseDTO result = service.register(sameUuid, sameUuid, testAmount).get();

        assertEquals(sameUuid, result.fromUuid());
        assertEquals(sameUuid, result.toUuid());
    }

    @Test
    @DisplayName("Should handle HTTP error on register transaction")
    void shouldHandleHttpErrorOnRegisterTransaction() {
        TransactionRequestDTO requestDTO = new TransactionRequestDTO(fromUuid, toUuid, testAmount);
        String expectedRequestJson = gson.toJson(requestDTO);

        when(httpClient.post("/transactions", expectedRequestJson))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Database Error")));

        CompletableFuture<TransactionResponseDTO> result = service.register(fromUuid, toUuid, testAmount);

        ExecutionException exception = assertThrows(ExecutionException.class, result::get);
        assertEquals("Database Error", exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Should handle malformed JSON response")
    void shouldHandleMalformedJsonResponse() {
        TransactionRequestDTO requestDTO = new TransactionRequestDTO(fromUuid, toUuid, testAmount);
        String expectedRequestJson = gson.toJson(requestDTO);

        HttpResponse<String> mockResponse = createMockResponse(201, "{invalid json}");
        when(httpClient.post("/transactions", expectedRequestJson))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        assertThrows(ExecutionException.class,
                () -> service.register(fromUuid, toUuid, testAmount).get());
    }

    @Test
    @DisplayName("Should serialize transaction request correctly")
    void shouldSerializeTransactionRequestCorrectly() throws ExecutionException, InterruptedException {
        String[] capturedJson = new String[1];

        TransactionResponseDTO responseDTO = new TransactionResponseDTO(
                1L, fromUuid, toUuid, testAmount, Instant.now()
        );
        String responseJson = gson.toJson(responseDTO);

        HttpResponse<String> mockResponse = createMockResponse(201, responseJson);
        when(httpClient.post(eq("/transactions"), anyString()))
                .thenAnswer(invocation -> {
                    capturedJson[0] = invocation.getArgument(1);
                    return CompletableFuture.completedFuture(mockResponse);
                });

        service.register(fromUuid, toUuid, testAmount).get();

        assertNotNull(capturedJson[0]);
        assertTrue(capturedJson[0].contains("\"fromUuid\":\"" + fromUuid + "\""));
        assertTrue(capturedJson[0].contains("\"toUuid\":\"" + toUuid + "\""));
        assertTrue(capturedJson[0].contains("\"amount\":" + testAmount));
    }

    @Test
    @DisplayName("Should deserialize transaction response correctly")
    void shouldDeserializeTransactionResponseCorrectly() throws ExecutionException, InterruptedException {
        Long expectedId = 42L;
        Instant expectedCreatedAt = Instant.parse("2024-01-15T10:30:00Z");

        TransactionRequestDTO requestDTO = new TransactionRequestDTO(fromUuid, toUuid, testAmount);
        String expectedRequestJson = gson.toJson(requestDTO);

        // Manually construct JSON to ensure exact format
        String responseJson = String.format(
                "{\"id\":%d,\"fromUuid\":\"%s\",\"toUuid\":\"%s\",\"amount\":%d,\"createdAt\":\"%s\"}",
                expectedId, fromUuid, toUuid, testAmount, expectedCreatedAt
        );

        HttpResponse<String> mockResponse = createMockResponse(201, responseJson);
        when(httpClient.post("/transactions", expectedRequestJson))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        TransactionResponseDTO result = service.register(fromUuid, toUuid, testAmount).get();

        assertEquals(expectedId, result.id());
        assertEquals(fromUuid, result.fromUuid());
        assertEquals(toUuid, result.toUuid());
        assertEquals(testAmount, result.amount());
        assertEquals(expectedCreatedAt, result.createdAt());
    }

    @Test
    @DisplayName("Should handle multiple concurrent transaction registrations")
    void shouldHandleMultipleConcurrentRegistrations() throws ExecutionException, InterruptedException {
        UUID from1 = UUID.randomUUID();
        UUID to1 = UUID.randomUUID();
        long amount1 = 100L;

        UUID from2 = UUID.randomUUID();
        UUID to2 = UUID.randomUUID();
        long amount2 = 200L;

        TransactionRequestDTO request1 = new TransactionRequestDTO(from1, to1, amount1);
        TransactionRequestDTO request2 = new TransactionRequestDTO(from2, to2, amount2);

        TransactionResponseDTO response1 = new TransactionResponseDTO(1L, from1, to1, amount1, Instant.now());
        TransactionResponseDTO response2 = new TransactionResponseDTO(2L, from2, to2, amount2, Instant.now());

        HttpResponse<String> mockResponse1 = createMockResponse(201, gson.toJson(response1));
        HttpResponse<String> mockResponse2 = createMockResponse(201, gson.toJson(response2));

        when(httpClient.post("/transactions", gson.toJson(request1)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse1));
        when(httpClient.post("/transactions", gson.toJson(request2)))
                .thenReturn(CompletableFuture.completedFuture(mockResponse2));

        CompletableFuture<TransactionResponseDTO> future1 = service.register(from1, to1, amount1);
        CompletableFuture<TransactionResponseDTO> future2 = service.register(from2, to2, amount2);

        assertEquals(1L, future1.get().id());
        assertEquals(2L, future2.get().id());
    }

    @Test
    @DisplayName("Should handle 500 internal server error")
    void shouldHandle500InternalServerError() {
        TransactionRequestDTO requestDTO = new TransactionRequestDTO(fromUuid, toUuid, testAmount);
        String expectedRequestJson = gson.toJson(requestDTO);

        HttpResponse<String> mockResponse = createMockResponse(500, "Internal Server Error");
        when(httpClient.post("/transactions", expectedRequestJson))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        assertThrows(ExecutionException.class,
                () -> service.register(fromUuid, toUuid, testAmount).get());
    }

    @Test
    @DisplayName("Should handle network timeout")
    void shouldHandleNetworkTimeout() {
        TransactionRequestDTO requestDTO = new TransactionRequestDTO(fromUuid, toUuid, testAmount);
        String expectedRequestJson = gson.toJson(requestDTO);

        when(httpClient.post("/transactions", expectedRequestJson))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Connection timeout")));

        CompletableFuture<TransactionResponseDTO> result = service.register(fromUuid, toUuid, testAmount);

        ExecutionException exception = assertThrows(ExecutionException.class, result::get);
        assertTrue(exception.getCause().getMessage().contains("timeout"));
    }

    @Test
    @DisplayName("Should include all required fields in request")
    void shouldIncludeAllRequiredFieldsInRequest() throws ExecutionException, InterruptedException {
        String[] capturedJson = new String[1];

        TransactionResponseDTO responseDTO = new TransactionResponseDTO(
                1L, fromUuid, toUuid, testAmount, Instant.now()
        );

        HttpResponse<String> mockResponse = createMockResponse(201, gson.toJson(responseDTO));
        when(httpClient.post(eq("/transactions"), anyString()))
                .thenAnswer(invocation -> {
                    capturedJson[0] = invocation.getArgument(1);
                    return CompletableFuture.completedFuture(mockResponse);
                });

        service.register(fromUuid, toUuid, testAmount).get();

        String json = capturedJson[0];
        assertNotNull(json);
        assertTrue(json.contains("fromUuid"));
        assertTrue(json.contains("toUuid"));
        assertTrue(json.contains("amount"));
    }

    @Test
    @DisplayName("Should return transaction with correct timestamp")
    void shouldReturnTransactionWithCorrectTimestamp() throws ExecutionException, InterruptedException {
        Instant specificTime = Instant.parse("2024-11-24T10:00:00Z");

        TransactionRequestDTO requestDTO = new TransactionRequestDTO(fromUuid, toUuid, testAmount);
        String expectedRequestJson = gson.toJson(requestDTO);

        TransactionResponseDTO responseDTO = new TransactionResponseDTO(
                1L, fromUuid, toUuid, testAmount, specificTime
        );
        String responseJson = gson.toJson(responseDTO);

        HttpResponse<String> mockResponse = createMockResponse(201, responseJson);
        when(httpClient.post("/transactions", expectedRequestJson))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        TransactionResponseDTO result = service.register(fromUuid, toUuid, testAmount).get();

        assertEquals(specificTime, result.createdAt());
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> createMockResponse(int statusCode, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        return response;
    }
}
