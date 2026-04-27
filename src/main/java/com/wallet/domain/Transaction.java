package com.wallet.domain;
import com.wallet.common.DomainError;
import io.vavr.control.Either;
import lombok.Value;
import io.vavr.control.Option;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Value
public class Transaction {
    UUID transactionId;
    TransactionType type;
    BigDecimal amount;
    Option<UUID> fromWalletId;
    Option<UUID> toWalletId;
    LocalDateTime timeStamp;
    TransactionStatus status;

    public static Either<DomainError, Transaction> createDeposit(UUID toWalletId, BigDecimal amount, TransactionStatus status) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Either.left(DomainError.INVALID_AMOUNT);
        }
        return Either.right(new Transaction(UUID.randomUUID(), TransactionType.DEPOSIT, amount, Option.none(), Option.of(toWalletId), LocalDateTime.now(), status));
    }
    public static Either<DomainError, Transaction> createWithdrawal(UUID fromWalletId, BigDecimal amount, TransactionStatus status) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Either.left(DomainError.INVALID_AMOUNT);
        }
        return Either.right(new Transaction(UUID.randomUUID(), TransactionType.WITHDRAW, amount, Option.of(fromWalletId), Option.none(), LocalDateTime.now(), status));
    }
    public static Either<DomainError, Transaction> createTransfer(UUID fromWalletId, UUID toWalletId, BigDecimal amount, TransactionStatus status) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return Either.left(DomainError.INVALID_AMOUNT);
        }
        return Either.right(new Transaction(UUID.randomUUID(), TransactionType.TRANSFER, amount, Option.of(fromWalletId), Option.of(toWalletId), LocalDateTime.now(), status));
    }
}