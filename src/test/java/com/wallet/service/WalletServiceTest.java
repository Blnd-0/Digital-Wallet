package com.wallet.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import com.wallet.domain.Currency;
import java.math.BigDecimal;

public class WalletServiceTest {

    private WalletService walletService;
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService();
        walletService = new WalletService(transactionService);
    }
        @Test
        void shouldDepositSuccessfully() {
            // arrange
            var wallet = walletService.createWallet("Blnd", Currency.USD).get();

            // act
            var result = walletService.deposit(wallet.getWalletId(), new BigDecimal("500"));

            // assert
            assertThat(result.isRight()).isTrue();
            assertThat(result.get().getBalance()).isEqualByComparingTo("500");
    }
    @Test
    void shouldFailNegAmount(){
        var wallet = walletService.createWallet("Blnd", Currency.USD).get();
        var result = walletService.deposit(wallet.getWalletId(), new BigDecimal("-500"));
        assertThat(result.isLeft()).isTrue();
    }
    @Test
    void ShouldFailNonExistentWallet(){
        var result = walletService.deposit(java.util.UUID.randomUUID(), new BigDecimal("500"));
        assertThat(result.isLeft()).isTrue();
    }
    //Withdraw
    @Test
    void shouldWithdrawSuccessfully() {
        // arrange
        var wallet = walletService.createWallet("Blnd", Currency.USD).get();
        walletService.deposit(wallet.getWalletId(), new BigDecimal("500")).get();
        var result = walletService.withdraw(wallet.getWalletId(), new BigDecimal("200"));
        assertThat(result.isRight()).isTrue();
        assertThat(result.get().getBalance()).isEqualByComparingTo("300");
    }
    //Withdraw more than balance
    @Test
    void shouldFailWithdrawMoreThanBalance() {
        // arrange
        var wallet = walletService.createWallet("Blnd", Currency.USD).get();
        walletService.deposit(wallet.getWalletId(), new BigDecimal("500")).get();
        var result = walletService.withdraw(wallet.getWalletId(), new BigDecimal("1000"));
        assertThat(result.isLeft()).isTrue();
    }
    //Withdraw negative amount
    @Test
    void shouldFailWithdrawNegativeAmount() {
        // arrange
        var wallet = walletService.createWallet("Blnd", Currency.USD).get();
        walletService.deposit(wallet.getWalletId(), new BigDecimal("500")).get();
        var result = walletService.withdraw(wallet.getWalletId(), new BigDecimal("-500"));
        assertThat(result.isLeft()).isTrue();
    }
    //Withdraw from non-existent wallet
    @Test
    void shouldFailWithdrawFromNonExistentWallet() {
        // arrange
        var result = walletService.withdraw(java.util.UUID.randomUUID(), new BigDecimal("500"));
        assertThat(result.isLeft()).isTrue();
    }
    //Create wallet
    @Test
    void shouldCreateWalletSuccessfully() {
        // arrange
        var result = walletService.createWallet("Blnd", Currency.USD);
        assertThat(result.isRight()).isTrue();
        assertThat(result.get().getOwnerName()).isEqualTo("Blnd");
        assertThat(result.get().getCurrency()).isEqualTo(Currency.USD);
        assertThat(result.get().getBalance()).isEqualByComparingTo("0");
    }
    //create wallet should fail with null owner
    @Test
    void shouldFailCreateWalletWithNullOwner() {
        // arrange
        var result = walletService.createWallet(null, Currency.USD);
        assertThat(result.isLeft()).isTrue();
    }
    //create wallet should fail with Null owner
    @Test
    void shouldFailCreateWalletWithEmptyOwner() {
        // arrange
        var result = walletService.createWallet("", Currency.USD);
        assertThat(result.isLeft()).isTrue();
    }
    @Test
    void shouldFailDepositZeroAmount() {
        var wallet = walletService.createWallet("Blnd", Currency.USD).get();
        var result = walletService.deposit(wallet.getWalletId(), BigDecimal.ZERO);
        assertThat(result.isLeft()).isTrue();
    }

    @Test
    void shouldFailDepositNullAmount() {
        var wallet = walletService.createWallet("Blnd", Currency.USD).get();
        var result = walletService.deposit(wallet.getWalletId(), null);
        assertThat(result.isLeft()).isTrue();
    }
    
    @Test
    void shouldFailDepositExceedingMaxAmount() {
        var wallet = walletService.createWallet("Blnd", Currency.USD).get();
        var result = walletService.deposit(wallet.getWalletId(), WalletService.MAX_DEPOSIT_AMOUNT.add(BigDecimal.ONE));
        assertThat(result.isLeft()).isTrue();
        assertThat(result.getLeft()).isEqualTo(com.wallet.common.DomainError.EXCEEDS_MAX_DEPOSIT);
    }

    @Test
    void shouldFailCreateWalletNullCurrency() {
        var result = walletService.createWallet("Blnd", null);
        assertThat(result.isLeft()).isTrue();
    }

    @Test
    void shouldFailCreateWalletNullOwner() {
        var result = walletService.createWallet(null, Currency.USD);
        assertThat(result.isLeft()).isTrue();
    }

    @Test
    void concurrentDepositRaceCondition() throws InterruptedException {
        // arrange
        var wallet = walletService.createWallet("ConcurrentOwner", Currency.USD).get();
        int threadsCount = 100;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(threadsCount);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(threadsCount);

        // act
        for (int i = 0; i < threadsCount; i++) {
            executor.submit(() -> {
                try {
                    walletService.deposit(wallet.getWalletId(), BigDecimal.ONE);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        // assert
        var updatedWallet = walletService.getWallet(wallet.getWalletId()).get();
        // This is expected to fail if there's a race condition
        if (updatedWallet.getBalance().compareTo(new BigDecimal("100")) != 0) {
            throw new RuntimeException("Race condition detected! Final balance was " + updatedWallet.getBalance() + " instead of 100");
        }
        assertThat(updatedWallet.getBalance()).isEqualByComparingTo(new BigDecimal("100"));
    }
}