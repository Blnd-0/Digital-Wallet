package com.wallet.service;
import com.wallet.common.DomainError;
import com.wallet.domain.Transaction;
import com.wallet.domain.TransactionStatus;
import com.wallet.domain.Wallet;
import com.wallet.service.WalletService;
import com.wallet.service.TransactionService;
import io.vavr.control.Either;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllArgsConstructor
public class TransferService {
    WalletService walletService;
    TransactionService transactionService;
    private static final Logger logger = LoggerFactory.getLogger(TransferService.class);

    public Either<DomainError, Transaction> transfer(UUID fromWalletId, UUID toWalletId, BigDecimal amount) {
        ReentrantLock lock1;
        ReentrantLock lock2;

        if (fromWalletId.compareTo(toWalletId) < 0) {
            lock1 = getLock(fromWalletId);
            lock2 = getLock(toWalletId);
        } else {
            lock1 = getLock(toWalletId);
            lock2 = getLock(fromWalletId);
        }
        lock1.lock();
        lock2.lock();
        try {
            Either<DomainError, UUID> selfTransferCheck = fromWalletId.equals(toWalletId)
                    ? Either.left(DomainError.SELF_TRANSFER)
                    : Either.right(fromWalletId);

            return selfTransferCheck
                    .flatMap(validAmount -> walletService.getWallet(fromWalletId)
                            .flatMap(fromWallet -> walletService.getWallet(toWalletId)
                                    .flatMap(toWallet -> fromWallet.getCurrency().equals(toWallet.getCurrency())
                                            ? walletService.withdraw(fromWalletId, amount)
                                            .flatMap(__ -> walletService.deposit(toWalletId, amount))
                                            .flatMap(__ -> Transaction.createTransfer(fromWalletId, toWalletId, amount, TransactionStatus.SUCCESS))
                                            .peek(transaction -> logger.info("Transfer successful: {}", transaction))
                                            .peek(transactionService::saveTransaction)
                                            : Either.left(DomainError.CURRENCY_MISMATCH)
                                    )))
                    .peekLeft(e -> logger.warn("Transfer failed: {}", e.getMessage()));
        }
        finally{
            lock1.unlock();
            lock2.unlock();
        }
    }
    private final ConcurrentHashMap<UUID, ReentrantLock> walletLocks=new ConcurrentHashMap<>();

    private ReentrantLock getLock(UUID walletId){
        return walletLocks.computeIfAbsent(walletId,id->new ReentrantLock());
    }
}
