package com.wallet.service;
import com.wallet.common.DomainError;
import com.wallet.domain.Currency;
import com.wallet.domain.Transaction;
import com.wallet.domain.TransactionStatus;
import com.wallet.domain.Wallet;
import java.math.BigDecimal;
import java.util.UUID;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletService {
    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);
    // TODO: Switch to ConcurrentHashMap to prevent structural corruption on concurrent access to different keys
    // private final Map<UUID, Wallet> wallets = new ConcurrentHashMap<>();
    private final Map<UUID, Wallet> wallets = new java.util.concurrent.ConcurrentHashMap<>();
    private final TransactionService transactionService;

    public WalletService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void loadWallets(java.util.List<Wallet> loadedWallets) {
        loadedWallets.forEach(w -> wallets.put(w.getWalletId(), w));
    }

    public java.util.Collection<Wallet> getAllWallets() {
        return wallets.values();
    }

    public Either<DomainError, Wallet> createWallet(String owner, Currency currency) {
        UUID id = UUID.randomUUID();
        return Wallet.create(id, owner, currency).peek(wallet->wallets.put(id,wallet))
                .peekLeft(e->logger.warn("Failed to create wallet: {}", e.getMessage()));
    }
    public Either<DomainError, Wallet> deposit(UUID walletId, BigDecimal amount) {
        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Invalid amount for deposit: {}", amount);
            return Either.left(DomainError.INVALID_AMOUNT);
        }
        // TODO: Race condition — two concurrent deposits on the same wallet both read the same balance,
        //       compute independently, and one overwrites the other (lost update).
        //       Fix: replace the three lines below with wallets.compute(walletId, (id, w) -> w.withBalance(w.getBalance().add(amount)))
        //       That makes the read-modify-write atomic. Requires ConcurrentHashMap above.
        return getWallet(walletId)
                .map(wallet -> wallet.withBalance(wallet.getBalance().add(amount)))
                .peek(updatedWallet -> wallets.put(walletId, updatedWallet))
                .peek(updatedWallet -> Transaction.createDeposit(walletId, amount, TransactionStatus.SUCCESS)
                        .forEach(transactionService::saveTransaction))
                .peek(updatedWallet -> logger.info("Wallet updated: {}", updatedWallet));
    }
    public Either<DomainError, Wallet> withdraw(UUID walletId, BigDecimal amount) {
        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Invalid amount for withdraw: {}", amount);
            return Either.left(DomainError.INVALID_AMOUNT);
        }
        // TODO: Same race condition as deposit — same fix applies (wallets.compute with ConcurrentHashMap).
        //       Also note: the balance check and subtract are two separate steps, so another thread could
        //       withdraw between them. compute() wraps the whole thing atomically.
        return getWallet(walletId)
                .flatMap(wallet->wallet.getBalance().compareTo(amount) < 0
                        ? Either.left(DomainError.INSUFFICIENT_FUNDS)
                        : Either.right(wallet))
                .map(wallet -> wallet.withBalance(wallet.getBalance().subtract(amount)))
                .peek(updatedWallet -> wallets.put(walletId, updatedWallet))
                .peek(updatedWallet -> Transaction.createWithdrawal(walletId, amount, TransactionStatus.SUCCESS)
                .forEach(transactionService::saveTransaction))
                .peek(updatedWallet -> logger.info("Wallet updated: {}", updatedWallet));
    }
    public Either<DomainError, Wallet> getWallet(UUID walletId) {
        return Option.of(wallets.get(walletId))
                .toEither(DomainError.WALLET_NOT_FOUND)
                .peek(wallet -> logger.info("Wallet found: {}", wallet))
                .peekLeft(e -> logger.warn("Wallet not found: {}", e.getMessage()));
    }

    public io.vavr.collection.List<Wallet> getWalletsByOwner(String ownerName) {
        return io.vavr.collection.List.ofAll(wallets.values())
                .filter(w -> w.getOwnerName().equals(ownerName));
    }
}