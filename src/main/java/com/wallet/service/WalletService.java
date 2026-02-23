package com.wallet.service;
import com.wallet.common.DomainError;
import com.wallet.domain.Currency;
import com.wallet.domain.Wallet;
import java.math.BigDecimal;
import java.util.UUID;
import io.vavr.control.Either;
import java.util.HashMap;
import java.util.Map;

public class WalletService {
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
            return Either.left(DomainError.INVALID_AMOUNT);
        }
        if (wallets.containsKey(walletId)) {
            Wallet wallet = wallets.get(walletId);
            Wallet updatedWallet = wallet.withBalance(wallet.getBalance().add(amount));
            wallets.put(walletId, updatedWallet);
            return Either.right(updatedWallet);
        }
        return Either.left(DomainError.WALLET_NOT_FOUND);
    }
    public Either<DomainError, Wallet> withdraw(UUID walletId, BigDecimal amount) {
        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Either.left(DomainError.INVALID_AMOUNT);
        }
        if (wallets.containsKey(walletId)) {
            Wallet wallet = wallets.get(walletId);
            if (wallet.getBalance().compareTo(amount) < 0) {
                return Either.left(DomainError.INSUFFICIENT_FUNDS);
            }
            Wallet updatedWallet = wallet.withBalance(wallet.getBalance().subtract(amount));
            wallets.put(walletId, updatedWallet);
            return Either.right(updatedWallet);
        }
        return Either.left(DomainError.WALLET_NOT_FOUND);
    }
    public Either<DomainError, Wallet> getWallet(UUID walletId) {
        if (wallets.containsKey(walletId)) {
            return Either.right(wallets.get(walletId));
        }
        return Either.left(DomainError.WALLET_NOT_FOUND);
    }
}
