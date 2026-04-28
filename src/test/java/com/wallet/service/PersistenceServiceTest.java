package com.wallet.service;

import com.wallet.domain.Currency;
import com.wallet.domain.Transaction;
import com.wallet.domain.TransactionStatus;
import com.wallet.domain.Wallet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PersistenceServiceTest {
    private PersistenceService persistenceService;
    private static final String WALLETS_CSV = "wallets.csv";
    private static final String TRANSACTIONS_CSV = "transactions.csv";

    @BeforeEach
    void setUp() {
        persistenceService = new PersistenceService();
        cleanup();
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    private void cleanup() {
        new File(WALLETS_CSV).delete();
        new File(TRANSACTIONS_CSV).delete();
    }

    @Test
    void shouldSaveAndLoadWallets() {
        Wallet w1 = Wallet.create(java.util.UUID.randomUUID(), "User1", Currency.USD).get().withBalance(new BigDecimal("100.50"));
        Wallet w2 = Wallet.create(java.util.UUID.randomUUID(), "User2", Currency.EUR).get().withBalance(new BigDecimal("50.00"));
        
        persistenceService.saveWallets(List.of(w1, w2));
        
        List<Wallet> loaded = persistenceService.loadWallets();
        
        assertEquals(2, loaded.size());
        assertTrue(loaded.stream().anyMatch(w -> w.getOwnerName().equals("User1") && w.getBalance().equals(new BigDecimal("100.50")) && w.getCurrency() == Currency.USD));
        assertTrue(loaded.stream().anyMatch(w -> w.getOwnerName().equals("User2") && w.getBalance().equals(new BigDecimal("50.00")) && w.getCurrency() == Currency.EUR));
    }

    @Test
    void shouldSaveAndLoadTransactions() {
        Wallet w1 = Wallet.create(java.util.UUID.randomUUID(), "User1", Currency.USD).get();
        Transaction t1 = Transaction.createDeposit(w1.getWalletId(), new BigDecimal("100.00"), TransactionStatus.SUCCESS).get();
        
        persistenceService.saveTransactions(List.of(t1));
        
        List<Transaction> loaded = persistenceService.loadTransactions();
        
        assertEquals(1, loaded.size());
        Transaction loadedT = loaded.get(0);
        assertEquals(t1.getTransactionId(), loadedT.getTransactionId());
        assertEquals(t1.getAmount(), loadedT.getAmount());
        assertEquals(t1.getType(), loadedT.getType());
        assertEquals(t1.getStatus(), loadedT.getStatus());
        assertEquals(t1.getToWalletId(), loadedT.getToWalletId());
    }
    @Test
    void shouldSaveAndLoadWalletsWithCommasInName() {
        PersistenceService service = new PersistenceService();
        UUID id = UUID.randomUUID();
        Wallet wallet = new Wallet(id, "Doe, John", new BigDecimal("100.00"), Currency.USD, LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));

        service.saveWallets(List.of(wallet));
        List<Wallet> loaded = service.loadWallets();

        assertEquals(1, loaded.size());
        assertEquals("Doe, John", loaded.get(0).getOwnerName());
        assertEquals(wallet.getWalletId(), loaded.get(0).getWalletId());
    }

    @Test
    void shouldSaveAndLoadWalletsWithQuotesInName() {
        PersistenceService service = new PersistenceService();
        UUID id = UUID.randomUUID();
        Wallet wallet = new Wallet(id, "John \"The Wallet\" Doe", new BigDecimal("100.00"), Currency.USD, LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));

        service.saveWallets(List.of(wallet));
        List<Wallet> loaded = service.loadWallets();

        assertEquals(1, loaded.size());
        assertEquals("John \"The Wallet\" Doe", loaded.get(0).getOwnerName());
    }
}
