package io.github.HenriqueMichelini.craftalism.economy.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import io.github.HenriqueMichelini.craftalism.economy.application.dto.PayExecutionResult;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.enums.PayStatus;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.*;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.BalanceApiService;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentTransferServiceTest {

    private BalanceApiService balanceApi;
    private java.util.logging.Logger logger;
    private UUID payerUuid;
    private UUID receiverUuid;

    @BeforeEach
    void setUp() {
        balanceApi = mock(BalanceApiService.class);
        logger = mock(java.util.logging.Logger.class);
        payerUuid = UUID.randomUUID();
        receiverUuid = UUID.randomUUID();
    }

    @Test
    void shouldUseLegacyFallbackWhenEndpointUnavailableAndEnabled() {
        PaymentTransferService service = new PaymentTransferService(balanceApi, true, logger);
        when(balanceApi.transfer(payerUuid, receiverUuid, 100L)).thenReturn(
            CompletableFuture.failedFuture(new TransferEndpointUnavailableException("missing"))
        );
        when(balanceApi.withdraw(payerUuid, 100L)).thenReturn(CompletableFuture.completedFuture(null));
        when(balanceApi.deposit(receiverUuid, 100L)).thenReturn(CompletableFuture.completedFuture(null));

        PayExecutionResult result = service.execute(payerUuid, receiverUuid, 100L).join();

        assertEquals(PayStatus.SUCCESS, result.getStatus());
        verify(balanceApi).withdraw(payerUuid, 100L);
        verify(balanceApi).deposit(receiverUuid, 100L);
    }

    @Test
    void shouldReturnEndpointUnavailableWhenDisabled() {
        PaymentTransferService service = new PaymentTransferService(balanceApi, false, logger);
        when(balanceApi.transfer(payerUuid, receiverUuid, 100L)).thenReturn(
            CompletableFuture.failedFuture(new TransferEndpointUnavailableException("missing"))
        );

        PayExecutionResult result = service.execute(payerUuid, receiverUuid, 100L).join();

        assertEquals(PayStatus.TRANSFER_ENDPOINT_UNAVAILABLE, result.getStatus());
        verify(balanceApi, never()).withdraw(any(), anyLong());
    }

    @Test
    void shouldMapInsufficientFundsFromTransferException() {
        PaymentTransferService service = new PaymentTransferService(balanceApi, false, logger);
        when(balanceApi.transfer(payerUuid, receiverUuid, 100L)).thenReturn(
            CompletableFuture.failedFuture(new TransferApiException(TransferFailureReason.INSUFFICIENT_FUNDS, "no funds"))
        );

        PayExecutionResult result = service.execute(payerUuid, receiverUuid, 100L).join();

        assertEquals(PayStatus.NOT_ENOUGH_FUNDS, result.getStatus());
    }

    @Test
    void shouldMapDuplicateTransferExceptionToDuplicateStatus() {
        PaymentTransferService service = new PaymentTransferService(balanceApi, false, logger);
        when(balanceApi.transfer(payerUuid, receiverUuid, 100L)).thenReturn(
            CompletableFuture.failedFuture(new TransferApiException(TransferFailureReason.DUPLICATE_REQUEST, "duplicate"))
        );

        PayExecutionResult result = service.execute(payerUuid, receiverUuid, 100L).join();

        assertEquals(PayStatus.TRANSFER_DUPLICATE, result.getStatus());
    }

    @Test
    void shouldMapInvalidTransferExceptionToInvalidRequestStatus() {
        PaymentTransferService service = new PaymentTransferService(balanceApi, false, logger);
        when(balanceApi.transfer(payerUuid, receiverUuid, 100L)).thenReturn(
            CompletableFuture.failedFuture(new TransferApiException(TransferFailureReason.INVALID_REQUEST, "invalid"))
        );

        PayExecutionResult result = service.execute(payerUuid, receiverUuid, 100L).join();

        assertEquals(PayStatus.TRANSFER_INVALID_REQUEST, result.getStatus());
    }
}
