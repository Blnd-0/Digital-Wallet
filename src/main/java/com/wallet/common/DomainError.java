package com.wallet.common;
import lombok.Getter;

@Getter
public enum DomainError {
        INSUFFICIENT_FUNDS("Insufficient balance in wallet"),
        INVALID_AMOUNT("Amount must be greater than zero"),
        WALLET_NOT_FOUND("Wallet does not exist"),
        CURRENCY_MISMATCH("Wallets must have same currency"),
        NEGATIVE_BALANCE("Balance cannot be negative"),
        SELF_TRANSFER("Cannot transfer to same wallet"),
        INVALID_INPUT("Input is null or invalid"),
        TRANSACTION_NOT_FOUND("Transaction not found"),;

    private final String message;

    DomainError(String message) {
        this.message = message;
    }
}
