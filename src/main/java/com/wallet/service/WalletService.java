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
    private final Map<UUID, Wallet> wallets = new HashMap<>();
    private final TransactionService transactionService;

    public WalletService(TransactionService transactionService) {
        this.transactionService = transactionService;
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
}