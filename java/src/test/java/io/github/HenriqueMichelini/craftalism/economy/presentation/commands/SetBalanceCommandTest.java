package io.github.HenriqueMichelini.craftalism.economy.presentation.commands;

import io.github.HenriqueMichelini.craftalism.economy.application.dto.SetBalanceExecutionResult;
import io.github.HenriqueMichelini.craftalism.economy.application.service.SetBalanceCommandApplicationService;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages.SetBalanceMessages;
import io.github.HenriqueMichelini.craftalism.economy.presentation.validation.PlayerNameCheck;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("SetBalanceCommand tests")
class SetBalanceCommandTest {

    @Mock
    private PlayerNameCheck playerNameCheck;
    @Mock
    private SetBalanceMessages messages;
    @Mock
    private SetBalanceCommandApplicationService service;
    @Mock
    private JavaPlugin plugin;
    @Mock
    private CurrencyFormatter formatter;
    @Mock
    private CommandSender sender;
    @Mock
    private Player senderPlayer;
    @Mock
    private Player receiver;
    @Mock
    private Command command;
    @Mock
    private BukkitScheduler scheduler;
    @Mock
    private BukkitTask task;
    @Mock
    private Logger logger;

    private SetBalanceCommand setBalanceCommand;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        setBalanceCommand = new SetBalanceCommand(playerNameCheck, messages, service, plugin, formatter);

        when(sender.hasPermission(anyString())).thenReturn(true);
        when(senderPlayer.hasPermission(anyString())).thenReturn(true);
        when(senderPlayer.getName()).thenReturn("AdminPlayer");

        when(playerNameCheck.isValid(anyString())).thenReturn(true);
        when(formatter.fromDisplayValue(any(BigDecimal.class))).thenReturn(1_2500L);
        when(formatter.formatCurrency(anyLong())).thenReturn("$1.25");

        when(plugin.getLogger()).thenReturn(logger);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    @DisplayName("should reject sender without permission")
    void shouldRejectSenderWithoutPermission() {
        when(sender.hasPermission(anyString())).thenReturn(false);

        boolean result = setBalanceCommand.onCommand(sender, command, "setbalance", new String[]{"Target", "1"});

        assertTrue(result);
        verify(messages).sendSetBalanceNoPermission(sender);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("should reject when args length is not exactly two")
    void shouldRejectWhenArgsLengthIsInvalid() {
        boolean result = setBalanceCommand.onCommand(sender, command, "setbalance", new String[]{"Target"});

        assertTrue(result);
        verify(messages).sendSetBalanceUsage(sender);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("should reject invalid target name")
    void shouldRejectInvalidTargetName() {
        when(playerNameCheck.isValid("Invalid@Name")).thenReturn(false);

        boolean result = setBalanceCommand.onCommand(sender, command, "setbalance", new String[]{"Invalid@Name", "10"});

        assertTrue(result);
        verify(messages).sendSetBalanceInvalidName(sender);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("should reject non numeric amount")
    void shouldRejectNonNumericAmount() {
        boolean result = setBalanceCommand.onCommand(sender, command, "setbalance", new String[]{"Target", "10.5"});

        assertTrue(result);
        verify(messages).sendSetBalanceInvalidAmount(sender);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("should reject amount when formatter conversion fails")
    void shouldRejectAmountWhenFormatterConversionFails() {
        when(formatter.fromDisplayValue(any(BigDecimal.class))).thenThrow(new ArithmeticException("overflow"));

        boolean result = setBalanceCommand.onCommand(sender, command, "setbalance", new String[]{"Target", "99999999999999999999"});

        assertTrue(result);
        verify(messages).sendSetBalanceInvalidAmount(sender);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("should reject negative amount after formatter conversion")
    void shouldRejectNegativeAmountAfterFormatterConversion() {
        when(formatter.fromDisplayValue(any(BigDecimal.class))).thenReturn(-1L);

        boolean result = setBalanceCommand.onCommand(sender, command, "setbalance", new String[]{"Target", "1"});

        assertTrue(result);
        verify(messages).sendSetBalanceInvalidAmount(sender);
        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("should send success messages to sender and online receiver for player sender")
    void shouldSendSuccessMessagesToSenderAndOnlineReceiverForPlayerSender() {
        UUID receiverUuid = UUID.randomUUID();
        SetBalanceExecutionResult successResult = SetBalanceExecutionResult.success(1_2500L, receiverUuid);
        when(service.execute("Target", 1_2500L)).thenReturn(CompletableFuture.completedFuture(successResult));
        when(receiver.isOnline()).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getPlayer(receiverUuid)).thenReturn(receiver);
            when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return task;
            });

            boolean result = setBalanceCommand.onCommand(senderPlayer, command, "setbalance", new String[]{"Target", "125"});

            assertTrue(result);
            verify(service).execute("Target", 1_2500L);
            verify(messages).sendSetBalanceSuccessReceiver(receiver, "$1.25", "AdminPlayer");
            verify(messages).sendSetBalanceSuccessSender(senderPlayer, "Target", "$1.25");
        }
    }

    @Test
    @DisplayName("should send success message only to sender when receiver uuid is absent")
    void shouldSendSuccessMessageOnlyToSenderWhenReceiverUuidIsAbsent() {
        SetBalanceExecutionResult successWithoutUuid = new SetBalanceExecutionResult(
            io.github.HenriqueMichelini.craftalism.economy.domain.service.enums.SetBalanceStatus.SUCCESS,
            java.util.Optional.of(1_2500L),
            java.util.Optional.empty()
        );
        when(service.execute("Target", 1_2500L)).thenReturn(CompletableFuture.completedFuture(successWithoutUuid));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return task;
            });

            boolean result = setBalanceCommand.onCommand(sender, command, "setbalance", new String[]{"Target", "125"});

            assertTrue(result);
            verify(messages, never()).sendSetBalanceSuccessReceiver(any(), anyString(), anyString());
            verify(messages).sendSetBalanceSuccessSender(sender, "Target", "$1.25");
        }
    }

    @Test
    @DisplayName("should map player not found status to proper message")
    void shouldMapPlayerNotFoundStatusToProperMessage() {
        when(service.execute("Target", 1_2500L)).thenReturn(CompletableFuture.completedFuture(SetBalanceExecutionResult.playerNotFound()));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return task;
            });

            boolean result = setBalanceCommand.onCommand(sender, command, "setbalance", new String[]{"Target", "125"});

            assertTrue(result);
            verify(messages).sendSetBalancePlayerNotFound(sender);
            verify(messages, never()).sendSetBalanceSuccessSender(any(), anyString(), anyString());
        }
    }

    @Test
    @DisplayName("should handle unexpected async failure and notify sender")
    void shouldHandleUnexpectedAsyncFailureAndNotifySender() {
        RuntimeException failure = new RuntimeException("boom");
        when(service.execute("Target", 1_2500L)).thenReturn(CompletableFuture.failedFuture(failure));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
                Runnable runnable = invocation.getArgument(1);
                runnable.run();
                return task;
            });

            boolean result = setBalanceCommand.onCommand(sender, command, "setbalance", new String[]{"Target", "125"});

            assertTrue(result);
            verify(logger).severe(contains("Unexpected error in setbalance command"));
            verify(messages).sendSetBalanceException(sender);
        }
    }
}
