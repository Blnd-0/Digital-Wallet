package com.wallet.service;
import com.wallet.common.DomainError;
import com.wallet.domain.Transaction;
import com.wallet.domain.TransactionType;
import io.vavr.collection.List;
import io.vavr.control.Either;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TransactionService {
    private final Map<UUID, Transaction> transactions = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    public Either<DomainError, Transaction> saveTransaction(Transaction transaction) {
        if(transaction == null){
            logger.warn("transaction is null");
            return Either.left (DomainError.INVALID_INPUT);
        }
        transactions.put(transaction.getTransactionId(), transaction);
        logger.info("saving transaction {}", transaction.getTransactionId());
        return Either.right (transaction);
    }
    public Either<DomainError, Transaction> getTransactionById(UUID transactionId) {
        if(transactions.containsKey(transactionId)){
            logger.info("getting transaction {}", transactionId);
            return Either.right (transactions.get(transactionId));
        }
        logger.warn("Transaction not found {}", transactionId);
        return Either.left (DomainError.TRANSACTION_NOT_FOUND);
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
