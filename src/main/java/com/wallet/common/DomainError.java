package com.wallet.common;
import lombok.Getter;
import lombok.Value;

@Getter
public enum DomainError {

        INSUFFICIENT_FUNDS("Insufficient balance in wallet"),
        INVALID_AMOUNT("Amount must be greater than zero"),
        WALLET_NOT_FOUND("Wallet does not exist"),
        CURRENCY_MISMATCH("Wallets must have same currency"),
        NEGATIVE_BALANCE("Balance cannot be negative"),
        SELF_TRANSFER("Cannot transfer to same wallet");

        private final String message;

    DomainError(String message) {
        this.message = message;
    }
}
