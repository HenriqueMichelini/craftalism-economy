package io.github.HenriqueMichelini.craftalism.economy.infra.api.service;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.client.HttpClientService;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.BalanceResponseDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.BalanceUpdateRequestDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.config.GsonFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.*;

@DisplayName("BalanceApiService Tests")
class BalanceApiServiceTest {

    @Mock
    private HttpClientService httpClient;
    private BalanceApiService service;
    private Gson gson;

    private UUID testUuid;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        service = new BalanceApiService(httpClient);
        gson = GsonFactory.createGson();

        testUuid = UUID.randomUUID();
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    @DisplayName("Should get balance successfully")
    void shouldGetBalanceSuccessfully() throws ExecutionException, InterruptedException {
        long expectedBalance = 1000_0000L;
        BalanceResponseDTO responseDTO = new BalanceResponseDTO(testUuid, expectedBalance);
        String jsonResponse = gson.toJson(responseDTO);

        HttpResponse<String> mockResponse = createMockResponse(jsonResponse);
        when(httpClient.get("/balances/" + testUuid))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        Long balance = service.getBalance(testUuid).get().amount();

        assertEquals(expectedBalance, balance);
        verify(httpClient).get("/balances/" + testUuid);
    }

    @Test
    @DisplayName("Should get zero balance")
    void shouldGetZeroBalance() throws ExecutionException, InterruptedException {
        BalanceResponseDTO responseDTO = new BalanceResponseDTO(testUuid, 0L);
        String jsonResponse = gson.toJson(responseDTO);

        HttpResponse<String> mockResponse = createMockResponse(jsonResponse);
        when(httpClient.get("/balances/" + testUuid))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        Long balance = service.getBalance(testUuid).get().amount();

        assertEquals(0L, balance);
    }

    @Test
    @DisplayName("Should get very large balance")
    void shouldGetVeryLargeBalance() throws ExecutionException, InterruptedException {
        long largeBalance = Long.MAX_VALUE / 2;
        BalanceResponseDTO responseDTO = new BalanceResponseDTO(testUuid, largeBalance);
        String jsonResponse = gson.toJson(responseDTO);

        HttpResponse<String> mockResponse = createMockResponse(jsonResponse);
        when(httpClient.get("/balances/" + testUuid))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        Long balance = service.getBalance(testUuid).get().amount();

        assertEquals(largeBalance, balance);
    }

