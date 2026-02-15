package com.wallet.domain;

public enum TransactionStatus {
    SUCCESS("Transaction completed successfully"),
    FAILED("Transaction failed");

    private final String message;

    TransactionStatus(String message) {
        this.message = message;
    }
    public String getMessage() {
        return message;
    }
}
