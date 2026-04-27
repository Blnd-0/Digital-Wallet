package com.wallet.service;

import com.wallet.domain.Currency;
import com.wallet.domain.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TransferServiceTest {

    private WalletService walletService;
    private TransactionService transactionService;
    private TransferService transferService;
    private UUID wallet1Id;
    private UUID wallet2Id;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService();
        walletService = new WalletService(transactionService);
        transferService = new TransferService(walletService, transactionService);
        wallet1Id = walletService.createWallet("Blnd", Currency.USD).get().getWalletId();
        wallet2Id = walletService.createWallet("Ali", Currency.USD).get().getWalletId();
        walletService.deposit(wallet1Id, new BigDecimal("1000"));
        walletService.deposit(wallet2Id, new BigDecimal("1000"));
    }
//
//    @Test
//    void shouldTransferSuccessfully() {
//        var result = transferService.transfer(wallet1Id, wallet2Id, new BigDecimal("300"));
//        assertThat(result.isRight()).isTrue();
//        assertThat(result.get().getType()).isEqualTo(TransactionType.TRANSFER);
//        assertThat(walletService.getWallet(wallet1Id).get().getBalance()).isEqualByComparingTo("700");
//        assertThat(walletService.getWallet(wallet2Id).get().getBalance()).isEqualByComparingTo("1300");
//    }
//
//    @Test
//    void shouldFailSelfTransfer() {
//        var result = transferService.transfer(wallet1Id, wallet1Id, new BigDecimal("100"));
//        assertThat(result.isLeft()).isTrue();
//    }
//
//    @Test
//    void shouldFailInsufficientFunds() {
//        var result = transferService.transfer(wallet1Id, wallet2Id, new BigDecimal("99999"));
//        assertThat(result.isLeft()).isTrue();
//    }
//
//    @Test
//    void shouldFailInvalidAmount() {
//        var result = transferService.transfer(wallet1Id, wallet2Id, new BigDecimal("-100"));
//        assertThat(result.isLeft()).isTrue();
//    }
//
//    @Test
//    void shouldFailNonExistentWallet() {
//        var result = transferService.transfer(UUID.randomUUID(), wallet2Id, new BigDecimal("100"));
//        assertThat(result.isLeft()).isTrue();
//    }
//
//    @Test
//    void shouldFailCurrencyMismatch() {
//        UUID eurWalletId = walletService.createWallet("Sara", Currency.EUR).get().getWalletId();
//        walletService.deposit(eurWalletId, new BigDecimal("500"));
//        var result = transferService.transfer(wallet1Id, eurWalletId, new BigDecimal("100"));
//        assertThat(result.isLeft()).isTrue();
//    }

    @Test
    void shouldHandleConcurrentTransfers() throws InterruptedException {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    var result = transferService.transfer(wallet1Id, wallet2Id, new BigDecimal("100"));
                    if (result.isRight()) successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        BigDecimal total = walletService.getWallet(wallet1Id).get().getBalance()
                .add(walletService.getWallet(wallet2Id).get().getBalance());
        assertThat(total).isEqualByComparingTo("2000");
    }
    @Test
    void shouldFailNullWalletId() {
        var result = transferService.transfer(null, wallet2Id, new BigDecimal("100"));
        assertThat(result.isLeft()).isTrue();
    }

    @Test
    void shouldFailZeroAmount() {
        var result = transferService.transfer(wallet1Id, wallet2Id, BigDecimal.ZERO);
        assertThat(result.isLeft()).isTrue();
    }

    @Test
    void shouldFailNullAmount() {
        var result = transferService.transfer(wallet1Id, wallet2Id, null);
        assertThat(result.isLeft()).isTrue();
    }

    @Test
    void shouldHandleBidirectionalConcurrentTransfers() throws InterruptedException {
        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);

        for (int i = 0; i < threads; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    latch.await();
                    if (index % 2 == 0) {
                        transferService.transfer(wallet1Id, wallet2Id, new BigDecimal("50"));
                    } else {
                        transferService.transfer(wallet2Id, wallet1Id, new BigDecimal("50"));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        BigDecimal total = walletService.getWallet(wallet1Id).get().getBalance()
                .add(walletService.getWallet(wallet2Id).get().getBalance());
        assertThat(total).isEqualByComparingTo("2000");
    }
}