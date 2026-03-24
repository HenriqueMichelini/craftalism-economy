package io.github.HenriqueMichelini.craftalism.economy.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.github.HenriqueMichelini.craftalism.economy.application.dto.PayExecutionResult;
import io.github.HenriqueMichelini.craftalism.economy.domain.model.Player;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.enums.PayStatus;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.BalanceResponseDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.PlayerResponseDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.dto.TransactionResponseDTO;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.exceptions.NotFoundException;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.BalanceApiService;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.PlayerApiService;
import io.github.HenriqueMichelini.craftalism.economy.infra.api.service.TransactionApiService;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.bukkit.plugin.java.JavaPlugin;
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
    private PlayerApiService playerApi;

    @Mock
    private BalanceApiService balanceApi;

    @Mock
    private TransactionApiService transactionApi;

    @Mock
    private JavaPlugin plugin;

    @Mock
    private java.util.logging.Logger logger;

    private PayCommandApplicationService service;

    private UUID payerUuid;
    private UUID receiverUuid;
    private String payerName;
    private String receiverName;
    private Long validAmount;
    private Player payerPlayer;
    private PlayerResponseDTO receiverDTO;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        when(plugin.getLogger()).thenReturn(logger);

        service = new PayCommandApplicationService(
            playerService,
            playerApi,
            balanceApi,
            transactionApi,
            plugin
        );

        payerUuid = UUID.randomUUID();
        receiverUuid = UUID.randomUUID();
        payerName = "Payer";
        receiverName = "Receiver";
        validAmount = 100_0000L;

        payerPlayer = new Player(payerUuid, payerName, Instant.now());
        receiverDTO = new PlayerResponseDTO(
            receiverUuid,
            receiverName,
            Instant.now()
        );
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    @DisplayName("Should complete successful payment")
    void shouldCompleteSuccessfulPayment()
        throws ExecutionException, InterruptedException {
        BalanceResponseDTO dto = new BalanceResponseDTO(payerUuid, 500_0000L);

        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.completedFuture(payerPlayer)
        );
        when(playerApi.getPlayerByName(receiverName)).thenReturn(
            CompletableFuture.completedFuture(receiverDTO)
        );
        when(balanceApi.getBalance(payerUuid)).thenReturn(
            CompletableFuture.completedFuture(dto)
        );
        when(balanceApi.withdraw(payerUuid, validAmount)).thenReturn(
            CompletableFuture.completedFuture(null)
        );
        when(balanceApi.deposit(receiverUuid, validAmount)).thenReturn(
            CompletableFuture.completedFuture(null)
        );
        when(
            transactionApi.register(payerUuid, receiverUuid, validAmount)
        ).thenReturn(
            CompletableFuture.completedFuture(
                new TransactionResponseDTO(
                    1L,
                    payerUuid,
                    receiverUuid,
                    validAmount,
                    Instant.now()
                )
            )
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, receiverName, validAmount)
            .get();

        assertEquals(PayStatus.SUCCESS, result.getStatus());
        verify(playerService).getCachedOrFetch(payerUuid, payerName);
        verify(playerApi).getPlayerByName(receiverName);
        verify(balanceApi).getBalance(payerUuid);
        verify(balanceApi).withdraw(payerUuid, validAmount);
        verify(balanceApi).deposit(receiverUuid, validAmount);
        verify(transactionApi).register(payerUuid, receiverUuid, validAmount);
    }

    @Test
    @DisplayName("Should handle exact balance payment")
    void shouldHandleExactBalancePayment()
        throws ExecutionException, InterruptedException {
        Long exactAmount = 100_0000L;
        BalanceResponseDTO dto = new BalanceResponseDTO(payerUuid, exactAmount);

        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.completedFuture(payerPlayer)
        );
        when(playerApi.getPlayerByName(receiverName)).thenReturn(
            CompletableFuture.completedFuture(receiverDTO)
        );
        when(balanceApi.getBalance(payerUuid)).thenReturn(
            CompletableFuture.completedFuture(dto)
        );
        when(balanceApi.withdraw(payerUuid, exactAmount)).thenReturn(
            CompletableFuture.completedFuture(null)
        );
        when(balanceApi.deposit(receiverUuid, exactAmount)).thenReturn(
            CompletableFuture.completedFuture(null)
        );
        when(
            transactionApi.register(payerUuid, receiverUuid, exactAmount)
        ).thenReturn(
            CompletableFuture.completedFuture(
                new TransactionResponseDTO(
                    1L,
                    payerUuid,
                    receiverUuid,
                    exactAmount,
                    Instant.now()
                )
            )
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, receiverName, exactAmount)
            .get();

        assertEquals(PayStatus.SUCCESS, result.getStatus());
    }

    @Test
    @DisplayName("Should reject payment to self")
    void shouldRejectPaymentToSelf()
        throws ExecutionException, InterruptedException {
        PlayerResponseDTO selfDTO = new PlayerResponseDTO(
            payerUuid,
            payerName,
            Instant.now()
        );

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
        verify(balanceApi, never()).getBalance(any());
        verify(balanceApi, never()).withdraw(any(), anyLong());
    }

    @Test
    @DisplayName("Should reject zero amount payment")
    void shouldRejectZeroAmountPayment()
        throws ExecutionException, InterruptedException {
        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.completedFuture(payerPlayer)
        );
        when(playerApi.getPlayerByName(receiverName)).thenReturn(
            CompletableFuture.completedFuture(receiverDTO)
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, receiverName, 0L)
            .get();

        assertEquals(PayStatus.INVALID_AMOUNT, result.getStatus());
        verify(balanceApi, never()).getBalance(any());
        verify(balanceApi, never()).withdraw(any(), anyLong());
    }

    @Test
    @DisplayName("Should reject negative amount payment")
    void shouldRejectNegativeAmountPayment()
        throws ExecutionException, InterruptedException {
        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.completedFuture(payerPlayer)
        );
        when(playerApi.getPlayerByName(receiverName)).thenReturn(
            CompletableFuture.completedFuture(receiverDTO)
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, receiverName, -100L)
            .get();

        assertEquals(PayStatus.INVALID_AMOUNT, result.getStatus());
        verify(balanceApi, never()).getBalance(any());
    }

    @Test
    @DisplayName("Should return TARGET_NOT_FOUND when receiver not found")
    void shouldReturnTargetNotFoundWhenReceiverNotFound()
        throws ExecutionException, InterruptedException {
        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.completedFuture(payerPlayer)
        );
        when(playerApi.getPlayerByName(receiverName)).thenReturn(
            CompletableFuture.failedFuture(
                new NotFoundException("Player not found")
            )
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, receiverName, validAmount)
            .get();

        assertEquals(PayStatus.TARGET_NOT_FOUND, result.getStatus());
        verify(balanceApi, never()).getBalance(any());
    }

    @Test
    @DisplayName(
        "Should return ERROR when receiver lookup fails with non-NotFoundException"
    )
    void shouldReturnErrorWhenReceiverLookupFails()
        throws ExecutionException, InterruptedException {
        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.completedFuture(payerPlayer)
        );
        when(playerApi.getPlayerByName(receiverName)).thenReturn(
            CompletableFuture.failedFuture(new RuntimeException("API Error"))
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, receiverName, validAmount)
            .get();

        assertEquals(PayStatus.ERROR, result.getStatus());
        verify(balanceApi, never()).getBalance(any());
    }

    @Test
    @DisplayName("Should reject payment when payer has insufficient funds")
    void shouldRejectPaymentWhenInsufficientFunds()
        throws ExecutionException, InterruptedException {
        BalanceResponseDTO dto = new BalanceResponseDTO(payerUuid, 50_0000L);

        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.completedFuture(payerPlayer)
        );
        when(playerApi.getPlayerByName(receiverName)).thenReturn(
            CompletableFuture.completedFuture(receiverDTO)
        );
        when(balanceApi.getBalance(payerUuid)).thenReturn(
            CompletableFuture.completedFuture(dto)
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, receiverName, validAmount)
            .get();

        assertEquals(PayStatus.NOT_ENOUGH_FUNDS, result.getStatus());
        verify(balanceApi).getBalance(payerUuid);
        verify(balanceApi, never()).withdraw(any(), anyLong());
        verify(balanceApi, never()).deposit(any(), anyLong());
        verify(transactionApi, never()).register(any(), any(), anyLong());
    }

    @Test
    @DisplayName(
        "Should reject payment when balance is exactly one less than amount"
    )
    void shouldRejectPaymentWhenBalanceOneShort()
        throws ExecutionException, InterruptedException {
        BalanceResponseDTO dto = new BalanceResponseDTO(
            payerUuid,
            validAmount - 1
        );

        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.completedFuture(payerPlayer)
        );
        when(playerApi.getPlayerByName(receiverName)).thenReturn(
            CompletableFuture.completedFuture(receiverDTO)
        );
        when(balanceApi.getBalance(payerUuid)).thenReturn(
            CompletableFuture.completedFuture(dto)
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, receiverName, validAmount)
            .get();

        assertEquals(PayStatus.NOT_ENOUGH_FUNDS, result.getStatus());
    }

    @Test
    @DisplayName(
        "Should return TARGET_NOT_FOUND when getting payer fails with NotFoundException"
    )
    void shouldReturnTargetNotFoundWhenGettingPayerFails()
        throws ExecutionException, InterruptedException {
        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.failedFuture(
                new NotFoundException("Payer not found")
            )
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, receiverName, validAmount)
            .get();

        assertEquals(PayStatus.TARGET_NOT_FOUND, result.getStatus());
        verify(playerApi, never()).getPlayerByName(any());
        verify(balanceApi, never()).getBalance(any());
    }

    @Test
    @DisplayName(
        "Should return ERROR when getting payer fails with non-NotFoundException"
    )
    void shouldReturnErrorWhenGettingPayerFailsWithOtherException()
        throws ExecutionException, InterruptedException {
        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.failedFuture(
                new RuntimeException("Database error")
            )
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, receiverName, validAmount)
            .get();

        assertEquals(PayStatus.ERROR, result.getStatus());
        verify(playerApi, never()).getPlayerByName(any());
        verify(balanceApi, never()).getBalance(any());
    }

    @Test
    @DisplayName("Should return ERROR when balance check fails")
    void shouldReturnErrorDuringBalanceCheck()
        throws ExecutionException, InterruptedException {
        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.completedFuture(payerPlayer)
        );
        when(playerApi.getPlayerByName(receiverName)).thenReturn(
            CompletableFuture.completedFuture(receiverDTO)
        );
        when(balanceApi.getBalance(payerUuid)).thenReturn(
            CompletableFuture.failedFuture(
                new RuntimeException("Balance API error")
            )
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, receiverName, validAmount)
            .get();

        assertEquals(PayStatus.ERROR, result.getStatus());
        verify(balanceApi, never()).withdraw(any(), anyLong());
    }

    @Test
    @DisplayName("Should return ERROR when withdraw fails")
    void shouldReturnErrorDuringWithdraw()
        throws ExecutionException, InterruptedException {
        BalanceResponseDTO dto = new BalanceResponseDTO(payerUuid, 500_0000L);

        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.completedFuture(payerPlayer)
        );
        when(playerApi.getPlayerByName(receiverName)).thenReturn(
            CompletableFuture.completedFuture(receiverDTO)
        );
        when(balanceApi.getBalance(payerUuid)).thenReturn(
            CompletableFuture.completedFuture(dto)
        );
        when(balanceApi.withdraw(payerUuid, validAmount)).thenReturn(
            CompletableFuture.failedFuture(
                new RuntimeException("Withdraw failed")
            )
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, receiverName, validAmount)
            .get();

        assertEquals(PayStatus.ERROR, result.getStatus());
        verify(balanceApi).withdraw(payerUuid, validAmount);
        verify(balanceApi, never()).deposit(receiverUuid, validAmount);
        verify(transactionApi, never()).register(any(), any(), anyLong());
    }

    @Test
    @DisplayName("Should return ERROR when deposit fails and rollback succeeds")
    void shouldReturnErrorWhenDepositFailsAndRollbackSucceeds()
        throws ExecutionException, InterruptedException {
        BalanceResponseDTO dto = new BalanceResponseDTO(payerUuid, 500_0000L);

        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.completedFuture(payerPlayer)
        );
        when(playerApi.getPlayerByName(receiverName)).thenReturn(
            CompletableFuture.completedFuture(receiverDTO)
        );
        when(balanceApi.getBalance(payerUuid)).thenReturn(
            CompletableFuture.completedFuture(dto)
        );
        when(balanceApi.withdraw(payerUuid, validAmount)).thenReturn(
            CompletableFuture.completedFuture(null)
        );
        when(balanceApi.deposit(receiverUuid, validAmount)).thenReturn(
            CompletableFuture.failedFuture(
                new RuntimeException("Deposit failed")
            )
        );
        when(balanceApi.deposit(payerUuid, validAmount)).thenReturn(
            CompletableFuture.completedFuture(null)
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, receiverName, validAmount)
            .get();

        assertEquals(PayStatus.ERROR, result.getStatus());
        verify(balanceApi).withdraw(payerUuid, validAmount);
        verify(balanceApi).deposit(receiverUuid, validAmount);
        verify(balanceApi).deposit(payerUuid, validAmount);
        verify(transactionApi, never()).register(any(), any(), anyLong());
    }

    @Test
    @DisplayName(
        "Should return SUCCESS even when transaction registration fails"
    )
    void shouldReturnSuccessWhenTransactionRegistrationFails()
        throws ExecutionException, InterruptedException {
        BalanceResponseDTO dto = new BalanceResponseDTO(payerUuid, 500_0000L);

        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.completedFuture(payerPlayer)
        );
        when(playerApi.getPlayerByName(receiverName)).thenReturn(
            CompletableFuture.completedFuture(receiverDTO)
        );
        when(balanceApi.getBalance(payerUuid)).thenReturn(
            CompletableFuture.completedFuture(dto)
        );
        when(balanceApi.withdraw(payerUuid, validAmount)).thenReturn(
            CompletableFuture.completedFuture(null)
        );
        when(balanceApi.deposit(receiverUuid, validAmount)).thenReturn(
            CompletableFuture.completedFuture(null)
        );
        when(
            transactionApi.register(payerUuid, receiverUuid, validAmount)
        ).thenReturn(
            CompletableFuture.failedFuture(
                new RuntimeException("Transaction log failed")
            )
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, receiverName, validAmount)
            .get();

        assertEquals(PayStatus.SUCCESS, result.getStatus());
        verify(transactionApi).register(payerUuid, receiverUuid, validAmount);
    }

    @Test
    @DisplayName("Should return ERROR when both deposit and rollback fail")
    void shouldReturnErrorWhenBothDepositAndRollbackFail()
        throws ExecutionException, InterruptedException {
        BalanceResponseDTO dto = new BalanceResponseDTO(payerUuid, 500_0000L);

        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.completedFuture(payerPlayer)
        );
        when(playerApi.getPlayerByName(receiverName)).thenReturn(
            CompletableFuture.completedFuture(receiverDTO)
        );
        when(balanceApi.getBalance(payerUuid)).thenReturn(
            CompletableFuture.completedFuture(dto)
        );
        when(balanceApi.withdraw(payerUuid, validAmount)).thenReturn(
            CompletableFuture.completedFuture(null)
        );
        when(balanceApi.deposit(receiverUuid, validAmount)).thenReturn(
            CompletableFuture.failedFuture(
                new RuntimeException("Deposit failed")
            )
        );
        when(balanceApi.deposit(payerUuid, validAmount)).thenReturn(
            CompletableFuture.failedFuture(
                new RuntimeException("Rollback failed")
            )
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, receiverName, validAmount)
            .get();

        assertEquals(PayStatus.ERROR, result.getStatus());
        verify(balanceApi).withdraw(payerUuid, validAmount);
        verify(balanceApi).deposit(receiverUuid, validAmount);
        verify(balanceApi).deposit(payerUuid, validAmount);
        verify(transactionApi, never()).register(any(), any(), anyLong());
    }

    @Test
    @DisplayName("Should handle payment with very large valid amount")
    void shouldHandlePaymentWithVeryLargeAmount()
        throws ExecutionException, InterruptedException {
        Long largeAmount = 1_000_000_0000L;
        BalanceResponseDTO dto = new BalanceResponseDTO(
            payerUuid,
            2_000_000_0000L
        );

        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.completedFuture(payerPlayer)
        );
        when(playerApi.getPlayerByName(receiverName)).thenReturn(
            CompletableFuture.completedFuture(receiverDTO)
        );
        when(balanceApi.getBalance(payerUuid)).thenReturn(
            CompletableFuture.completedFuture(dto)
        );
        when(balanceApi.withdraw(payerUuid, largeAmount)).thenReturn(
            CompletableFuture.completedFuture(null)
        );
        when(balanceApi.deposit(receiverUuid, largeAmount)).thenReturn(
            CompletableFuture.completedFuture(null)
        );
        when(
            transactionApi.register(payerUuid, receiverUuid, largeAmount)
        ).thenReturn(
            CompletableFuture.completedFuture(
                new TransactionResponseDTO(
                    1L,
                    payerUuid,
                    receiverUuid,
                    largeAmount,
                    Instant.now()
                )
            )
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, receiverName, largeAmount)
            .get();

        assertEquals(PayStatus.SUCCESS, result.getStatus());
    }

    @Test
    @DisplayName("Should handle payment with minimum positive amount")
    void shouldHandlePaymentWithMinimumAmount()
        throws ExecutionException, InterruptedException {
        Long minAmount = 1L;
        BalanceResponseDTO dto = new BalanceResponseDTO(payerUuid, 100_0000L);

        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.completedFuture(payerPlayer)
        );
        when(playerApi.getPlayerByName(receiverName)).thenReturn(
            CompletableFuture.completedFuture(receiverDTO)
        );
        when(balanceApi.getBalance(payerUuid)).thenReturn(
            CompletableFuture.completedFuture(dto)
        );
        when(balanceApi.withdraw(payerUuid, minAmount)).thenReturn(
            CompletableFuture.completedFuture(null)
        );
        when(balanceApi.deposit(receiverUuid, minAmount)).thenReturn(
            CompletableFuture.completedFuture(null)
        );
        when(
            transactionApi.register(payerUuid, receiverUuid, minAmount)
        ).thenReturn(
            CompletableFuture.completedFuture(
                new TransactionResponseDTO(
                    1L,
                    payerUuid,
                    receiverUuid,
                    minAmount,
                    Instant.now()
                )
            )
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, receiverName, minAmount)
            .get();

        assertEquals(PayStatus.SUCCESS, result.getStatus());
    }

    @Test
    @DisplayName("Should handle payment when payer balance is zero")
    void shouldHandlePaymentWhenPayerBalanceIsZero()
        throws ExecutionException, InterruptedException {
        BalanceResponseDTO dto = new BalanceResponseDTO(payerUuid, 0L);

        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.completedFuture(payerPlayer)
        );
        when(playerApi.getPlayerByName(receiverName)).thenReturn(
            CompletableFuture.completedFuture(receiverDTO)
        );
        when(balanceApi.getBalance(payerUuid)).thenReturn(
            CompletableFuture.completedFuture(dto)
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, receiverName, validAmount)
            .get();

        assertEquals(PayStatus.NOT_ENOUGH_FUNDS, result.getStatus());
    }

    @Test
    @DisplayName("Should handle special characters in player names")
    void shouldHandleSpecialCharactersInNames()
        throws ExecutionException, InterruptedException {
        String specialName = "Player_123-XYZ";
        UUID specialUuid = UUID.randomUUID();
        PlayerResponseDTO specialDTO = new PlayerResponseDTO(
            specialUuid,
            specialName,
            Instant.now()
        );
        BalanceResponseDTO dto = new BalanceResponseDTO(payerUuid, 500_0000L);

        when(playerService.getCachedOrFetch(payerUuid, payerName)).thenReturn(
            CompletableFuture.completedFuture(payerPlayer)
        );
        when(playerApi.getPlayerByName(specialName)).thenReturn(
            CompletableFuture.completedFuture(specialDTO)
        );
        when(balanceApi.getBalance(payerUuid)).thenReturn(
            CompletableFuture.completedFuture(dto)
        );
        when(balanceApi.withdraw(payerUuid, validAmount)).thenReturn(
            CompletableFuture.completedFuture(null)
        );
        when(balanceApi.deposit(specialUuid, validAmount)).thenReturn(
            CompletableFuture.completedFuture(null)
        );
        when(
            transactionApi.register(payerUuid, specialUuid, validAmount)
        ).thenReturn(
            CompletableFuture.completedFuture(
                new TransactionResponseDTO(
                    1L,
                    payerUuid,
                    specialUuid,
                    validAmount,
                    Instant.now()
                )
            )
        );

        PayExecutionResult result = service
            .execute(payerUuid, payerName, specialName, validAmount)
            .get();

        assertEquals(PayStatus.SUCCESS, result.getStatus());
    }
}
