package io.github.HenriqueMichelini.craftalism.economy.presentation.commands;

import io.github.HenriqueMichelini.craftalism.economy.application.dto.SetBalanceExecutionResult;
import io.github.HenriqueMichelini.craftalism.economy.application.service.SetBalanceCommandApplicationService;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages.SetBalanceMessages;
import io.github.HenriqueMichelini.craftalism.economy.presentation.validation.PlayerNameCheck;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("SetBalanceCommand Tests")
class SetBalanceCommandTest {

    @Mock
    private SetBalanceMessages messages;
    @Mock
    private SetBalanceCommandApplicationService service;
    @Mock
    private PlayerNameCheck playerNameCheck;
    @Mock
    private JavaPlugin plugin;

    private SetBalanceCommand command;

    @Mock
    private CommandSender sender;
    @Mock
    private Player senderPlayer;
    @Mock
    private Player targetPlayer;
    @Mock
    private Command mockCommand;
    @Mock
    private BukkitScheduler scheduler;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        command = new SetBalanceCommand(playerNameCheck, messages, service, plugin);

        when(senderPlayer.getName()).thenReturn("Admin");
        when(sender.getName()).thenReturn("Console");

        when(playerNameCheck.isValid(anyString())).thenReturn(true);

        when(senderPlayer.hasPermission(anyString())).thenReturn(true);

