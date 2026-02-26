package com.wallet;

import com.wallet.domain.Currency;
import com.wallet.service.TransactionService;
import com.wallet.service.TransferService;
import com.wallet.service.WalletService;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        TransactionService transactionService = new TransactionService();
        WalletService walletService = new WalletService(transactionService);

// Create wallet
        var wallet = walletService.createWallet("Blnd", Currency.USD);
        wallet.peek(w -> System.out.println("Created: " + w.getOwnerName() + " | " + w.getBalance()))
                .peekLeft(e -> System.out.println("Create failed: " + e.getMessage()));

        UUID walletId = wallet.get().getWalletId();

// Deposit
        walletService.deposit(walletId, new BigDecimal("500"))
                .peek(w -> System.out.println("After deposit: " + w.getBalance()))
                .peekLeft(e -> System.out.println("Deposit failed: " + e.getMessage()));

// Withdraw
        walletService.withdraw(walletId, new BigDecimal("200"))
                .peek(w -> System.out.println("After withdraw: " + w.getBalance()))
                .peekLeft(e -> System.out.println("Withdraw failed: " + e.getMessage()));

// Withdraw more than balance
        walletService.withdraw(walletId, new BigDecimal("99999"))
                .peekLeft(e -> System.out.println("Expected failure: " + e.getMessage()));

        System.out.println("*******************************************************************************************");
        System.out.println("*******************************************************************************************");

// Non-existent wallet
        walletService.deposit(UUID.randomUUID(), new BigDecimal("100"))
                .peekLeft(e -> System.out.println("Expected failure: " + e.getMessage()));
        // saveTransaction - null
        transactionService.saveTransaction(null)
                .peekLeft(e -> System.out.println("Null transaction: " + e.getMessage()));

// getTransactionById - invalid ID
        transactionService.getTransactionById(UUID.randomUUID())
                .peekLeft(e -> System.out.println("Transaction not found: " + e.getMessage()));

// getTransactionHistory - use walletId from above
        transactionService.getTransactionHistory(walletId)
                .forEach(t -> System.out.println("History: " + t.getTransactionId() + " | " + t.getType()));

// getAllTransactions
        transactionService.getAllTransactions()
                .forEach(t -> System.out.println("All: " + t.getTransactionId() + " | " + t.getType()));
        TransferService transferService = new TransferService(walletService, transactionService);

// Create two wallets
        var wallet1 = walletService.createWallet("Blnd", Currency.USD);
        var wallet2 = walletService.createWallet("Ali", Currency.USD);
        UUID wallet1Id = wallet1.get().getWalletId();
        UUID wallet2Id = wallet2.get().getWalletId();

// Deposit into wallet1
        walletService.deposit(wallet1Id, new BigDecimal("1000"));

// Valid transfer
        transferService.transfer(wallet1Id, wallet2Id, new BigDecimal("300"))
                .peek(t -> System.out.println("Transfer success: " + t.getTransactionId()))
                .peekLeft(e -> System.out.println("Transfer failed: " + e.getMessage()));

// Self transfer
        transferService.transfer(wallet1Id, wallet1Id, new BigDecimal("100"))
                .peekLeft(e -> System.out.println("Self transfer: " + e.getMessage()));

// Insufficient funds
        transferService.transfer(wallet1Id, wallet2Id, new BigDecimal("99999"))
                .peekLeft(e -> System.out.println("Insufficient: " + e.getMessage()));

// Invalid amount
        transferService.transfer(wallet1Id, wallet2Id, new BigDecimal("-50"))
                .peekLeft(e -> System.out.println("Invalid amount: " + e.getMessage()));

// Non-existent wallet
        transferService.transfer(UUID.randomUUID(), wallet2Id, new BigDecimal("100"))
                .peekLeft(e -> System.out.println("Wallet not found: " + e.getMessage()));

// Currency mismatch
        var wallet3 = walletService.createWallet("Sara", Currency.EUR);
        UUID wallet3Id = wallet3.get().getWalletId();
        walletService.deposit(wallet3Id, new BigDecimal("500"));
        transferService.transfer(wallet1Id, wallet3Id, new BigDecimal("100"))
                .peekLeft(e -> System.out.println("Currency mismatch: " + e.getMessage()));
    }
}