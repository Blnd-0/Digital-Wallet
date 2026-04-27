package com.wallet;

import com.wallet.domain.Currency;
import com.wallet.gui.WalletGui;
import com.wallet.service.PersistenceService;
import com.wallet.service.TransactionService;
import com.wallet.service.TransferService;
import com.wallet.service.WalletService;

import javax.swing.*;
import java.math.BigDecimal;

public class Main {
    public static void main(String[] args) {
        TransactionService transactionService = new TransactionService();
        WalletService walletService = new WalletService(transactionService);
        TransferService transferService = new TransferService(walletService, transactionService);
        PersistenceService persistenceService = new PersistenceService();

        // Load data
        walletService.loadWallets(persistenceService.loadWallets());
        transactionService.loadTransactions(persistenceService.loadTransactions());

        // Save data helper
        Runnable saveAction = () -> {
            persistenceService.saveWallets(walletService.getAllWallets());
            persistenceService.saveTransactions(transactionService.getAllTransactions().toJavaList());
        };

        // Save data on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(saveAction));

        startApp(walletService, transactionService, transferService, saveAction);
    }

    public static void startApp(WalletService walletService, TransactionService transactionService, TransferService transferService, Runnable saveAction) {
        startApp(walletService, transactionService, transferService, saveAction, true);
    }

    public static void startApp(WalletService walletService, TransactionService transactionService, TransferService transferService, Runnable saveAction, boolean exitOnCancel) {
        // Simple 'login'
        String username = JOptionPane.showInputDialog(null, "Enter your username:", "Login", JOptionPane.QUESTION_MESSAGE);
        
        if (username == null || username.trim().isEmpty()) {
            if (exitOnCancel) {
                System.exit(0);
            }
            return;
        }

        SwingUtilities.invokeLater(() -> {
            WalletGui gui = new WalletGui(username, walletService, transactionService, transferService, saveAction);
            gui.setVisible(true);
        });
    }
}