package com.wallet.gui;

import com.wallet.Main;
import com.wallet.domain.Currency;
import com.wallet.domain.Transaction;
import com.wallet.domain.Wallet;
import com.wallet.service.PersistenceService;
import com.wallet.service.TransactionService;
import com.wallet.service.TransferService;
import com.wallet.service.WalletService;
import io.vavr.control.Either;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.Vector;

public class WalletGui extends JFrame {
    private final String ownerName;
    private final WalletService walletService;
    private final TransactionService transactionService;
    private final TransferService transferService;
    private final Runnable saveAction;

    private JComboBox<WalletWrapper> walletDropdown;
    private JComboBox<WalletWrapper> targetWalletDropdown;
    private JTextField amountField;
    private JTable transactionTable;
    private DefaultTableModel tableModel;
    private JLabel balanceLabel;
    private JLabel statusLabel;

    public WalletGui(String ownerName, WalletService walletService, TransactionService transactionService, TransferService transferService, Runnable saveAction) {
        this.ownerName = ownerName;
        this.walletService = walletService;
        this.transactionService = transactionService;
        this.transferService = transferService;
        this.saveAction = saveAction;

        setTitle("Digital Wallet - " + ownerName);
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        initUI();
        refreshWallets();
    }

    private void initUI() {
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosed(java.awt.event.WindowEvent e) {
                if (saveAction != null) {
                    saveAction.run();
                }
            }
        });
        // Main split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(400);

        // Left Panel - Controls
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Controls"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;

        // Wallet Selection
        gbc.gridy = 0;
        leftPanel.add(new JLabel("Select Wallet:"), gbc);
        walletDropdown = new JComboBox<>();
        walletDropdown.addActionListener(e -> updateBalanceAndTransactions());
        gbc.gridy = 1;
        leftPanel.add(walletDropdown, gbc);

        // Amount Input
        gbc.gridy = 2;
        leftPanel.add(new JLabel("Amount:"), gbc);
        amountField = new JTextField();
        gbc.gridy = 3;
        leftPanel.add(amountField, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        JButton depositBtn = new JButton("Deposit");
        JButton withdrawBtn = new JButton("Withdraw");
        JButton transferBtn = new JButton("Transfer");
        JButton createBtn = new JButton("Create Wallet");

        depositBtn.addActionListener(e -> handleDeposit());
        withdrawBtn.addActionListener(e -> handleWithdraw());
        transferBtn.addActionListener(e -> handleTransfer());
        createBtn.addActionListener(e -> handleCreateWallet());

        buttonPanel.add(createBtn);
        buttonPanel.add(depositBtn);
        buttonPanel.add(withdrawBtn);
        buttonPanel.add(transferBtn);

        gbc.gridy = 4;
        gbc.weighty = 0;
        leftPanel.add(buttonPanel, gbc);

        // Target Wallet for Transfer
        gbc.gridy = 5;
        leftPanel.add(new JLabel("Target Wallet (for Transfer):"), gbc);
        targetWalletDropdown = new JComboBox<>();
        gbc.gridy = 6;
        leftPanel.add(targetWalletDropdown, gbc);

        // Spacer
        gbc.gridy = 7;
        gbc.weighty = 1.0;
        leftPanel.add(new JPanel(), gbc);

        // Right Panel - Transaction History
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Transaction History"));
        
        String[] columnNames = {"ID", "Type", "Amount", "From", "To", "Time", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        transactionTable = new JTable(tableModel);
        rightPanel.add(new JScrollPane(transactionTable), BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);
        add(splitPane, BorderLayout.CENTER);

        // Bottom Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        JPanel leftBottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        balanceLabel = new JLabel("Balance: -");
        JButton switchUserBtn = new JButton("Switch User");
        switchUserBtn.addActionListener(e -> handleSwitchUser());
        JButton exportCsvBtn = new JButton("Export CSV");
        exportCsvBtn.addActionListener(e -> handleExportCsv());
        leftBottom.add(balanceLabel);
        leftBottom.add(switchUserBtn);
        leftBottom.add(exportCsvBtn);

        statusLabel = new JLabel("Welcome, " + ownerName);
        statusLabel.setForeground(Color.BLUE);

        bottomBar.add(leftBottom, BorderLayout.WEST);
        bottomBar.add(statusLabel, BorderLayout.EAST);
        add(bottomBar, BorderLayout.SOUTH);
    }

    private void refreshWallets() {
        WalletWrapper selected = (WalletWrapper) walletDropdown.getSelectedItem();
        UUID selectedId = selected != null ? selected.wallet.getWalletId() : null;

        walletDropdown.removeAllItems();
        targetWalletDropdown.removeAllItems();

        var wallets = walletService.getWalletsByOwner(ownerName);
        for (Wallet w : wallets) {
            WalletWrapper wrapper = new WalletWrapper(w);
            walletDropdown.addItem(wrapper);
            targetWalletDropdown.addItem(wrapper);
            if (selectedId != null && w.getWalletId().equals(selectedId)) {
                walletDropdown.setSelectedItem(wrapper);
            }
        }
        
        updateBalanceAndTransactions();
    }

    private void updateBalanceAndTransactions() {
        WalletWrapper selected = (WalletWrapper) walletDropdown.getSelectedItem();
        if (selected == null) {
            balanceLabel.setText("Balance: -");
            tableModel.setRowCount(0);
            return;
        }

        Wallet w = selected.wallet;
        // Get fresh data from service if possible, but Wallet objects are immutable value objects in this project.
        // The WalletService updates its internal map with new Wallet instances.
        // So we should ideally get the latest wallet from service.
        walletService.getWallet(w.getWalletId()).peek(latest -> {
            balanceLabel.setText(String.format("Balance: %s %s", latest.getBalance(), latest.getCurrency()));
        });

        tableModel.setRowCount(0);
        var history = transactionService.getTransactionHistory(w.getWalletId());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (Transaction t : history) {
            Vector<Object> row = new Vector<>();
            row.add(t.getTransactionId().toString().substring(0, 8));
            row.add(t.getType());
            row.add(t.getAmount());
            row.add(t.getFromWalletId().map(UUID::toString).map(s -> s.substring(0, 8)).getOrElse("-"));
            row.add(t.getToWalletId().map(UUID::toString).map(s -> s.substring(0, 8)).getOrElse("-"));
            row.add(t.getTimeStamp().format(formatter));
            row.add(t.getStatus());
            tableModel.addRow(row);
        }
    }

    private void handleDeposit() {
        withSelectedWallet(wallet -> {
            BigDecimal amount = getAmount();
            if (amount == null) return;
            
            var result = walletService.deposit(wallet.getWalletId(), amount);
            handleResult(result, "Deposit successful");
        });
    }

    private void handleWithdraw() {
        withSelectedWallet(wallet -> {
            BigDecimal amount = getAmount();
            if (amount == null) return;
            
            var result = walletService.withdraw(wallet.getWalletId(), amount);
            handleResult(result, "Withdrawal successful");
        });
    }

    private void handleTransfer() {
        withSelectedWallet(fromWallet -> {
            WalletWrapper targetWrapper = (WalletWrapper) targetWalletDropdown.getSelectedItem();
            if (targetWrapper == null) {
                showError("Select a target wallet");
                return;
            }
            BigDecimal amount = getAmount();
            if (amount == null) return;
            
            var result = transferService.transfer(fromWallet.getWalletId(), targetWrapper.wallet.getWalletId(), amount);
            handleResult(result, "Transfer successful");
        });
    }

    private void handleCreateWallet() {
        String[] currencies = {"USD", "EUR", "IQD"};
        String currencyStr = (String) JOptionPane.showInputDialog(this, "Select Currency", "Create Wallet",
                JOptionPane.QUESTION_MESSAGE, null, currencies, currencies[0]);
        
        if (currencyStr != null) {
            Currency currency = Currency.valueOf(currencyStr);
            var result = walletService.createWallet(ownerName, currency);
            handleResult(result, "Wallet created");
            refreshWallets();
        }
    }

    private void handleSwitchUser() {
        this.dispose();
        Main.startApp(walletService, transactionService, transferService, saveAction, false);
    }

    private void handleExportCsv() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Transactions to CSV");
        fileChooser.setSelectedFile(new java.io.File("transactions_export.csv"));
        
        int userSelection = fileChooser.showSaveDialog(this);
        
        if (userSelection == javax.swing.JFileChooser.APPROVE_OPTION) {
            java.io.File fileToSave = fileChooser.getSelectedFile();
            
            // Gather all transactions for the current user
            var userWallets = walletService.getWalletsByOwner(ownerName);
            var walletIds = userWallets.map(Wallet::getWalletId).toSet();
            
            var allTransactions = transactionService.getAllTransactions();
            var userTransactions = allTransactions.filter(t -> 
                t.getFromWalletId().exists(walletIds::contains) || 
                t.getToWalletId().exists(walletIds::contains)
            );
            
            PersistenceService persistenceService = new PersistenceService();
            persistenceService.saveTransactionsToFile(userTransactions.toJavaList(), fileToSave);
            
            statusLabel.setText("Exported " + userTransactions.size() + " transactions to " + fileToSave.getName());
            statusLabel.setForeground(new Color(0, 100, 0)); // Dark green
        }
    }

    private void withSelectedWallet(java.util.function.Consumer<Wallet> action) {
        WalletWrapper selected = (WalletWrapper) walletDropdown.getSelectedItem();
        if (selected == null) {
            showError("No wallet selected");
            return;
        }
        action.accept(selected.wallet);
    }

    private BigDecimal getAmount() {
        try {
            return new BigDecimal(amountField.getText());
        } catch (NumberFormatException e) {
            showError("Invalid amount");
            return null;
        }
    }

    private void handleResult(Either<?, ?> result, String successMsg) {
        if (result.isRight()) {
            statusLabel.setText(successMsg);
            statusLabel.setForeground(new Color(0, 128, 0));
            refreshWallets();
            amountField.setText("");
        } else {
            Object left = result.getLeft();
            if (left instanceof com.wallet.common.DomainError) {
                showError(((com.wallet.common.DomainError) left).getMessage());
            } else {
                showError(left.toString());
            }
        }
    }

    private void showError(String msg) {
        statusLabel.setText("Error: " + msg);
        statusLabel.setForeground(Color.RED);
    }

    private static class WalletWrapper {
        final Wallet wallet;
        WalletWrapper(Wallet wallet) { this.wallet = wallet; }
        @Override
        public String toString() {
            return String.format("%s (%s)", wallet.getWalletId().toString().substring(0, 8), wallet.getCurrency());
        }
    }
}
