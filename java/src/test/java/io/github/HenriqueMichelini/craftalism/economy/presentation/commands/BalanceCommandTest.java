package io.github.HenriqueMichelini.craftalism.economy.presentation.commands;

import io.github.HenriqueMichelini.craftalism.economy.application.dto.BalanceExecutionResult;
import io.github.HenriqueMichelini.craftalism.economy.application.service.BalanceCommandApplicationService;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages.BalanceMessages;
import io.github.HenriqueMichelini.craftalism.economy.presentation.validation.PlayerNameCheck;
import org.bukkit.command.Command;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BalanceCommandTest {
    @Mock private BalanceMessages messages;
    @Mock private PlayerNameCheck playerNameCheck;
    @Mock private BalanceCommandApplicationService balanceService;
    @Mock private CurrencyFormatter formatter;
    @Mock private Player player;
    @Mock private ConsoleCommandSender console;
    @Mock private Command command;

    private BalanceCommand balanceCommand;
    private final UUID playerUuid = UUID.randomUUID();

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        balanceCommand = new BalanceCommand(messages, playerNameCheck, balanceService, formatter);
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(player.getName()).thenReturn("TestPlayer");

        when(player.hasPermission(anyString())).thenReturn(true);
    }


    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void shouldShowSelfBalance_WhenPlayerExecutesWithoutArgs() {
        Long balance = 1005000L;
        BalanceExecutionResult result = BalanceExecutionResult.successSelf(balance);
        when(balanceService.executeSelf(playerUuid))
                .thenReturn(CompletableFuture.completedFuture(result));

        boolean commandResult = balanceCommand.onCommand(player, command, "balance", new String[]{});

        assertTrue(commandResult);
        verify(balanceService).executeSelf(playerUuid);
        verify(messages, timeout(100)).sendBalanceSelfSuccess(player, String.valueOf(balance));
    }

    @Test
    void shouldShowError_WhenSelfBalanceFails() {
        BalanceExecutionResult result = BalanceExecutionResult.error();
        when(balanceService.executeSelf(playerUuid))
                .thenReturn(CompletableFuture.completedFuture(result));

        balanceCommand.onCommand(player, command, "balance", new String[]{});

        verify(balanceService).executeSelf(playerUuid);
        verify(messages, timeout(100)).sendBalanceError(player);
    }

    @Test
    void shouldDenyConsole_WhenExecutingWithoutArgs() {
        boolean result = balanceCommand.onCommand(console, command, "balance", new String[]{});

        assertTrue(result);
        verify(messages).sendBalancePlayerOnly();
        verifyNoInteractions(balanceService);
    }

    @Test
    void shouldShowOtherBalance_WhenValidPlayerName() {
        String targetName = "OtherPlayer";
        Long balance = 2507500L;
        BalanceExecutionResult result = BalanceExecutionResult.successOther(balance);

        when(playerNameCheck.isValid(targetName)).thenReturn(true);
        when(balanceService.executeOther(targetName))
                .thenReturn(CompletableFuture.completedFuture(result));

        boolean commandResult = balanceCommand.onCommand(player, command, "balance", new String[]{targetName});

        assertTrue(commandResult);
        verify(balanceService).executeOther(targetName);
        verify(messages, timeout(100)).sendBalanceOtherSuccess(player, targetName, String.valueOf(balance));
    }

    @Test
    void shouldShowError_WhenInvalidPlayerName() {
        String invalidName = "Invalid@Name!";
        when(playerNameCheck.isValid(invalidName)).thenReturn(false);

        boolean result = balanceCommand.onCommand(player, command, "balance", new String[]{invalidName});

        assertTrue(result);
        verify(messages).sendBalanceInvalidName(player);
        verifyNoInteractions(balanceService);
    }

    @Test
    void shouldShowNotFound_WhenPlayerDoesNotExist() {
        String targetName = "UnknownPlayer";
        BalanceExecutionResult result = BalanceExecutionResult.notFound();

        when(playerNameCheck.isValid(targetName)).thenReturn(true);
        when(balanceService.executeOther(targetName))
                .thenReturn(CompletableFuture.completedFuture(result));

        balanceCommand.onCommand(player, command, "balance", new String[]{targetName});

        verify(messages, timeout(100)).sendBalanceOtherNotFound(player, targetName);
    }

    @Test
    void shouldShowNoBalance_WhenPlayerHasNoBalance() {
        String targetName = "PlayerWithoutBalance";
        BalanceExecutionResult result = BalanceExecutionResult.noBalance();

        when(playerNameCheck.isValid(targetName)).thenReturn(true);
        when(balanceService.executeOther(targetName))
                .thenReturn(CompletableFuture.completedFuture(result));

        balanceCommand.onCommand(player, command, "balance", new String[]{targetName});

        verify(messages, timeout(100)).sendBalanceOtherNoBalance(player, targetName);
    }

    @Test
    void shouldDenyConsole_WhenCheckingOtherBalance() {
        String targetName = "SomePlayer";

        boolean result = balanceCommand.onCommand(console, command, "balance", new String[]{targetName});

        assertTrue(result);
        verify(messages).sendBalancePlayerOnly();
        verifyNoInteractions(balanceService);
    }
}