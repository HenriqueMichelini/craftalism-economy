package io.github.HenriqueMichelini.craftalism.economy.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.HenriqueMichelini.craftalism.economy.application.dto.PayExecutionResult;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.enums.PayStatus;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.client.HttpClientService;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.client.OAuth2TokenService;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.PlayerResponseDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.repository.PlayerCacheRepository;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.BalanceApiService;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.PlayerApiService;
import io.github.HenriqueMichelini.craftalism.economy.infra.config.GsonFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PayCommandApplicationServiceIntegrationTest {

    private static final Gson GSON = GsonFactory.getInstance();
    private HttpServer server;

    @AfterEach
    void cleanup() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldPaySuccessfullyUsingTransferEndpoint() throws Exception {
        TestApiStub api = startServer(TransferBehavior.SUCCESS, true);
        PayCommandApplicationService service = buildService(api.baseUrl(), false);

        PayExecutionResult result = service.execute(api.payerUuid, "payer", "receiver", 10L).get();

        assertEquals(PayStatus.SUCCESS, result.getStatus());
        assertEquals(1, api.transferCalls);
        assertEquals(0, api.withdrawCalls + api.depositCalls);
    }

    @Test
    void shouldSerializeFromAndToPlayerUuidFieldsForTransfer() throws Exception {
        TestApiStub api = startServer(TransferBehavior.SUCCESS, true);
        PayCommandApplicationService service = buildService(api.baseUrl(), false);

        PayExecutionResult result = service.execute(api.payerUuid, "payer", "receiver", 10L).get();

        assertEquals(PayStatus.SUCCESS, result.getStatus());
        assertEquals(1, api.transferCalls);
    }

    @Test
    void shouldMapInsufficientFundsFromTransferContract() throws Exception {
        TestApiStub api = startServer(TransferBehavior.INSUFFICIENT_FUNDS, true);
        PayCommandApplicationService service = buildService(api.baseUrl(), false);

        PayExecutionResult result = service.execute(api.payerUuid, "payer", "receiver", 20L).get();

        assertEquals(PayStatus.NOT_ENOUGH_FUNDS, result.getStatus());
    }

    @Test
    void shouldMapGenericTransferFailure() throws Exception {
        TestApiStub api = startServer(TransferBehavior.SERVER_ERROR, true);
        PayCommandApplicationService service = buildService(api.baseUrl(), false);

        PayExecutionResult result = service.execute(api.payerUuid, "payer", "receiver", 20L).get();

        assertEquals(PayStatus.TRANSFER_TEMPORARILY_UNAVAILABLE, result.getStatus());
    }

    @Test
    void shouldFallbackToLegacyFlowWhenTransferEndpointUnavailableAndEnabled() throws Exception {
        TestApiStub api = startServer(TransferBehavior.ENDPOINT_UNAVAILABLE, true);
        PayCommandApplicationService service = buildService(api.baseUrl(), true);

        PayExecutionResult result = service.execute(api.payerUuid, "payer", "receiver", 25L).get();

        assertEquals(PayStatus.SUCCESS, result.getStatus());
        assertEquals(1, api.transferCalls);
        assertEquals(1, api.withdrawCalls);
        assertEquals(1, api.depositCalls);
    }

    @Test
    void shouldFailWhenTransferEndpointUnavailableAndFallbackDisabled() throws Exception {
        TestApiStub api = startServer(TransferBehavior.ENDPOINT_UNAVAILABLE, true);
        PayCommandApplicationService service = buildService(api.baseUrl(), false);

        PayExecutionResult result = service.execute(api.payerUuid, "payer", "receiver", 25L).get();

        assertEquals(PayStatus.TRANSFER_ENDPOINT_UNAVAILABLE, result.getStatus());
        assertEquals(0, api.withdrawCalls + api.depositCalls);
    }

    @Test
    void shouldFailFallbackWhenLegacyEndpointAlsoUnavailable() throws Exception {
        TestApiStub api = startServer(TransferBehavior.ENDPOINT_UNAVAILABLE, false);
        PayCommandApplicationService service = buildService(api.baseUrl(), true);

        PayExecutionResult result = service.execute(api.payerUuid, "payer", "receiver", 25L).get();

        assertEquals(PayStatus.TRANSFER_TEMPORARILY_UNAVAILABLE, result.getStatus());
    }

    private PayCommandApplicationService buildService(String baseUrl, boolean legacyFallbackEnabled) {
        OAuth2TokenService tokenService = mock(OAuth2TokenService.class);
        when(tokenService.getToken()).thenReturn(java.util.concurrent.CompletableFuture.completedFuture("test-token"));

        HttpClientService http = new HttpClientService(baseUrl, tokenService, 2, 2);
        PlayerApiService playerApi = new PlayerApiService(http);
        BalanceApiService balanceApi = new BalanceApiService(http);
        PlayerApplicationService playerApp = new PlayerApplicationService(playerApi, new PlayerCacheRepository());
        PaymentTransferService transferService = new PaymentTransferService(balanceApi, legacyFallbackEnabled, null);

        return new PayCommandApplicationService(playerApp, playerApi, transferService, null);
    }

    private TestApiStub startServer(TransferBehavior transferBehavior, boolean legacyAvailable) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        TestApiStub stub = new TestApiStub(transferBehavior, legacyAvailable);

        server.createContext("/api/players/name", stub::handlePlayerByName);
        server.createContext("/api/players", stub::handlePlayers);
        server.createContext("/api/balances/transfer", stub::handleTransfer);
        server.createContext("/api/balances", stub::handleBalances);
        server.start();

        stub.port = server.getAddress().getPort();
        return stub;
    }

    private enum TransferBehavior {
        SUCCESS,
        INSUFFICIENT_FUNDS,
        SERVER_ERROR,
        ENDPOINT_UNAVAILABLE,
    }

    private static class TestApiStub {

        private final UUID payerUuid = UUID.randomUUID();
        private final UUID receiverUuid = UUID.randomUUID();
        private final Map<UUID, String> players = new HashMap<>();
        private final TransferBehavior transferBehavior;
        private final boolean legacyAvailable;
        private int port;

        private int transferCalls;
        private int withdrawCalls;
        private int depositCalls;

        private TestApiStub(TransferBehavior transferBehavior, boolean legacyAvailable) {
            this.transferBehavior = transferBehavior;
            this.legacyAvailable = legacyAvailable;
            players.put(payerUuid, "payer");
            players.put(receiverUuid, "receiver");
        }

        private String baseUrl() {
            return "http://localhost:" + port;
        }

        private void handlePlayerByName(HttpExchange exchange) throws IOException {
            String[] parts = exchange.getRequestURI().getPath().split("/");
            String name = parts[parts.length - 1];
            if ("receiver".equals(name)) {
                PlayerResponseDTO dto = new PlayerResponseDTO(receiverUuid, "receiver", Instant.now());
                writeJson(exchange, 200, GSON.toJson(dto));
                return;
            }
            writeJson(exchange, 404, "{\"error\":\"not found\"}");
        }

        private void handlePlayers(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api/players") && exchange.getRequestMethod().equals("POST")) {
                PlayerResponseDTO dto = new PlayerResponseDTO(payerUuid, "payer", Instant.now());
                writeJson(exchange, 201, GSON.toJson(dto));
                return;
            }

            if (path.startsWith("/api/players/") && exchange.getRequestMethod().equals("GET")) {
                String id = path.substring("/api/players/".length());
                UUID uuid = UUID.fromString(id);
                if (players.containsKey(uuid)) {
                    PlayerResponseDTO dto = new PlayerResponseDTO(uuid, players.get(uuid), Instant.now());
                    writeJson(exchange, 200, GSON.toJson(dto));
                    return;
                }
            }

            writeJson(exchange, 404, "{\"error\":\"not found\"}");
        }

        private void handleTransfer(HttpExchange exchange) throws IOException {
            transferCalls++;
            String body = readRequestBody(exchange);
            String idempotencyKey = exchange.getRequestHeaders().getFirst("Idempotency-Key");

            if (
                !body.contains("\"fromPlayerUuid\":\"" + payerUuid + "\"") ||
                !body.contains("\"toPlayerUuid\":\"" + receiverUuid + "\"") ||
                body.contains("senderUuid") ||
                body.contains("receiverUuid")
            ) {
                writeJson(exchange, 400, "{\"error\":\"invalid_transfer_payload\"}");
                return;
            }

            if (idempotencyKey == null || idempotencyKey.isBlank()) {
                writeJson(exchange, 400, "{\"error\":\"missing_idempotency_key\"}");
                return;
            }

            switch (transferBehavior) {
                case SUCCESS -> writeJson(exchange, 204, "");
                case INSUFFICIENT_FUNDS -> writeJson(
                    exchange,
                    422,
                    "{\"type\":\"https://api.craftalism.com/errors/business-rule\",\"title\":\"Unprocessable Entity\",\"status\":422,\"detail\":\"Insufficient funds for uuid: " + payerUuid + " | amount: 20\"}"
                );
                case SERVER_ERROR -> writeJson(exchange, 503, "{\"error\":\"temporarily_unavailable\"}");
                case ENDPOINT_UNAVAILABLE -> writeJson(exchange, 404, "{\"error\":\"route not found\"}");
            }
        }

        private void handleBalances(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (!legacyAvailable) {
                writeJson(exchange, 503, "{\"error\":\"legacy disabled\"}");
                return;
            }

            if (path.contains("/withdraw")) {
                withdrawCalls++;
                writeJson(exchange, 204, "");
                return;
            }

            if (path.contains("/deposit")) {
                depositCalls++;
                writeJson(exchange, 204, "");
                return;
            }

            writeJson(exchange, 404, "{\"error\":\"not found\"}");
        }

        private void writeJson(HttpExchange exchange, int status, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        }

        private String readRequestBody(HttpExchange exchange) throws IOException {
            try (InputStream inputStream = exchange.getRequestBody()) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
    }
}
