# Digital Wallet

A Java-based digital wallet application with a Swing GUI that manages user wallets, supports multi-currency balances, and handles deposits, withdrawals, and transfers between wallets. Built with a functional error-handling style using [Vavr](https://www.vavr.io/), CSV-based persistence, and thread-safe transfer operations.

## Features

- **Multi-user login** — username-based session login with Switch User support; each user sees only their own wallets.
- **Wallet management** — create wallets with an owner name and currency (USD, EUR, GBP).
- **Deposits & withdrawals** — add or remove funds with validation (positive amounts, sufficient balance).
- **Transfers** — move funds between wallets safely, with checks for:
  - Self-transfers
  - Currency mismatch
  - Insufficient funds
  - Non-existent wallets
- **Transaction history** — every operation is recorded and displayed per wallet in the GUI.
- **Export to CSV** — user-triggered export of all transactions for the logged-in user via a file save dialog.
- **CSV persistence** — wallets and transactions are loaded from CSV on startup and saved on shutdown automatically.
- **Functional error handling** — operations return `Either<DomainError, T>` instead of throwing exceptions, making failure cases explicit and chainable.
- **Concurrency-safe transfers** — per-wallet `ReentrantLock`s with consistent UUID-ordered lock acquisition to prevent deadlocks during concurrent transfers.
- **Logging** — SLF4J + Logback integration for info/warn-level operational logs.

## Tech Stack

- **Java 21**
- **Maven** (build tool)
- **Swing** — built-in Java GUI framework
- **Vavr 0.10.4** — functional types (`Either`, `Option`, immutable `List`)
- **Lombok 1.18.30** — boilerplate reduction (`@Value`, `@Getter`, `@AllArgsConstructor`)
- **SLF4J 2.0.16 + Logback 1.5.12** — logging
- **JUnit 5** & **AssertJ** — unit and concurrency testing

## Project Structure

```
src/main/java/com/wallet/
├── Main.java                      # Entry point: service wiring, CSV load/save, login, GUI launch
├── common/
│   └── DomainError.java           # Enum of domain-level error codes & messages
├── domain/
│   ├── Wallet.java                # Immutable wallet aggregate (balance, owner, currency)
│   ├── Transaction.java           # Immutable transaction record
│   ├── TransactionType.java       # DEPOSIT, WITHDRAW, TRANSFER
│   ├── TransactionStatus.java     # SUCCESS, FAILED
│   └── Currency.java              # USD, EUR, GBP
├── service/
│   ├── WalletService.java         # Create wallets, deposit, withdraw
│   ├── TransactionService.java    # Save & query transactions
│   ├── TransferService.java       # Lock-based wallet-to-wallet transfers
│   └── PersistenceService.java    # CSV read/write for wallets and transactions
└── gui/
    └── WalletGui.java             # Swing GUI: wallet controls, transaction table, export
```

## Domain Model

### Wallet
An immutable record (`@Value`) holding `walletId`, `ownerName`, `balance`, `currency`, and `createdAt`. New balances produce a new `Wallet` instance via `withBalance(...)` rather than mutating in place. The updated instance is stored back into the `ConcurrentHashMap`.

### Transaction
An immutable record describing a single operation. Uses Vavr `Option<UUID>` for `fromWalletId` and `toWalletId` because deposits have no source and withdrawals have no destination. Factory methods (`createDeposit`, `createWithdrawal`, `createTransfer`) validate the amount and return `Either<DomainError, Transaction>`.

### DomainError
Enumerates all failure modes: `INSUFFICIENT_FUNDS`, `INVALID_AMOUNT`, `WALLET_NOT_FOUND`, `CURRENCY_MISMATCH`, `NEGATIVE_BALANCE`, `SELF_TRANSFER`, `INVALID_INPUT`, `TRANSACTION_NOT_FOUND`.

## How It Works

Every service operation returns `Either<DomainError, T>`:
- **Right** — the success value (a `Wallet` or `Transaction`).
- **Left** — a `DomainError` describing why it failed.

This lets callers chain operations with `.flatMap(...)` / `.map(...)` and handle side effects (logging, persistence) with `.peek(...)` / `.peekLeft(...)` without throwing or catching exceptions.

For transfers, `TransferService` acquires `ReentrantLock`s for **both** wallets in a deterministic order (lower UUID string first) before performing the withdrawal-then-deposit sequence, preventing deadlocks when two transfers between the same pair of wallets run concurrently.

## Persistence

On startup, `Main` loads `wallets.csv` and `transactions.csv` into memory via `PersistenceService`. A JVM shutdown hook saves all current state back to those files when the application exits. The CSV format is flat and comma-separated; owner names containing commas are a known limitation.

## Getting Started

### Prerequisites
- JDK 21+
- Maven 3.6+

### Build
```bash
mvn clean compile
```

### Run
```bash
mvn exec:java -Dexec.mainClass="com.wallet.Main"
```
Or run `Main.java` directly from your IDE. A login dialog will appear on startup.

## Testing

JUnit 5 tests cover:
- Successful deposits, withdrawals, and transfers (happy paths)
- Edge cases: null inputs, invalid amounts, insufficient funds, currency mismatches, self-transfers
- Concurrency: bidirectional concurrent transfer test to verify deadlock prevention

## Notes & Limitations

- **CSV fragility** — owner names or fields containing commas will break CSV parsing. A quoted-field CSV parser would fix this.
- **No authentication** — login is username-only; there are no passwords or access controls.
- **`DailyLimitTracker` is a stub** — the class exists as a placeholder; per-wallet daily transaction limits are not yet implemented.
- **No REST or CLI layer** — the application is GUI-only; there is no API exposed.