        // <-- ADD THIS: make the (console) sender have permissions by default
        when(sender.hasPermission(anyString())).thenReturn(true);

        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            task.run();
            return null;
        }).when(scheduler).runTask(eq(plugin), any(Runnable.class));
    }


    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    @DisplayName("Should set balance successfully")
    void shouldSetBalanceSuccessfully() {
        String targetName = "TestPlayer";
        String amountStr = "10000000";
        long amount = 10000000L;
        UUID targetUuid = UUID.randomUUID();

        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.success(amount, targetUuid)));

        when(targetPlayer.isOnline()).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getPlayer(targetUuid)).thenReturn(targetPlayer);

            boolean result = command.onCommand(sender, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            assertTrue(result);
            verify(service).execute(targetName, amount);
            verify(messages).sendSetBalanceSuccessSender(sender, targetName, amountStr);
            verify(messages).sendSetBalanceSuccessReceiver(targetPlayer, amountStr, "Console");
        }
    }

    @Test
    @DisplayName("Should send message with player sender name")
    void shouldSendMessageWithPlayerSenderName() {
        String targetName = "TargetPlayer";
        String amountStr = "5000000";
        long amount = 5000000L;
        UUID targetUuid = UUID.randomUUID();

        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.success(amount, targetUuid)));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getPlayer(targetUuid)).thenReturn(targetPlayer);

            command.onCommand(senderPlayer, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            verify(messages, never()).sendSetBalanceSuccessReceiver(any(), any(), any());
            verify(messages).sendSetBalanceSuccessSender(senderPlayer, "TargetPlayer", "5000000");
        }
    }

    @Test
    @DisplayName("Should handle null target player")
    void shouldHandleNullTargetPlayer() {
        String targetName = "OfflinePlayer";
        String amountStr = "1000000";
        long amount = 1000000L;
        UUID targetUuid = UUID.randomUUID();

        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.success(amount, targetUuid)));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getPlayer(targetUuid)).thenReturn(null);

            boolean result = command.onCommand(sender, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            assertTrue(result);
            verify(messages, never()).sendSetBalanceSuccessReceiver(any(), any(), any());
            verify(messages).sendSetBalanceSuccessSender(sender, "OfflinePlayer", "1000000");
        }
    }

    @Test
    @DisplayName("Should reject command with no arguments")
    void shouldRejectCommandWithNoArguments() {
        boolean result = command.onCommand(sender, mockCommand, "setbalance", new String[]{});

        assertTrue(result);
        verify(messages).sendSetBalanceUsage(sender);
        verify(service, never()).execute(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should reject command with one argument")
    void shouldRejectCommandWithOneArgument() {
        boolean result = command.onCommand(sender, mockCommand, "setbalance", new String[]{"Player"});

        assertTrue(result);
        verify(messages).sendSetBalanceUsage(sender);
        verify(service, never()).execute(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should reject command with three arguments")
    void shouldRejectCommandWithThreeArguments() {
        boolean result = command.onCommand(sender, mockCommand, "setbalance",
                new String[]{"Player", "100", "extra"});

        assertTrue(result);
        verify(messages).sendSetBalanceUsage(sender);
        verify(service, never()).execute(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should reject invalid player name")
    void shouldRejectInvalidPlayerName() {
        String invalidName = "Invalid@Name";
        when(playerNameCheck.isValid(invalidName)).thenReturn(false);

        boolean result = command.onCommand(sender, mockCommand, "setbalance",
                new String[]{invalidName, "100"});

        assertTrue(result);
        verify(messages).sendSetBalanceInvalidName(sender);
        verify(service, never()).execute(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should reject non-numeric amount")
    void shouldRejectNonNumericAmount() {
        String targetName = "Player";
        String invalidAmount = "abc";

        boolean result = command.onCommand(sender, mockCommand, "setbalance",
                new String[]{targetName, invalidAmount});

        assertTrue(result);
        verify(messages).sendSetBalanceInvalidAmount(sender);
        verify(service, never()).execute(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should reject decimal amount")
    void shouldRejectDecimalAmount() {
        String targetName = "Player";
        String decimalAmount = "10.50";

        boolean result = command.onCommand(sender, mockCommand, "setbalance",
                new String[]{targetName, decimalAmount});

        assertTrue(result);
        verify(messages).sendSetBalanceInvalidAmount(sender);
        verify(service, never()).execute(anyString(), anyLong());
    }

    @Test
    @DisplayName("Should handle player not found error")
    void shouldHandlePlayerNotFoundError() {
        String targetName = "NonExistent";
        String amountStr = "1000000";
        long amount = 1000000L;

        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.playerNotFound()));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

            command.onCommand(sender, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            verify(messages).sendSetBalancePlayerNotFound(sender);
            verify(messages, never()).sendSetBalanceSuccessSender(any(), anyString(), anyString());
        }
    }

    @Test
    @DisplayName("Should handle invalid amount from service")
    void shouldHandleInvalidAmountFromService() {
        String targetName = "Player";
        String amountStr = "-1";
        long amount = -1L;

        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.invalidAmount()));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

            command.onCommand(sender, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            verify(messages).sendSetBalanceInvalidAmount(sender);
        }
    }

    @Test
    @DisplayName("Should handle update failed error")
    void shouldHandleUpdateFailedError() {
        String targetName = "Player";
        String amountStr = "1000000";
        long amount = 1000000L;

        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.updateFailed()));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

            command.onCommand(sender, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            verify(messages).sendSetBalanceUpdateFailed(sender);
        }
    }

    @Test
    @DisplayName("Should handle general exception")
    void shouldHandleGeneralException() {
        String targetName = "Player";
        String amountStr = "1000000";
        long amount = 1000000L;

        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.exception()));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

            command.onCommand(sender, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            verify(messages).sendSetBalanceException(sender);
        }
    }

    @Test
    @DisplayName("Should handle zero amount")
    void shouldHandleZeroAmount() {
        String targetName = "Player";
        String amountStr = "0";
        long amount = 0L;
        UUID targetUuid = UUID.randomUUID();

        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.success(amount, targetUuid)));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getPlayer(targetUuid)).thenReturn(targetPlayer);

            boolean result = command.onCommand(sender, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            assertTrue(result);
            verify(service).execute(targetName, amount);
        }
    }

    @Test
    @DisplayName("Should handle very large amounts")
    void shouldHandleVeryLargeAmounts() {
        String targetName = "Player";
        String amountStr = String.valueOf(Long.MAX_VALUE);
        long amount = Long.MAX_VALUE;
        UUID targetUuid = UUID.randomUUID();

        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.success(amount, targetUuid)));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getPlayer(targetUuid)).thenReturn(targetPlayer);

            command.onCommand(sender, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            verify(service).execute(targetName, amount);
        }
    }

    @Test
    @DisplayName("Should handle negative amounts passed to service")
    void shouldHandleNegativeAmountsPassedToService() {
        String targetName = "Player";
        String amountStr = "-100";
        long amount = -100L;

        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.invalidAmount()));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

            command.onCommand(sender, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            verify(service, never()).execute(any(), anyLong());
            verify(messages).sendSetBalanceInvalidAmount(sender);
        }
    }

    @Test
    @DisplayName("Should always return true")
    void shouldAlwaysReturnTrue() {
        assertTrue(command.onCommand(sender, mockCommand, "setbalance", new String[]{}));
        assertTrue(command.onCommand(sender, mockCommand, "setbalance", new String[]{"Player"}));

        when(playerNameCheck.isValid("")).thenReturn(false);
        assertTrue(command.onCommand(sender, mockCommand, "setbalance", new String[]{"", "100"}));
    }

    @Test
    @DisplayName("Should handle amount with leading zeros")
    void shouldHandleAmountWithLeadingZeros() {
        String targetName = "Player";
        String amountStr = "00100";
        long amount = 100L;
        UUID targetUuid = UUID.randomUUID();

        when(service.execute(targetName, amount))
                .thenReturn(CompletableFuture.completedFuture(
                        SetBalanceExecutionResult.success(amount, targetUuid)));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getPlayer(targetUuid)).thenReturn(targetPlayer);

            command.onCommand(sender, mockCommand, "setbalance",
                    new String[]{targetName, amountStr});

            verify(service).execute(targetName, amount);
        }
    }

    @Test
    @DisplayName("Should not call service when validation fails")
    void shouldNotCallServiceWhenValidationFails() {
        command.onCommand(sender, mockCommand, "setbalance", new String[]{});
        verify(service, never()).execute(anyString(), anyLong());

        when(playerNameCheck.isValid("Bad")).thenReturn(false);
        command.onCommand(sender, mockCommand, "setbalance", new String[]{"Bad", "100"});
        verify(service, never()).execute(anyString(), anyLong());

        command.onCommand(sender, mockCommand, "setbalance", new String[]{"Player", "abc"});
        verify(service, never()).execute(anyString(), anyLong());
    }
}