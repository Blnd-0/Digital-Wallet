# Digital Wallet

A Java-based digital wallet application that manages user wallets, supports multi-currency balances, and handles deposits, withdrawals, and transfers between wallets. Built with a functional error-handling style using [Vavr](https://www.vavr.io/) and designed with thread-safe transfer operations.

## Features

- **Wallet management** — create wallets with an owner name and currency (USD, IQD, EUR).
- **Deposits & withdrawals** — add or remove funds with validation (positive amounts, sufficient balance).
- **Transfers** — move funds between wallets safely, with checks for:
  - Self-transfers
  - Currency mismatch
  - Insufficient funds
  - Non-existent wallets
- **Transaction history** — every successful operation is recorded; history can be queried per wallet or globally.
- **Functional error handling** — operations return `Either<DomainError, T>` instead of throwing exceptions, making failure cases explicit and chainable.
- **Concurrency-safe transfers** — per-wallet `ReentrantLock`s with consistent lock ordering to prevent deadlocks during concurrent transfers.
- **Logging** — SLF4J + Logback integration for info/warn-level operational logs.

## Tech Stack

- **Java 21**
- **Maven** (build tool)
- **Vavr 0.10.4** — functional types (`Either`, `Option`, immutable `List`)
- **Lombok 1.18.30** — boilerplate reduction (`@Value`, `@Getter`, `@AllArgsConstructor`)
- **SLF4J 2.0.16 + Logback 1.5.12** — logging
- **JUnit 5** & **AssertJ** — testing dependencies (declared)

## Project Structure

```
src/main/java/com/wallet/
├── Main.java                      # Demo entry point exercising the services
├── common/
│   └── DomainError.java           # Enum of domain-level error codes & messages
├── domain/
│   ├── Wallet.java                # Immutable wallet aggregate (balance, owner, currency)
│   ├── Transaction.java           # Immutable transaction record
│   ├── TransactionType.java       # DEPOSIT, WITHDRAW, TRANSFER
│   ├── TransactionStatus.java     # SUCCESS, FAILED
│   └── Currency.java              # USD, IQD, EUR
└── service/
    ├── WalletService.java         # Create wallets, deposit, withdraw
    ├── TransactionService.java    # Save & query transactions
    ├── TransferService.java       # Lock-based wallet-to-wallet transfers
    └── DailyLimitTracker.java     # (planned) per-wallet daily transaction limits
```

## Domain Model

### Wallet
An immutable record (`@Value`) holding `walletId`, `ownerName`, `balance`, `currency`, and `createdAt`. New balances produce a new `Wallet` instance via `withBalance(...)` rather than mutating in place.

### Transaction
An immutable record describing a single operation. Uses Vavr `Option<UUID>` for `fromWalletId` and `toWalletId` because deposits have no source and withdrawals have no destination. Factory methods (`createDeposit`, `createWithdrawal`, `createTransfer`) validate the amount and return `Either<DomainError, Transaction>`.

### DomainError
Enumerates all failure modes: `INSUFFICIENT_FUNDS`, `INVALID_AMOUNT`, `WALLET_NOT_FOUND`, `CURRENCY_MISMATCH`, `NEGATIVE_BALANCE`, `SELF_TRANSFER`, `INVALID_INPUT`, `TRANSACTION_NOT_FOUND`.

## How It Works

Every service operation returns `Either<DomainError, T>`:
- **Right** — the success value (a `Wallet` or `Transaction`).
- **Left** — a `DomainError` describing why it failed.

This lets callers chain operations with `.flatMap(...)` / `.map(...)` and inspect outcomes with `.peek(...)` / `.peekLeft(...)` without throwing or catching exceptions.

For transfers, `TransferService` acquires `ReentrantLock`s for **both** wallets in a deterministic order (lower UUID first) before performing the withdrawal-then-deposit sequence, preventing deadlocks when two transfers between the same pair of wallets run concurrently.

## Getting Started

### Prerequisites
- JDK 21+
- Maven 3.6+

### Build
```bash
mvn clean compile
```

### Run the demo
The `Main` class walks through wallet creation, deposits, withdrawals, valid and invalid transfers, and prints the resulting transaction history.

```bash
mvn exec:java -Dexec.mainClass="com.wallet.Main"
```

…or run `Main.java` directly from your IDE.


## Notes & Limitations

- **In-memory storage** — wallets and transactions are stored in `HashMap`s; data is lost on restart. A persistence layer (DB / repository) is the natural next step.
- **`DailyLimitTracker` is a stub** — the class exists as a placeholder; per-wallet daily transaction limits are not yet implemented.
- **No exposed API** — the project is a library/demo; there is no REST or CLI layer yet.
- **Tests not yet authored** — JUnit 5 and AssertJ are wired into the build, but no test classes are present in `src/test`.
