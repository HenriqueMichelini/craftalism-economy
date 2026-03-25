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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@DisplayName("SetBalanceCommand tests")
class SetBalanceCommandTest {

    @Mock
    private SetBalanceMessages messages;
    @Mock
    private SetBalanceCommandApplicationService service;
    @Mock
    private PlayerNameCheck playerNameCheck;
    @Mock
    private JavaPlugin plugin;
    @Mock
    private Logger logger;

    @Mock
    private CommandSender sender;
    @Mock
    private Player senderPlayer;
    @Mock
    private Player targetPlayer;
    @Mock
    private Command command;
    @Mock
    private BukkitScheduler scheduler;

    private SetBalanceCommand setBalanceCommand;
    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        setBalanceCommand = new SetBalanceCommand(playerNameCheck, messages, service, plugin);

        when(sender.hasPermission(anyString())).thenReturn(true);
        when(senderPlayer.hasPermission(anyString())).thenReturn(true);
        when(senderPlayer.getName()).thenReturn("Admin");
        when(playerNameCheck.isValid(anyString())).thenReturn(true);
        when(plugin.getLogger()).thenReturn(logger);

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
    void shouldSetBalanceSuccessfully() {
        UUID targetUuid = UUID.randomUUID();
        when(service.execute("Target", 100L))
                .thenReturn(CompletableFuture.completedFuture(SetBalanceExecutionResult.success(100L, targetUuid)));
        when(targetPlayer.isOnline()).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getPlayer(targetUuid)).thenReturn(targetPlayer);

            boolean result = setBalanceCommand.onCommand(sender, command, "setbalance", new String[]{"Target", "100"});

            assertTrue(result);
            verify(messages).sendSetBalanceSuccessSender(sender, "Target", "100");
            verify(messages).sendSetBalanceSuccessReceiver(targetPlayer, "100", "Console");
        }
    }

    @Test
    void shouldSetBalanceUsingPlayerSenderName() {
        UUID targetUuid = UUID.randomUUID();
        when(service.execute("Target", 200L))
                .thenReturn(CompletableFuture.completedFuture(SetBalanceExecutionResult.success(200L, targetUuid)));
        when(targetPlayer.isOnline()).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getPlayer(targetUuid)).thenReturn(targetPlayer);

            setBalanceCommand.onCommand(senderPlayer, command, "setbalance", new String[]{"Target", "200"});

            verify(messages).sendSetBalanceSuccessReceiver(targetPlayer, "200", "Admin");
        }
    }

    @Test
    void shouldRejectDecimalAmount() {
        boolean result = setBalanceCommand.onCommand(sender, command, "setbalance", new String[]{"Target", "10.50"});

        assertTrue(result);
        verify(messages).sendSetBalanceInvalidAmount(sender);
        verifyNoInteractions(service);
    }

    @Test
    void shouldHandlePlayerNotFound() {
        when(service.execute("Ghost", 100L))
                .thenReturn(CompletableFuture.completedFuture(SetBalanceExecutionResult.playerNotFound()));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

            setBalanceCommand.onCommand(sender, command, "setbalance", new String[]{"Ghost", "100"});

            verify(messages).sendSetBalancePlayerNotFound(sender);
        }
    }

    @Test
    void shouldHandleUpdateFailed() {
        when(service.execute("Target", 300L))
                .thenReturn(CompletableFuture.completedFuture(SetBalanceExecutionResult.updateFailed()));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

            setBalanceCommand.onCommand(sender, command, "setbalance", new String[]{"Target", "300"});

            verify(messages).sendSetBalanceUpdateFailed(sender);
        }
    }

    @Test
    void shouldHandleGeneralExceptionResult() {
        when(service.execute("Target", 400L))
                .thenReturn(CompletableFuture.completedFuture(SetBalanceExecutionResult.exception()));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);

            setBalanceCommand.onCommand(sender, command, "setbalance", new String[]{"Target", "400"});

            verify(messages).sendSetBalanceException(sender);
        }
    }

    @Test
    void shouldParseLeadingZeros() {
        UUID targetUuid = UUID.randomUUID();
        when(service.execute("Target", 100L))
                .thenReturn(CompletableFuture.completedFuture(SetBalanceExecutionResult.success(100L, targetUuid)));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getPlayer(targetUuid)).thenReturn(null);

            setBalanceCommand.onCommand(sender, command, "setbalance", new String[]{"Target", "00100"});

            verify(service).execute("Target", 100L);
            verify(messages).sendSetBalanceSuccessSender(sender, "Target", "100");
        }
    }

    @Test
    void shouldHandleNullTargetPlayer() {
        UUID targetUuid = UUID.randomUUID();
        when(service.execute("Target", 100L))
                .thenReturn(CompletableFuture.completedFuture(SetBalanceExecutionResult.success(100L, targetUuid)));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getPlayer(targetUuid)).thenReturn(null);

            setBalanceCommand.onCommand(sender, command, "setbalance", new String[]{"Target", "100"});

            verify(messages, never()).sendSetBalanceSuccessReceiver(any(), any(), any());
            verify(messages).sendSetBalanceSuccessSender(sender, "Target", "100");
        }
    }
}
