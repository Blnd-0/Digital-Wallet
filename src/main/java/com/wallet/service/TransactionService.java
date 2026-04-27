package com.wallet.service;
import com.wallet.common.DomainError;
import com.wallet.domain.Transaction;
import io.vavr.collection.List;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionService {
    private final Map<UUID, Transaction> transactions = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    public Either<DomainError, Transaction> saveTransaction(Transaction transaction) {
        return Option.of(transaction)
                .toEither(DomainError.INVALID_INPUT)
                .peek(transaction1 -> transactions.put(transaction1.getTransactionId(), transaction1))
                .peek(transaction1 -> logger.info("saving transaction {}", transaction1.getTransactionId()))
                .peekLeft(e -> logger.warn("Failed to save transaction: {}", e.getMessage()));
    }
    public Either<DomainError, Transaction> getTransactionById(UUID transactionId) {
        return Option.of(transactions.get(transactionId))
                .toEither(DomainError.TRANSACTION_NOT_FOUND)
                .peek(transaction -> logger.info("Transaction found: {}", transaction))
                .peekLeft(e -> logger.warn("Transaction not found: {}", e.getMessage()));
    }
    public List<Transaction> getTransactionHistory(UUID walletId) {
        logger.info("getting transaction history {}", walletId);
        return List.ofAll(transactions.values().stream()
                .filter(transaction -> transaction.getFromWalletId().contains(walletId)||transaction.getToWalletId().contains(walletId))
                .toList()
        );
    }
    public List<Transaction> getAllTransactions() {
        logger.info("getting all transactions");
        return List.ofAll(transactions.values());
    }
}