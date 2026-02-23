package com.wallet.domain;
import com.wallet.common.DomainError;
import lombok.Value;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import io.vavr.control.Either;

@Value
public class Wallet {
UUID walletId;
String ownerName;
BigDecimal balance;
Currency currency;
LocalDateTime createdAt;

    public static Either<DomainError, Wallet> create(UUID id, String owner, Currency currency) {
        if (id == null)
            return Either.left(DomainError.INVALID_INPUT);
        if (owner == null || owner.isBlank())
            return Either.left(DomainError.INVALID_INPUT);
        if (currency == null)
            return Either.left(DomainError.INVALID_INPUT);

        return Either.right(new Wallet(id, owner, BigDecimal.ZERO, currency, LocalDateTime.now()));
    }
    public Wallet withBalance(BigDecimal newBalance) {
        return new Wallet(walletId, ownerName, newBalance, currency, createdAt);
    }
}
