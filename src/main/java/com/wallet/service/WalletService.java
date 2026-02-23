package com.wallet.service;
import com.wallet.common.DomainError;
import com.wallet.domain.Currency;
import com.wallet.domain.Wallet;
import java.math.BigDecimal;
import java.util.UUID;
import io.vavr.control.Either;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalletService {
    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);
    private final Map<UUID, Wallet> wallets = new HashMap<>();

    public Either<DomainError, Wallet> createWallet(String owner, Currency currency) {
        UUID id = UUID.randomUUID();
        Either<DomainError, Wallet> result = Wallet.create(id, owner, currency);
        if (result.isRight()) {
            wallets.put(id, result.get());
        }
        return result;
    }
    public Either<DomainError, Wallet> deposit(UUID walletId, BigDecimal amount) {
        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Invalid amount for deposit: {}", amount);
            return Either.left(DomainError.INVALID_AMOUNT);
        }
        if (wallets.containsKey(walletId)) {
            Wallet wallet = wallets.get(walletId);
            Wallet updatedWallet = wallet.withBalance(wallet.getBalance().add(amount));
            wallets.put(walletId, updatedWallet);
            logger.info("Wallet updated: {}", updatedWallet);
            return Either.right(updatedWallet);
        }
        logger.warn("Wallet not found: {}", walletId);
        return Either.left(DomainError.WALLET_NOT_FOUND);
    }
    public Either<DomainError, Wallet> withdraw(UUID walletId, BigDecimal amount) {
        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.warn("Invalid amount for withdraw: {}", amount);
            return Either.left(DomainError.INVALID_AMOUNT);
        }
        if (wallets.containsKey(walletId)) {
            Wallet wallet = wallets.get(walletId);
            if (wallet.getBalance().compareTo(amount) < 0) {
                logger.warn("Insufficient funds for wallet : {}", walletId);
                return Either.left(DomainError.INSUFFICIENT_FUNDS);
            }
            Wallet updatedWallet = wallet.withBalance(wallet.getBalance().subtract(amount));
            wallets.put(walletId, updatedWallet);
            logger.info("Wallet updated: {}", updatedWallet);
            return Either.right(updatedWallet);
        }
        logger.warn("Wallet not found: {}", walletId);
        return Either.left(DomainError.WALLET_NOT_FOUND);
    }
    public Either<DomainError, Wallet> getWallet(UUID walletId) {
        if (wallets.containsKey(walletId)) {
            logger.info("Wallet found: {}", walletId);
            return Either.right(wallets.get(walletId));
        }
        logger.warn("Wallet not found: {}", walletId);
        return Either.left(DomainError.WALLET_NOT_FOUND);
    }
}
