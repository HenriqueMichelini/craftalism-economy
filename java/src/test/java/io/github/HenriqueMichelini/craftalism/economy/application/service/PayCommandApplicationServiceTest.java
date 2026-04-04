package io.github.HenriqueMichelini.craftalism.economy.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.HenriqueMichelini.craftalism.economy.application.dto.PayExecutionResult;
import io.github.HenriqueMichelini.craftalism.economy.domain.model.Player;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.enums.PayStatus;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.PlayerResponseDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.NotFoundException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@DisplayName("PayCommandApplicationService Tests")
class PayCommandApplicationServiceTest {

    @Mock
    private PlayerApplicationService playerService;

    @Mock
    private io.github.HenriqueMichelini.craftalism.economy.infra.api.service.PlayerApiService playerApi;

    @Mock
    private PaymentTransferService paymentTransferService;

    @Mock
    private java.util.logging.Logger logger;

    private PayCommandApplicationService service;
    private UUID payerUuid;
    private UUID receiverUuid;
    private String payerName;
    private String receiverName;
    private long validAmount;
    private Player payerPlayer;
    private PlayerResponseDTO receiverDTO;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        service =
            new PayCommandApplicationService(
                playerService,
                playerApi,
                paymentTransferService,
                logger
            );

        payerUuid = UUID.randomUUID();
        receiverUuid = UUID.randomUUID();
        payerName = "Payer";
        receiverName = "Receiver";
        validAmount = 100_0000L;
        payerPlayer = new Player(payerUuid, payerName, Instant.now());
        receiverDTO = new PlayerResponseDTO(receiverUuid, receiverName, Instant.now());
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void shouldCompleteSuccessfulPayment() throws ExecutionException, InterruptedException {
        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.completedFuture(payerPlayer)
        );
        when(playerApi.getPlayerByName(receiverName)).thenReturn(
            CompletableFuture.completedFuture(receiverDTO)
        );
        when(paymentTransferService.execute(payerUuid, receiverUuid, validAmount)).thenReturn(
            CompletableFuture.completedFuture(PayExecutionResult.success(receiverUuid))
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, receiverName, validAmount)
            .get();

        assertEquals(PayStatus.SUCCESS, result.getStatus());
        verify(paymentTransferService).execute(payerUuid, receiverUuid, validAmount);
    }

    @Test
    void shouldReturnErrorWhenTransferFails() throws ExecutionException, InterruptedException {
        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.completedFuture(payerPlayer)
        );
        when(playerApi.getPlayerByName(receiverName)).thenReturn(
            CompletableFuture.completedFuture(receiverDTO)
        );
        when(paymentTransferService.execute(payerUuid, receiverUuid, validAmount)).thenReturn(
            CompletableFuture.completedFuture(PayExecutionResult.exception())
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, receiverName, validAmount)
            .get();

        assertEquals(PayStatus.ERROR, result.getStatus());
    }

    @Test
    void shouldRejectSelfPayment() throws ExecutionException, InterruptedException {
        PlayerResponseDTO selfDTO = new PlayerResponseDTO(payerUuid, payerName, Instant.now());
        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.completedFuture(payerPlayer)
        );
        when(playerApi.getPlayerByName(payerName)).thenReturn(
            CompletableFuture.completedFuture(selfDTO)
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, payerName, validAmount)
            .get();

        assertEquals(PayStatus.CANNOT_PAY_SELF, result.getStatus());
        verify(paymentTransferService, never()).execute(any(), any(), anyLong());
    }

    @Test
    void shouldReturnTargetNotFoundWhenReceiverNotFound() throws ExecutionException, InterruptedException {
        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.completedFuture(payerPlayer)
        );
        when(playerApi.getPlayerByName(receiverName)).thenReturn(
            CompletableFuture.failedFuture(new NotFoundException("Player not found"))
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, receiverName, validAmount)
            .get();

        assertEquals(PayStatus.TARGET_NOT_FOUND, result.getStatus());
    }
}
