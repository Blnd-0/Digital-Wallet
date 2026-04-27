package com.wallet.service;

import com.wallet.domain.*;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PersistenceService {
    private static final Logger logger = LoggerFactory.getLogger(PersistenceService.class);
    private static final String WALLETS_CSV = "wallets.csv";
    private static final String TRANSACTIONS_CSV = "transactions.csv";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private String escape(String data) {
        if (data == null) return "";
        if (data.contains(",") || data.contains("\"") || data.contains("\n")) {
            return "\"" + data.replace("\"", "\"\"") + "\"";
        }
        return data;
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    current.append('\"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    public void saveWallets(java.util.Collection<Wallet> wallets) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(WALLETS_CSV))) {
            for (Wallet w : wallets) {
                pw.println(String.join(",",
                        w.getWalletId().toString(),
                        escape(w.getOwnerName()),
                        w.getBalance().toString(),
                        w.getCurrency().name(),
                        w.getCreatedAt().format(DATE_TIME_FORMATTER)
                ));
            }
        } catch (IOException e) {
            logger.error("Failed to save wallets", e);
        }
    }

    public List<Wallet> loadWallets() {
        List<Wallet> wallets = new ArrayList<>();
        File file = new File(WALLETS_CSV);
        if (!file.exists()) return wallets;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = parseCsvLine(line);
                if (parts.length == 5) {
                    Wallet w = new Wallet(
                            UUID.fromString(parts[0]),
                            parts[1],
                            new BigDecimal(parts[2]),
                            Currency.valueOf(parts[3]),
                            LocalDateTime.parse(parts[4], DATE_TIME_FORMATTER)
                    );
                    wallets.add(w);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load wallets", e);
        }
        return wallets;
    }

    public void saveTransactions(java.util.Collection<Transaction> transactions) {
        saveTransactionsToFile(transactions, new File(TRANSACTIONS_CSV));
    }

    public void saveTransactionsToFile(java.util.Collection<Transaction> transactions, File file) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            for (Transaction t : transactions) {
                pw.println(String.join(",",
                        t.getTransactionId().toString(),
                        t.getType().name(),
                        t.getAmount().toString(),
                        t.getFromWalletId().map(UUID::toString).getOrElse(""),
                        t.getToWalletId().map(UUID::toString).getOrElse(""),
                        t.getTimeStamp().format(DATE_TIME_FORMATTER),
                        t.getStatus().name()
                ));
            }
        } catch (IOException e) {
            logger.error("Failed to save transactions to " + file.getName(), e);
        }
    }

    public List<Transaction> loadTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        File file = new File(TRANSACTIONS_CSV);
        if (!file.exists()) return transactions;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = parseCsvLine(line);
                if (parts.length == 7) {
                    transactions.add(new Transaction(
                            UUID.fromString(parts[0]),
                            TransactionType.valueOf(parts[1]),
                            new BigDecimal(parts[2]),
                            parts[3].isEmpty() ? Option.none() : Option.of(UUID.fromString(parts[3])),
                            parts[4].isEmpty() ? Option.none() : Option.of(UUID.fromString(parts[4])),
                            LocalDateTime.parse(parts[5], DATE_TIME_FORMATTER),
                            TransactionStatus.valueOf(parts[6])
                    ));
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load transactions", e);
        }
        return transactions;
    }
}
