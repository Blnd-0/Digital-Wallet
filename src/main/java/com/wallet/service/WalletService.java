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
    public static final BigDecimal MAX_DEPOSIT_AMOUNT = new BigDecimal("10000");
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
        if (amount.compareTo(MAX_DEPOSIT_AMOUNT) > 0) {
            logger.warn("Deposit amount {} exceeds maximum allowed limit {}", amount, MAX_DEPOSIT_AMOUNT);
            return Either.left(DomainError.EXCEEDS_MAX_DEPOSIT);
        }
        @SuppressWarnings("unchecked")
        Either<DomainError, Wallet>[] depositResult = new Either[1];
        wallets.compute(walletId, (id, w) -> {
            if (w == null) {
                depositResult[0] = Either.left(DomainError.WALLET_NOT_FOUND);
                return null;
            }
            Wallet updated = w.withBalance(w.getBalance().add(amount));
            depositResult[0] = Either.right(updated);
            return updated;
        });
        Either<DomainError, Wallet> depositEither = depositResult[0] != null ? depositResult[0] : Either.left(DomainError.WALLET_NOT_FOUND);
        return depositEither
                .peek(updatedWallet -> Transaction.createDeposit(walletId, amount, TransactionStatus.SUCCESS)
                        .forEach(transactionService::saveTransaction))
                .peek(updatedWallet -> logger.info("Wallet updated: {}", updatedWallet))
                .peekLeft(e -> logger.warn("Deposit failed: {}", e.getMessage()));
    }
    public Either<DomainError, Wallet> withdraw(UUID walletId, BigDecimal amount) {
        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Invalid amount for withdraw: {}", amount);
            return Either.left(DomainError.INVALID_AMOUNT);
        }
        @SuppressWarnings("unchecked")
        Either<DomainError, Wallet>[] withdrawResult = new Either[1];
        wallets.compute(walletId, (id, w) -> {
            if (w == null) {
                withdrawResult[0] = Either.left(DomainError.WALLET_NOT_FOUND);
                return null;
            }
            if (w.getBalance().compareTo(amount) < 0) {
                withdrawResult[0] = Either.left(DomainError.INSUFFICIENT_FUNDS);
                return w;
            }
            Wallet updated = w.withBalance(w.getBalance().subtract(amount));
            withdrawResult[0] = Either.right(updated);
            return updated;
        });
        Either<DomainError, Wallet> withdrawEither = withdrawResult[0] != null ? withdrawResult[0] : Either.left(DomainError.WALLET_NOT_FOUND);
        return withdrawEither
                .peek(updatedWallet -> Transaction.createWithdrawal(walletId, amount, TransactionStatus.SUCCESS)
                        .forEach(transactionService::saveTransaction))
                .peek(updatedWallet -> logger.info("Wallet updated: {}", updatedWallet))
                .peekLeft(e -> logger.warn("Withdraw failed: {}", e.getMessage()));
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