    @Test
    @DisplayName("Should handle HTTP error on get balance")
    void shouldHandleHttpErrorOnGetBalance() {
        when(httpClient.get("/balances/" + testUuid))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("HTTP Error")));

        CompletableFuture<Long> result = service.getBalance(testUuid).thenApply(BalanceResponseDTO::amount);

        ExecutionException exception = assertThrows(ExecutionException.class, result::get);
        assertEquals("HTTP Error", exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Should handle malformed JSON on get balance")
    void shouldHandleMalformedJsonOnGetBalance() {
        HttpResponse<String> mockResponse = createMockResponse("{invalid json}");
        when(httpClient.get("/balances/" + testUuid))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        assertThrows(ExecutionException.class,
                () -> service.getBalance(testUuid).get());
    }

    @Test
    @DisplayName("Should deposit amount successfully")
    void shouldDepositAmountSuccessfully() throws ExecutionException, InterruptedException {
        long depositAmount = 500_0000L;
        BalanceUpdateRequestDTO requestDTO = new BalanceUpdateRequestDTO(depositAmount);
        String expectedJson = gson.toJson(requestDTO);

        HttpResponse<String> mockResponse = createMockResponse("{}");
        when(httpClient.post("/balances/" + testUuid + "/deposit", expectedJson))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        Void result = service.deposit(testUuid, depositAmount).get();

        assertNull(result);
        verify(httpClient).post("/balances/" + testUuid + "/deposit", expectedJson);
    }

    @Test
    @DisplayName("Should deposit minimum amount")
    void shouldDepositMinimumAmount() throws ExecutionException, InterruptedException {
        long minAmount = 1L;
        BalanceUpdateRequestDTO requestDTO = new BalanceUpdateRequestDTO(minAmount);
        String expectedJson = gson.toJson(requestDTO);

        HttpResponse<String> mockResponse = createMockResponse("{}");
        when(httpClient.post("/balances/" + testUuid + "/deposit", expectedJson))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        Void result = service.deposit(testUuid, minAmount).get();

        assertNull(result);
    }

    @Test
    @DisplayName("Should deposit very large amount")
    void shouldDepositVeryLargeAmount() throws ExecutionException, InterruptedException {
        long largeAmount = 1_000_000_0000L;
        BalanceUpdateRequestDTO requestDTO = new BalanceUpdateRequestDTO(largeAmount);
        String expectedJson = gson.toJson(requestDTO);

        HttpResponse<String> mockResponse = createMockResponse("{}");
        when(httpClient.post("/balances/" + testUuid + "/deposit", expectedJson))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        Void result = service.deposit(testUuid, largeAmount).get();

        assertNull(result);
    }

    @Test
    @DisplayName("Should handle HTTP error on deposit")
    void shouldHandleHttpErrorOnDeposit() {
        long depositAmount = 500_0000L;
        BalanceUpdateRequestDTO requestDTO = new BalanceUpdateRequestDTO(depositAmount);
        String expectedJson = gson.toJson(requestDTO);

        when(httpClient.post("/balances/" + testUuid + "/deposit", expectedJson))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Network Error")));

        CompletableFuture<Void> result = service.deposit(testUuid, depositAmount);

        ExecutionException exception = assertThrows(ExecutionException.class, result::get);
        assertEquals("Network Error", exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Should withdraw amount successfully")
    void shouldWithdrawAmountSuccessfully() throws ExecutionException, InterruptedException {
        long withdrawAmount = 300_0000L;
        BalanceUpdateRequestDTO requestDTO = new BalanceUpdateRequestDTO(withdrawAmount);
        String expectedJson = gson.toJson(requestDTO);

        HttpResponse<String> mockResponse = createMockResponse("{}");
        when(httpClient.post("/balances/" + testUuid + "/withdraw", expectedJson))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        Void result = service.withdraw(testUuid, withdrawAmount).get();

        assertNull(result);
        verify(httpClient).post("/balances/" + testUuid + "/withdraw", expectedJson);
    }

    @Test
    @DisplayName("Should withdraw minimum amount")
    void shouldWithdrawMinimumAmount() throws ExecutionException, InterruptedException {
        long minAmount = 1L;
        BalanceUpdateRequestDTO requestDTO = new BalanceUpdateRequestDTO(minAmount);
        String expectedJson = gson.toJson(requestDTO);

        HttpResponse<String> mockResponse = createMockResponse("{}");
        when(httpClient.post("/balances/" + testUuid + "/withdraw", expectedJson))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        Void result = service.withdraw(testUuid, minAmount).get();

        assertNull(result);
    }

    @Test
    @DisplayName("Should handle HTTP error on withdraw")
    void shouldHandleHttpErrorOnWithdraw() {
        long withdrawAmount = 300_0000L;
        BalanceUpdateRequestDTO requestDTO = new BalanceUpdateRequestDTO(withdrawAmount);
        String expectedJson = gson.toJson(requestDTO);

        when(httpClient.post("/balances/" + testUuid + "/withdraw", expectedJson))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Insufficient funds")));

        CompletableFuture<Void> result = service.withdraw(testUuid, withdrawAmount);

        ExecutionException exception = assertThrows(ExecutionException.class, result::get);
        assertEquals("Insufficient funds", exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Should handle multiple concurrent balance operations")
    void shouldHandleMultipleConcurrentOperations() throws ExecutionException, InterruptedException {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();

        BalanceResponseDTO response1 = new BalanceResponseDTO(uuid1, 1000L);
        BalanceResponseDTO response2 = new BalanceResponseDTO(uuid2, 2000L);

        HttpResponse<String> mockResponse1 = createMockResponse(gson.toJson(response1));
        HttpResponse<String> mockResponse2 = createMockResponse(gson.toJson(response2));

        when(httpClient.get("/balances/" + uuid1))
                .thenReturn(CompletableFuture.completedFuture(mockResponse1));
        when(httpClient.get("/balances/" + uuid2))
                .thenReturn(CompletableFuture.completedFuture(mockResponse2));

        CompletableFuture<Long> future1 = service.getBalance(uuid1).thenApply(BalanceResponseDTO::amount);
        CompletableFuture<Long> future2 = service.getBalance(uuid2).thenApply(BalanceResponseDTO::amount);

        assertEquals(1000L, future1.get());
        assertEquals(2000L, future2.get());
    }

    @Test
    @DisplayName("Should serialize deposit request correctly")
    void shouldSerializeDepositRequestCorrectly() throws ExecutionException, InterruptedException {
        long amount = 12345L;
        String[] capturedJson = new String[1];

        HttpResponse<String> mockResponse = createMockResponse("{}");
        when(httpClient.post(eq("/balances/" + testUuid + "/deposit"), anyString()))
                .thenAnswer(invocation -> {
                    capturedJson[0] = invocation.getArgument(1);
                    return CompletableFuture.completedFuture(mockResponse);
                });

        service.deposit(testUuid, amount).get();

        assertNotNull(capturedJson[0]);
        assertTrue(capturedJson[0].contains("\"amount\":12345"));
    }

    @Test
    @DisplayName("Should serialize withdraw request correctly")
    void shouldSerializeWithdrawRequestCorrectly() throws ExecutionException, InterruptedException {
        long amount = 54321L;
        String[] capturedJson = new String[1];

        HttpResponse<String> mockResponse = createMockResponse("{}");
        when(httpClient.post(eq("/balances/" + testUuid + "/withdraw"), anyString()))
                .thenAnswer(invocation -> {
                    capturedJson[0] = invocation.getArgument(1);
                    return CompletableFuture.completedFuture(mockResponse);
                });

        service.withdraw(testUuid, amount).get();

        assertNotNull(capturedJson[0]);
        assertTrue(capturedJson[0].contains("\"amount\":54321"));
    }

    @Test
    @DisplayName("Should get top balances successfully")
    void shouldGetTopBalancesSuccessfully() throws ExecutionException, InterruptedException {
        int limit = 10;
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();

        List<BalanceResponseDTO> expectedBalances = List.of(
                new BalanceResponseDTO(uuid1, 1000_0000L),
                new BalanceResponseDTO(uuid2, 500_0000L),
                new BalanceResponseDTO(uuid3, 250_0000L)
        );

        String jsonResponse = gson.toJson(expectedBalances);
        HttpResponse<String> mockResponse = createMockResponse(jsonResponse);

        when(httpClient.get("/balances/top?limit=" + limit))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        List<BalanceResponseDTO> result = service.getTopBalances(limit).get();

        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals(uuid1, result.get(0).uuid());
        assertEquals(1000_0000L, result.get(0).amount());
        assertEquals(uuid2, result.get(1).uuid());
        assertEquals(500_0000L, result.get(1).amount());

        verify(httpClient).get("/balances/top?limit=" + limit);
    }

    @Test
    @DisplayName("Should get top balances with custom limit")
    void shouldGetTopBalancesWithCustomLimit() throws ExecutionException, InterruptedException {
        int customLimit = 25;
        List<BalanceResponseDTO> balances = new ArrayList<>();
        for (int i = 0; i < customLimit; i++) {
            balances.add(new BalanceResponseDTO(UUID.randomUUID(), (long) i * 10000));
        }

        String jsonResponse = gson.toJson(balances);
        HttpResponse<String> mockResponse = createMockResponse(jsonResponse);

        when(httpClient.get("/balances/top?limit=" + customLimit))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        List<BalanceResponseDTO> result = service.getTopBalances(customLimit).get();

        assertEquals(customLimit, result.size());
    }

    @Test
    @DisplayName("Should get empty list when no balances exist")
    void shouldGetEmptyListWhenNoBalancesExist() throws ExecutionException, InterruptedException {
        String jsonResponse = gson.toJson(List.of());
        HttpResponse<String> mockResponse = createMockResponse(jsonResponse);

        when(httpClient.get("/balances/top?limit=10"))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        List<BalanceResponseDTO> result = service.getTopBalances(10).get();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should handle HTTP error on get top balances")
    void shouldHandleHttpErrorOnGetTopBalances() {
        when(httpClient.get("/balances/top?limit=10"))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API Error")));

        CompletableFuture<List<BalanceResponseDTO>> result = service.getTopBalances(10);

        ExecutionException exception = assertThrows(ExecutionException.class, result::get);
        assertEquals("API Error", exception.getCause().getMessage());
    }

    @Test
    @DisplayName("Should handle malformed JSON on get top balances")
    void shouldHandleMalformedJsonOnGetTopBalances() {
        HttpResponse<String> mockResponse = createMockResponse("{invalid json}");
        when(httpClient.get("/balances/top?limit=10"))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        assertThrows(ExecutionException.class,
                () -> service.getTopBalances(10).get());
    }

    @Test
    @DisplayName("Should get top balances with limit 1")
    void shouldGetTopBalancesWithLimitOne() throws ExecutionException, InterruptedException {
        UUID topUuid = UUID.randomUUID();
        List<BalanceResponseDTO> balances = List.of(
                new BalanceResponseDTO(topUuid, 9999_0000L)
        );

        String jsonResponse = gson.toJson(balances);
        HttpResponse<String> mockResponse = createMockResponse(jsonResponse);

        when(httpClient.get("/balances/top?limit=1"))
                .thenReturn(CompletableFuture.completedFuture(mockResponse));

        List<BalanceResponseDTO> result = service.getTopBalances(1).get();

        assertEquals(1, result.size());
        assertEquals(topUuid, result.getFirst().uuid());
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> createMockResponse(String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        return response;
    }
}
