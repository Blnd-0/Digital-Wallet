package com.wallet.service;

import com.wallet.domain.TransactionStatus;
import com.wallet.domain.TransactionType;
import com.wallet.domain.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.util.UUID;

public class TransactionServiceTest {

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService();
    }

    @Test
    void shouldSaveTransactionSuccessfully() {
        var transaction = Transaction.createDeposit(UUID.randomUUID(), new BigDecimal("500"), TransactionStatus.SUCCESS).get();
        var result = transactionService.saveTransaction(transaction);
        assertThat(result.isRight()).isTrue();
    }

    @Test
    void shouldFailSaveNullTransaction() {
        var result = transactionService.saveTransaction(null);
        assertThat(result.isLeft()).isTrue();
    }

    @Test
    void shouldGetTransactionById() {
        var transaction = Transaction.createDeposit(UUID.randomUUID(), new BigDecimal("500"), TransactionStatus.SUCCESS).get();
        transactionService.saveTransaction(transaction);
        var result = transactionService.getTransactionById(transaction.getTransactionId());
        assertThat(result.isRight()).isTrue();
    }

    @Test
    void shouldFailGetNonExistentTransaction() {
        var result = transactionService.getTransactionById(UUID.randomUUID());
        assertThat(result.isLeft()).isTrue();
    }

    @Test
    void shouldGetTransactionHistory() {
        UUID walletId = UUID.randomUUID();
        var transaction = Transaction.createDeposit(walletId, new BigDecimal("500"), TransactionStatus.SUCCESS).get();
        transactionService.saveTransaction(transaction);
        var history = transactionService.getTransactionHistory(walletId);
        assertThat(history.size()).isEqualTo(1);
        assertThat(history.head().getType()).isEqualTo(TransactionType.DEPOSIT);
    }

    @Test
    void shouldGetAllTransactions() {
        transactionService.saveTransaction(Transaction.createDeposit(UUID.randomUUID(), new BigDecimal("100"), TransactionStatus.SUCCESS).get());
        transactionService.saveTransaction(Transaction.createDeposit(UUID.randomUUID(), new BigDecimal("200"), TransactionStatus.SUCCESS).get());
        assertThat(transactionService.getAllTransactions().size()).isEqualTo(2);
    }
}