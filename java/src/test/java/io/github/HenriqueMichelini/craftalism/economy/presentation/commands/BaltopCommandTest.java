package io.github.HenriqueMichelini.craftalism.economy.presentation.commands;

import io.github.HenriqueMichelini.craftalism.economy.application.service.BaltopCommandApplicationService;
import io.github.HenriqueMichelini.craftalism.economy.application.service.BaltopCommandApplicationService.BaltopEntry;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.currency.CurrencyFormatter;
import io.github.HenriqueMichelini.craftalism.economy.domain.service.logs.messages.BaltopMessages;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.Mockito.*;

@DisplayName("BaltopCommand Tests")
class  BaltopCommandTest {

    @Mock
    private BaltopMessages messages;
    @Mock
    private BaltopCommandApplicationService service;
    @Mock
    private CurrencyFormatter formatter;

    private BaltopCommand command;

    @Mock
    private Player player;
    @Mock
    private CommandSender consoleSender;
    @Mock
    private Command mockCommand;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        command = new BaltopCommand(messages, service, formatter);
        when(player.hasPermission(anyString())).thenReturn(true);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    @DisplayName("Should display baltop successfully for player")
    void shouldDisplayBaltopSuccessfullyForPlayer() {
        List<BaltopEntry> entries = createBaltopEntries(5);
        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(entries));

        when(formatter.formatCurrency(anyLong()))
                .thenAnswer(inv -> "$" + (inv.getArgument(0, Long.class) / 10000) + ".00");

        boolean result = command.onCommand(player, mockCommand, "baltop", new String[]{});

        assertTrue(result);
        verify(messages).sendBaltopLoading(player);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(messages).sendBaltopHeader(player, "5");
        verify(messages, times(5)).sendBaltopEntry(eq(player), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should display baltop with correct positions")
    void shouldDisplayBaltopWithCorrectPositions() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();

        List<BaltopEntry> entries = List.of(
                new BaltopEntry("FirstPlace", 1000_0000L, uuid1),
                new BaltopEntry("SecondPlace", 500_0000L, uuid2),
                new BaltopEntry("ThirdPlace", 250_0000L, uuid3)
        );

        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(entries));

        when(formatter.formatCurrency(1000_0000L)).thenReturn("$1,000.00");
        when(formatter.formatCurrency(500_0000L)).thenReturn("$500.00");
        when(formatter.formatCurrency(250_0000L)).thenReturn("$250.00");

        boolean result = command.onCommand(player, mockCommand, "baltop", new String[]{});

