package com.wallet.domain;

public enum TransactionType {
    DEPOSIT("Deposit transaction"),
    WITHDRAW("Withdrawal transaction"),
    TRANSFER("Transfer transaction");

    private final String message;

    TransactionType(String message){
        this.message = message;
    }
    public String getMessage() {
        return message;
    }
}
