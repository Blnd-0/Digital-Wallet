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
            return Either.left(new ValidationError("Wallet ID cannot be null"));
        if (owner == null || owner.isBlank())
            return Either.left(new ValidationError("Owner name cannot be blank"));
        if (currency == null)
            return Either.left(new ValidationError("Currency cannot be null"));

        return Either.right(new Wallet(id, owner, currency));
    }

public Wallet withDeposit(BigDecimal amount){
    return new Wallet(
            this.walletId,
            this.ownerName,
            this.balance.add(amount),
            this.currency,
            this.createdAt
    );
}

public Wallet withWithdrawal(BigDecimal amount) {
    return new Wallet(
            this.walletId,
            this.ownerName,
            this.balance.subtract(amount),
            this.currency,
            this.createdAt
    );
}

public Wallet withBalance(BigDecimal newBalance){
    return new Wallet(
            this.walletId,
            this.ownerName,
            newBalance,
            this.currency,
            this.createdAt
    );
}
}
