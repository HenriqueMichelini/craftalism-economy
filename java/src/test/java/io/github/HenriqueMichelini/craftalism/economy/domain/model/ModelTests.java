package io.github.HenriqueMichelini.craftalism.economy.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ModelTests {

    @Nested
    @DisplayName("Balance Tests")
     class BalanceTest {

        @Test
        @DisplayName("Should create balance with default values")
        void shouldCreateBalanceWithDefaultValues() {
            Balance balance = new Balance();

            assertNotNull(balance);
            assertNull(balance.getUuid());
            assertNull(balance.getAmount());
        }

        @Test
        @DisplayName("Should set and get UUID")
        void shouldSetAndGetUuid() {
            Balance balance = new Balance();
            UUID uuid = UUID.randomUUID();

            balance.setUuid(uuid);

            assertEquals(uuid, balance.getUuid());
        }

        @Test
        @DisplayName("Should set and get amount")
        void shouldSetAndGetAmount() {
            Balance balance = new Balance();

            balance.setAmount(1000000L);

            assertEquals(1000000L, balance.getAmount());
        }

        @Test
        @DisplayName("Should allow setting zero amount")
        void shouldAllowSettingZeroAmount() {
            Balance balance = new Balance();

            balance.setAmount(0L);

            assertEquals(0L, balance.getAmount());
        }

        @Test
        @DisplayName("Should allow negative amounts")
        void shouldAllowNegativeAmounts() {
            Balance balance = new Balance();

            balance.setAmount(-500L);

            assertEquals(-500L, balance.getAmount());
        }

        @Test
        @DisplayName("Should handle maximum long value")
        void shouldHandleMaximumLongValue() {
            Balance balance = new Balance();

            balance.setAmount(Long.MAX_VALUE);

            assertEquals(Long.MAX_VALUE, balance.getAmount());
        }

        @Test
        @DisplayName("Should handle minimum long value")
        void shouldHandleMinimumLongValue() {
            Balance balance = new Balance();

            balance.setAmount(Long.MIN_VALUE);

            assertEquals(Long.MIN_VALUE, balance.getAmount());
        }

        @Test
        @DisplayName("Should allow updating amount multiple times")
        void shouldAllowUpdatingAmountMultipleTimes() {
            Balance balance = new Balance();

            balance.setAmount(1000L);
            assertEquals(1000L, balance.getAmount());

            balance.setAmount(2000L);
            assertEquals(2000L, balance.getAmount());

            balance.setAmount(3000L);
            assertEquals(3000L, balance.getAmount());
        }

        @Test
        @DisplayName("Should handle UUID changes")
        void shouldHandleUuidChanges() {
            Balance balance = new Balance();
            UUID uuid1 = UUID.randomUUID();
            UUID uuid2 = UUID.randomUUID();

            balance.setUuid(uuid1);
            assertEquals(uuid1, balance.getUuid());

            balance.setUuid(uuid2);
            assertEquals(uuid2, balance.getUuid());
        }
    }

    @Nested
    @DisplayName("Player Tests")
    class PlayerTest {

        @Test
        @DisplayName("Should create player with constructor")
        void shouldCreatePlayerWithConstructor() {
            UUID uuid = UUID.randomUUID();
            String name = "TestPlayer";
            Instant createdAt = Instant.now();

            Player player = new Player(uuid, name, createdAt);

            assertNotNull(player);
            assertEquals(uuid, player.getUuid());
            assertEquals(name, player.getName());
            assertEquals(createdAt, player.getCreatedAt());
        }

        @Test
        @DisplayName("Should set and get UUID")
        void shouldSetAndGetUuid() {
            Player player = new Player(UUID.randomUUID(), "Test", Instant.now());
            UUID newUuid = UUID.randomUUID();

            player.setUuid(newUuid);

            assertEquals(newUuid, player.getUuid());
        }

        @Test
        @DisplayName("Should set and get name")
        void shouldSetAndGetName() {
            Player player = new Player(UUID.randomUUID(), "OldName", Instant.now());

            player.setName("NewName");

            assertEquals("NewName", player.getName());
        }

        @Test
        @DisplayName("Should set and get createdAt")
        void shouldSetAndGetCreatedAt() {
            Player player = new Player(UUID.randomUUID(), "Test", Instant.now());
            Instant newTime = Instant.now().plusSeconds(3600);

            player.setCreatedAt(newTime);

            assertEquals(newTime, player.getCreatedAt());
        }

        @Test
        @DisplayName("Should handle empty player name")
        void shouldHandleEmptyPlayerName() {
            Player player = new Player(UUID.randomUUID(), "", Instant.now());

            assertEquals("", player.getName());
        }

        @Test
        @DisplayName("Should handle long player names")
        void shouldHandleLongPlayerNames() {
            String longName = "A".repeat(100);
            Player player = new Player(UUID.randomUUID(), longName, Instant.now());

            assertEquals(longName, player.getName());
        }

        @Test
        @DisplayName("Should handle special characters in name")
        void shouldHandleSpecialCharactersInName() {
            String specialName = "Test_Player-123!@#";
            Player player = new Player(UUID.randomUUID(), specialName, Instant.now());

            assertEquals(specialName, player.getName());
        }

        @Test
        @DisplayName("Should handle past creation timestamps")
        void shouldHandlePastCreationTimestamps() {
            Instant pastTime = Instant.parse("2020-01-01T00:00:00Z");
            Player player = new Player(UUID.randomUUID(), "Test", pastTime);

            assertEquals(pastTime, player.getCreatedAt());
        }

        @Test
        @DisplayName("Should handle future creation timestamps")
        void shouldHandleFutureCreationTimestamps() {
            Instant futureTime = Instant.parse("2030-01-01T00:00:00Z");
            Player player = new Player(UUID.randomUUID(), "Test", futureTime);

            assertEquals(futureTime, player.getCreatedAt());
        }

        @Test
        @DisplayName("Should allow null values in constructor")
        void shouldAllowNullValuesInConstructor() {
            Player player = new Player(null, null, null);

            assertNull(player.getUuid());
            assertNull(player.getName());
            assertNull(player.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("Transaction Tests")
    class TransactionTest {

        @Test
        @DisplayName("Should create transaction with default values")
        void shouldCreateTransactionWithDefaultValues() {
            Transaction transaction = new Transaction();

            assertNotNull(transaction);
            assertNull(transaction.getId());
            assertNull(transaction.getFromUuid());
            assertNull(transaction.getToUuid());
            assertNull(transaction.getAmount());
            assertNull(transaction.getCreatedAt());
        }

        @Test
        @DisplayName("Should set and get ID")
        void shouldSetAndGetId() {
            Transaction transaction = new Transaction();

            transaction.setId(12345L);

            assertEquals(12345L, transaction.getId());
        }

        @Test
        @DisplayName("Should set and get fromUuid")
        void shouldSetAndGetFromUuid() {
            Transaction transaction = new Transaction();
            UUID fromUuid = UUID.randomUUID();

            transaction.setFromUuid(fromUuid);

            assertEquals(fromUuid, transaction.getFromUuid());
        }

        @Test
        @DisplayName("Should set and get toUuid")
        void shouldSetAndGetToUuid() {
            Transaction transaction = new Transaction();
            UUID toUuid = UUID.randomUUID();

            transaction.setToUuid(toUuid);

            assertEquals(toUuid, transaction.getToUuid());
        }

        @Test
        @DisplayName("Should set and get amount")
        void shouldSetAndGetAmount() {
            Transaction transaction = new Transaction();

            transaction.setAmount(5000000L);

            assertEquals(5000000L, transaction.getAmount());
        }

        @Test
        @DisplayName("Should set and get createdAt")
        void shouldSetAndGetCreatedAt() {
            Transaction transaction = new Transaction();
            Instant now = Instant.now();

            transaction.setCreatedAt(now);

            assertEquals(now, transaction.getCreatedAt());
        }

        @Test
        @DisplayName("Should handle complete transaction data")
        void shouldHandleCompleteTransactionData() {
            Transaction transaction = new Transaction();
            Long id = 999L;
            UUID fromUuid = UUID.randomUUID();
            UUID toUuid = UUID.randomUUID();
            Long amount = 1000000L;
            Instant createdAt = Instant.now();

            transaction.setId(id);
            transaction.setFromUuid(fromUuid);
            transaction.setToUuid(toUuid);
            transaction.setAmount(amount);
            transaction.setCreatedAt(createdAt);

            assertEquals(id, transaction.getId());
            assertEquals(fromUuid, transaction.getFromUuid());
            assertEquals(toUuid, transaction.getToUuid());
            assertEquals(amount, transaction.getAmount());
            assertEquals(createdAt, transaction.getCreatedAt());
        }

        @Test
        @DisplayName("Should allow zero amount transactions")
        void shouldAllowZeroAmountTransactions() {
            Transaction transaction = new Transaction();

            transaction.setAmount(0L);

            assertEquals(0L, transaction.getAmount());
        }

        @Test
        @DisplayName("Should allow negative amount transactions")
        void shouldAllowNegativeAmountTransactions() {
            Transaction transaction = new Transaction();

            transaction.setAmount(-1000L);

            assertEquals(-1000L, transaction.getAmount());
        }

        @Test
        @DisplayName("Should handle same sender and receiver")
        void shouldHandleSameSenderAndReceiver() {
            Transaction transaction = new Transaction();
            UUID sameUuid = UUID.randomUUID();

            transaction.setFromUuid(sameUuid);
            transaction.setToUuid(sameUuid);

            assertEquals(sameUuid, transaction.getFromUuid());
            assertEquals(sameUuid, transaction.getToUuid());
        }

        @Test
        @DisplayName("Should handle large transaction IDs")
        void shouldHandleLargeTransactionIds() {
            Transaction transaction = new Transaction();

            transaction.setId(Long.MAX_VALUE);

            assertEquals(Long.MAX_VALUE, transaction.getId());
        }

        @Test
        @DisplayName("Should handle maximum amount value")
        void shouldHandleMaximumAmountValue() {
            Transaction transaction = new Transaction();

            transaction.setAmount(Long.MAX_VALUE);

            assertEquals(Long.MAX_VALUE, transaction.getAmount());
        }

        @Test
        @DisplayName("Should allow updating transaction fields")
        void shouldAllowUpdatingTransactionFields() {
            Transaction transaction = new Transaction();

            transaction.setAmount(1000L);
            assertEquals(1000L, transaction.getAmount());

            transaction.setAmount(2000L);
            assertEquals(2000L, transaction.getAmount());
        }

        @Test
        @DisplayName("Should handle epoch timestamps")
        void shouldHandleEpochTimestamps() {
            Transaction transaction = new Transaction();
            Instant epoch = Instant.EPOCH;

            transaction.setCreatedAt(epoch);

            assertEquals(epoch, transaction.getCreatedAt());
        }
    }
}