        assertTrue(result);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(messages).sendBaltopEntry(player, "1", "FirstPlace", "$1,000.00");
        verify(messages).sendBaltopEntry(player, "2", "SecondPlace", "$500.00");
        verify(messages).sendBaltopEntry(player, "3", "ThirdPlace", "$250.00");
    }

    @Test
    @DisplayName("Should format currency correctly for each entry")
    void shouldFormatCurrencyCorrectlyForEachEntry() {
        List<BaltopEntry> entries = List.of(
                new BaltopEntry("Player1", 123_4567L, UUID.randomUUID()),
                new BaltopEntry("Player2", 987_6543L, UUID.randomUUID())
        );

        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(entries));

        when(formatter.formatCurrency(123_4567L)).thenReturn("$12.3456");
        when(formatter.formatCurrency(987_6543L)).thenReturn("$98.7654");

        command.onCommand(player, mockCommand, "baltop", new String[]{});

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(formatter).formatCurrency(123_4567L);
        verify(formatter).formatCurrency(987_6543L);
        verify(messages).sendBaltopEntry(player, "1", "Player1", "$12.3456");
        verify(messages).sendBaltopEntry(player, "2", "Player2", "$98.7654");
    }

    @Test
    @DisplayName("Should display empty baltop correctly")
    void shouldDisplayEmptyBaltopCorrectly() {
        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        boolean result = command.onCommand(player, mockCommand, "baltop", new String[]{});

        assertTrue(result);
        verify(messages).sendBaltopLoading(player);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(messages).sendBaltopHeader(player, "0");
        verify(messages, never()).sendBaltopEntry(eq(player), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should display baltop with 10 entries")
    void shouldDisplayBaltopWith10Entries() {
        List<BaltopEntry> entries = createBaltopEntries(10);
        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(entries));

        when(formatter.formatCurrency(anyLong()))
                .thenReturn("$100.00");

        command.onCommand(player, mockCommand, "baltop", new String[]{});

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(messages).sendBaltopHeader(player, "10");
        verify(messages, times(10)).sendBaltopEntry(eq(player), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should reject command from console")
    void shouldRejectCommandFromConsole() {
        boolean result = command.onCommand(consoleSender, mockCommand, "baltop", new String[]{});

        assertTrue(result);
        verify(messages).sendBaltopPlayerOnly();
        verify(service, never()).getTop10();
        verify(messages, never()).sendBaltopLoading(any());
    }

    @Test
    @DisplayName("Should reject command with arguments")
    void shouldRejectCommandWithArguments() {
        boolean result = command.onCommand(player, mockCommand, "baltop", new String[]{"extra"});

        assertTrue(result);
        verify(messages).sendBaltopUsage(player);
        verify(service, never()).getTop10();
        verify(messages, never()).sendBaltopLoading(player);
    }

    @Test
    @DisplayName("Should reject command with multiple arguments")
    void shouldRejectCommandWithMultipleArguments() {
        boolean result = command.onCommand(player, mockCommand, "baltop", new String[]{"arg1", "arg2"});

        assertTrue(result);
        verify(messages).sendBaltopUsage(player);
        verify(service, never()).getTop10();
    }

    @Test
    @DisplayName("Should accept command with no arguments")
    void shouldAcceptCommandWithNoArguments() {
        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        boolean result = command.onCommand(player, mockCommand, "baltop", new String[]{});

        assertTrue(result);
        verify(messages).sendBaltopLoading(player);
        verify(service).getTop10();
    }

    @Test
    @DisplayName("Should handle service exception gracefully")
    void shouldHandleServiceExceptionGracefully() {
        when(service.getTop10())
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API Error")));

        boolean result = command.onCommand(player, mockCommand, "baltop", new String[]{});

        assertTrue(result);
        verify(messages).sendBaltopLoading(player);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(messages).sendBaltopError(player);
        verify(messages, never()).sendBaltopHeader(any(), anyString());
    }

    @Test
    @DisplayName("Should handle network timeout exception")
    void shouldHandleNetworkTimeoutException() {
        when(service.getTop10())
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Connection timeout")));

        command.onCommand(player, mockCommand, "baltop", new String[]{});

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(messages).sendBaltopError(player);
    }

    @Test
    @DisplayName("Should handle database exception")
    void shouldHandleDatabaseException() {
        when(service.getTop10())
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Database unavailable")));

        command.onCommand(player, mockCommand, "baltop", new String[]{});

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(messages).sendBaltopError(player);
        verify(formatter, never()).formatCurrency(anyLong());
    }

    @Test
    @DisplayName("Should handle baltop with Unknown players")
    void shouldHandleBaltopWithUnknownPlayers() {
        List<BaltopEntry> entries = List.of(
                new BaltopEntry("Unknown", 1000_0000L, UUID.randomUUID()),
                new BaltopEntry("KnownPlayer", 500_0000L, UUID.randomUUID()),
                new BaltopEntry("Unknown", 250_0000L, UUID.randomUUID())
        );

        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(entries));

        when(formatter.formatCurrency(anyLong()))
                .thenReturn("$100.00");

        command.onCommand(player, mockCommand, "baltop", new String[]{});

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(messages).sendBaltopEntry(player, "1", "Unknown", "$100.00");
        verify(messages).sendBaltopEntry(player, "2", "KnownPlayer", "$100.00");
        verify(messages).sendBaltopEntry(player, "3", "Unknown", "$100.00");
    }

    @Test
    @DisplayName("Should handle baltop with zero balances")
    void shouldHandleBaltopWithZeroBalances() {
        List<BaltopEntry> entries = List.of(
                new BaltopEntry("Player1", 1000_0000L, UUID.randomUUID()),
                new BaltopEntry("Player2", 0L, UUID.randomUUID()),
                new BaltopEntry("Player3", 0L, UUID.randomUUID())
        );

        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(entries));

        when(formatter.formatCurrency(1000_0000L)).thenReturn("$1,000.00");
        when(formatter.formatCurrency(0L)).thenReturn("$0.00");

        command.onCommand(player, mockCommand, "baltop", new String[]{});

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(formatter).formatCurrency(1000_0000L);
        verify(formatter, times(2)).formatCurrency(0L);
    }

    @Test
    @DisplayName("Should handle baltop with very large balances")
    void shouldHandleBaltopWithVeryLargeBalances() {
        long hugeBalance = Long.MAX_VALUE / 2;
        List<BaltopEntry> entries = List.of(
                new BaltopEntry("RichPlayer", hugeBalance, UUID.randomUUID())
        );

        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(entries));

        when(formatter.formatCurrency(hugeBalance)).thenReturn("$922,337,203,685,477.00");

        command.onCommand(player, mockCommand, "baltop", new String[]{});

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(formatter).formatCurrency(hugeBalance);
        verify(messages).sendBaltopEntry(player, "1", "RichPlayer", "$922,337,203,685,477.00");
    }

    @Test
    @DisplayName("Should handle players with special characters in names")
    void shouldHandlePlayersWithSpecialCharactersInNames() {
        List<BaltopEntry> entries = List.of(
                new BaltopEntry("Player_123", 1000_0000L, UUID.randomUUID()),
                new BaltopEntry("Test-User", 500_0000L, UUID.randomUUID()),
                new BaltopEntry("Name[VIP]", 250_0000L, UUID.randomUUID())
        );

        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(entries));

        when(formatter.formatCurrency(anyLong()))
                .thenReturn("$100.00");

        command.onCommand(player, mockCommand, "baltop", new String[]{});

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(messages).sendBaltopEntry(player, "1", "Player_123", "$100.00");
        verify(messages).sendBaltopEntry(player, "2", "Test-User", "$100.00");
        verify(messages).sendBaltopEntry(player, "3", "Name[VIP]", "$100.00");
    }

    @Test
    @DisplayName("Should always return true")
    void shouldAlwaysReturnTrue() {
        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        assertTrue(command.onCommand(player, mockCommand, "baltop", new String[]{}));
        assertTrue(command.onCommand(consoleSender, mockCommand, "baltop", new String[]{}));
        assertTrue(command.onCommand(player, mockCommand, "baltop", new String[]{"extra"}));
    }

    @Test
    @DisplayName("Should send loading message before fetching data")
    void shouldSendLoadingMessageBeforeFetchingData() {
        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        command.onCommand(player, mockCommand, "baltop", new String[]{});

        verify(messages).sendBaltopLoading(player);
        verify(service).getTop10();
    }

    @Test
    @DisplayName("Should handle single entry baltop")
    void shouldHandleSingleEntryBaltop() {
        List<BaltopEntry> entries = List.of(
                new BaltopEntry("OnlyPlayer", 100_0000L, UUID.randomUUID())
        );

        when(service.getTop10())
                .thenReturn(CompletableFuture.completedFuture(entries));

        when(formatter.formatCurrency(100_0000L)).thenReturn("$10.00");

        command.onCommand(player, mockCommand, "baltop", new String[]{});

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(messages).sendBaltopHeader(player, "1");
        verify(messages).sendBaltopEntry(player, "1", "OnlyPlayer", "$10.00");
    }

    private List<BaltopEntry> createBaltopEntries(int count) {
        List<BaltopEntry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String playerName = "Player" + (i + 1);
            long balance = (count - i) * 100_0000L;
            UUID uuid = UUID.randomUUID();
            entries.add(new BaltopEntry(playerName, balance, uuid));
        }
        return entries;
    }
}
