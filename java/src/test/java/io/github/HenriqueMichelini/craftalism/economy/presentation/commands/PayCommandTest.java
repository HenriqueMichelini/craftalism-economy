package io.github.HenriqueMichelini.craftalism.economy.presentation.commands;

import io.github.HenriqueMichelini.craftalism.economy.application.dto.PayExecutionResult;
import io.github.HenriqueMichelini.craftalism.economy.application.service.PayCommandApplicationService;
import io.github.HenriqueMichelini.craftalism.economy.application.service.TransactionApplicationService;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages.PayMessages;
import io.github.HenriqueMichelini.craftalism.economy.presentation.validation.PlayerNameCheck;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@DisplayName("PayCommand tests")
class PayCommandTest {

    @Mock
    private PayMessages messages;
    @Mock
    private PayCommandApplicationService payService;
    @Mock
    private TransactionApplicationService transactionService;
    @Mock
    private PlayerNameCheck playerNameCheck;
    @Mock
    private CurrencyFormatter formatter;

    @Mock
    private Player player;
    @Mock
    private Player receiver;
    @Mock
    private CommandSender console;
    @Mock
    private Command command;

    private PayCommand payCommand;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        payCommand = new PayCommand(messages, payService, transactionService, playerNameCheck, formatter);

        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Sender");
        when(player.hasPermission(anyString())).thenReturn(true);
        when(playerNameCheck.isValid(anyString())).thenReturn(true);
        when(formatter.fromDisplayValue(any(BigDecimal.class))).thenReturn(1_0000L);
        when(formatter.formatCurrency(1_0000L)).thenReturn("$1.00");
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    @DisplayName("should reject non-player sender")
    void shouldRejectNonPlayerSender() {
        boolean result = payCommand.onCommand(console, command, "pay", new String[]{"Target", "1"});

        assertTrue(result);
        verify(messages).sendPayPlayerOnly();
        verifyNoInteractions(payService);
    }

    @Test
    @DisplayName("should reject missing permission")
    void shouldRejectMissingPermission() {
        when(player.hasPermission(anyString())).thenReturn(false);

        boolean result = payCommand.onCommand(player, command, "pay", new String[]{"Target", "1"});

        assertTrue(result);
        verify(messages).sendPayNoPermission(player);
        verifyNoInteractions(payService);
    }

    @Test
    @DisplayName("should reject invalid amount format")
    void shouldRejectInvalidAmountFormat() {
        boolean result = payCommand.onCommand(player, command, "pay", new String[]{"Target", "abc"});

        assertTrue(result);
        verify(messages).sendPayInvalidAmount(player);
        verifyNoInteractions(payService);
    }

    @Test
    @DisplayName("should reject self payment")
    void shouldRejectSelfPayment() {
        boolean result = payCommand.onCommand(player, command, "pay", new String[]{"Sender", "1"});

        assertTrue(result);
        verify(messages).sendPaySelfPayment(player);
        verifyNoInteractions(payService);
    }

    @Test
    @DisplayName("should send success messages for sender and online receiver")
    void shouldSendSuccessMessagesForSenderAndOnlineReceiver() {
        when(payService.execute(any(), anyString(), anyString(), anyLong()))
                .thenReturn(CompletableFuture.completedFuture(PayExecutionResult.success(UUID.randomUUID())));
        when(receiver.isOnline()).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getPlayer("Target")).thenReturn(receiver);

            boolean result = payCommand.onCommand(player, command, "pay", new String[]{"Target", "1"});

            assertTrue(result);
            verify(payService).execute(eq(player.getUniqueId()), eq("Sender"), eq("Target"), eq(1_0000L));
            verify(messages).sendPaySuccessSender(player, "$1.00", "Target");
            verify(messages).sendPaySuccessReceiver(receiver, "$1.00", "Sender");
        }
    }

    @Test
    @DisplayName("should map target not found status")
    void shouldMapTargetNotFoundStatus() {
        when(payService.execute(any(), anyString(), anyString(), anyLong()))
                .thenReturn(CompletableFuture.completedFuture(PayExecutionResult.targetNotFound()));

        boolean result = payCommand.onCommand(player, command, "pay", new String[]{"Target", "1"});

        assertTrue(result);
        verify(messages).sendPayPlayerNotFound(player);
    }
}
