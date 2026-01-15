package io.github.HenriqueMichelini.craftalism.economy.application.dto;

import io.github.HenriqueMichelini.craftalism.economy.domain.service.enums.BalanceStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BalanceExecutionResultTest {

    @Test
    void successSelf_ShouldCreateCorrectResult() {
        Long amount = 1000000L;
        BalanceExecutionResult result = BalanceExecutionResult.successSelf(amount);

        assertEquals(BalanceStatus.SUCCESS_SELF, result.status());
        assertEquals(amount, result.amount());
    }

    @Test
    void successOther_ShouldCreateCorrectResult() {
        Long amount = 2000000L;
        BalanceExecutionResult result = BalanceExecutionResult.successOther(amount);

        assertEquals(BalanceStatus.SUCCESS_OTHER, result.status());
        assertEquals(amount, result.amount());
    }

    @Test
    void noBalance_ShouldCreateCorrectResult() {
        BalanceExecutionResult result = BalanceExecutionResult.noBalance();

        assertEquals(BalanceStatus.NO_BALANCE, result.status());
        assertEquals(0L, result.amount());
    }

    @Test
    void notFound_ShouldCreateCorrectResult() {
        BalanceExecutionResult result = BalanceExecutionResult.notFound();

        assertEquals(BalanceStatus.NOT_FOUND, result.status());
        assertEquals(0L, result.amount());
    }

    @Test
    void error_ShouldCreateCorrectResult() {
        BalanceExecutionResult result = BalanceExecutionResult.error();

        assertEquals(BalanceStatus.ERROR, result.status());
        assertEquals(0L, result.amount());
    }
}