package io.github.HenriqueMichelini.craftalism.economy.domain.service;

import io.github.HenriqueMichelini.craftalism.economy.domain.model.Balance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

@DisplayName("FundsTransfer Tests")
class FundsTransferTest {

    private FundsTransfer fundsTransfer;
    private Balance fromBalance;
    private Balance toBalance;

    @BeforeEach
    void setUp() {
        fundsTransfer = new FundsTransfer();

        fromBalance = new Balance();
        fromBalance.setUuid(UUID.randomUUID());
        fromBalance.setAmount(10000L);

        toBalance = new Balance();
        toBalance.setUuid(UUID.randomUUID());
        toBalance.setAmount(5000L);
    }

    @Test
    @DisplayName("Should transfer funds correctly between balances")
    void shouldTransferFundsCorrectly() {
        long transferAmount = 3000L;
        long expectedFromAmount = 7000L;
        long expectedToAmount = 8000L;

        fundsTransfer.transfer(fromBalance, toBalance, transferAmount);

        assertEquals(expectedFromAmount, fromBalance.getAmount(),
                "From balance should be reduced by transfer amount");
        assertEquals(expectedToAmount, toBalance.getAmount(),
                "To balance should be increased by transfer amount");
    }

    @Test
    @DisplayName("Should handle transfer of entire balance")
    void shouldTransferEntireBalance() {
        long transferAmount = 10000L;

        fundsTransfer.transfer(fromBalance, toBalance, transferAmount);

        assertEquals(0L, fromBalance.getAmount(),
                "From balance should be zero after transferring entire amount");
        assertEquals(15000L, toBalance.getAmount(),
                "To balance should receive full transfer");
    }

    @Test
    @DisplayName("Should handle small amount transfers")
    void shouldHandleSmallTransfers() {
        long transferAmount = 1L;

        fundsTransfer.transfer(fromBalance, toBalance, transferAmount);

        assertEquals(9999L, fromBalance.getAmount());
        assertEquals(5001L, toBalance.getAmount());
    }

    @Test
    @DisplayName("Should handle large amount transfers")
    void shouldHandleLargeTransfers() {
        fromBalance.setAmount(Long.MAX_VALUE / 2);
        toBalance.setAmount(0L);
        long transferAmount = 1000000000L;

        fundsTransfer.transfer(fromBalance, toBalance, transferAmount);

        assertEquals((Long.MAX_VALUE / 2) - transferAmount, fromBalance.getAmount());
        assertEquals(transferAmount, toBalance.getAmount());
    }

    @Test
    @DisplayName("Should handle transfer to balance with zero amount")
    void shouldTransferToZeroBalance() {
        toBalance.setAmount(0L);
        long transferAmount = 5000L;

        fundsTransfer.transfer(fromBalance, toBalance, transferAmount);

        assertEquals(5000L, fromBalance.getAmount());
        assertEquals(5000L, toBalance.getAmount());
    }

    @Test
    @DisplayName("Should handle transfer from balance after multiple operations")
    void shouldHandleMultipleTransfers() {
        long firstTransfer = 2000L;
        long secondTransfer = 3000L;

        fundsTransfer.transfer(fromBalance, toBalance, firstTransfer);
        fundsTransfer.transfer(fromBalance, toBalance, secondTransfer);

        assertEquals(5000L, fromBalance.getAmount());
        assertEquals(10000L, toBalance.getAmount());
    }

    @Test
    @DisplayName("Should allow negative balance after transfer (overdraft)")
    void shouldAllowNegativeBalance() {
        fromBalance.setAmount(100L);
        long transferAmount = 200L;

        fundsTransfer.transfer(fromBalance, toBalance, transferAmount);

        assertEquals(-100L, fromBalance.getAmount(),
                "Method allows overdraft - validation should be done elsewhere");
        assertEquals(5200L, toBalance.getAmount());
    }

    @Test
    @DisplayName("Should handle transfer between same balance object")
    void shouldHandleSelfTransfer() {
        long transferAmount = 1000L;
        long originalAmount = fromBalance.getAmount();

        fundsTransfer.transfer(fromBalance, fromBalance, transferAmount);

        assertEquals(originalAmount, fromBalance.getAmount(),
                "Self transfer should result in no net change (subtract then add same amount)");
    }
}