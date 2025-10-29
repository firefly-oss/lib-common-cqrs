# CQRS Integration with Event Sourcing Architecture

**A comprehensive guide for integrating lib-common-cqrs with Event Sourcing patterns in the Firefly ecosystem**

---

## 🎯 Introduction

This document provides detailed guidance on how to effectively integrate **Command Query Responsibility Segregation (CQRS)** patterns with **Event Sourcing** architecture using Firefly's enterprise libraries. The combination of these patterns creates a powerful foundation for building scalable, auditable, and maintainable systems.

---

## 🏗️ Architecture Overview

### Why CQRS + Event Sourcing?

Event Sourcing and CQRS are complementary architectural patterns that solve different but related challenges:

**Event Sourcing Benefits:**
- Complete audit trail of all changes
- Ability to replay events and reconstruct state
- Natural support for temporal queries
- Debugging and compliance capabilities

**CQRS Benefits:**
- Clear separation between write and read operations
- Independent scaling of command and query sides
- Optimized read models for specific use cases
- Reduced coupling in complex business logic

### Combined Architecture Pattern

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                    CQRS + Event Sourcing Architecture                                   │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                         │
│  ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐        │
│  │   Web/API Layer     │    │   Web/API Layer     │    │   Web/API Layer     │        │
│  │                     │    │                     │    │                     │        │
│  │   POST /transfer    │    │   GET /balance      │    │   GET /transactions │        │
│  └─────────────────────┘    └─────────────────────┘    └─────────────────────┘        │
│             │                           │                           │                  │
│             ▼                           ▼                           ▼                  │
│  ┌─────────────────────┐    ┌─────────────────────┐    ┌─────────────────────┐        │
│  │   Command Side      │    │   Query Side        │    │   Query Side        │        │
│  │   (WRITE)           │    │   (READ)            │    │   (READ)            │        │
│  │                     │    │                     │    │                     │        │
│  │   CommandBus        │    │   QueryBus          │    │   QueryBus          │        │
│  │       │             │    │       │             │    │       │             │        │
│  │       ▼             │    │       ▼             │    │       ▼             │        │
│  │   CommandHandler    │    │   QueryHandler      │    │   QueryHandler      │        │
│  └─────────────────────┘    └─────────────────────┘    └─────────────────────┘        │
│             │                           │                           │                  │
│             ▼                           │                           │                  │
│  ┌─────────────────────┐                │                           │                  │
│  │   Event Sourcing    │                │                           │                  │
│  │                     │                │                           │                  │
│  │  • AggregateRoot    │                │                           │                  │
│  │  • Domain Events    │                │                           │                  │
│  │  • Event Store      │                │                           │                  │
│  └─────────────────────┘                │                           │                  │
│             │                           │                           │                  │
│             ▼                           │                           │                  │
│  ┌─────────────────────┐                │                           │                  │
│  │   Event Stream      │────────────────┼───────────────────────────┘                  │
│  │                     │                │                                              │
│  │  • PostgreSQL       │                ▼                                              │
│  │  • JSONB Storage    │    ┌─────────────────────┐                                   │
│  │  • Reactive API     │    │   Read Models       │                                   │
│  └─────────────────────┘    │                     │                                   │
│                              │  • Projections      │                                   │
│                              │  • Materialized     │                                   │
│                              │    Views            │                                   │
│                              │  • Cached Results   │                                   │
│                              └─────────────────────┘                                   │
│                                                                                         │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🚀 Quick Start Integration

### Dependencies Setup

```xml
<!-- Event Sourcing Core -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-eventsourcing</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>

<!-- Event-Driven Architecture (EDA) for Event Publishing -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-eda</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>

<!-- CQRS Framework -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-cqrs</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- Caching Support -->
<dependency>
    <groupId>com.firefly</groupId>
    <artifactId>lib-common-cache</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>

<!-- R2DBC Driver for PostgreSQL -->
<dependency>
    <groupId>io.r2dbc</groupId>
    <artifactId>r2dbc-postgresql</artifactId>
</dependency>

<!-- Spring Boot R2DBC -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-r2dbc</artifactId>
</dependency>

<!-- Kafka Support (Optional) -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>

<!-- RabbitMQ Support (Optional) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### 2. Configuration

```yaml
# application.yml
firefly:
  # Event Sourcing Configuration
  eventsourcing:
    enabled: true
    store:
      type: r2dbc
      schema: events
    snapshots:
      enabled: true
      frequency: 50
    publisher:
      enabled: true
      type: KAFKA  # Options: KAFKA, RABBITMQ, APPLICATION_EVENT, AUTO
      destination-prefix: "banking.events"
      destination-mappings:
        "account.created": "banking.events.accounts"
        "money.transferred": "banking.events.transactions"

  # Event Publishing via EDA
  eda:
    enabled: true
    default-publisher-type: KAFKA
    publishers:
      kafka:
        enabled: true
        default-topic: "banking.events"
        bootstrap-servers: "localhost:9092"
        producer:
          key-serializer: "org.apache.kafka.common.serialization.StringSerializer"
          value-serializer: "org.apache.kafka.common.serialization.StringSerializer"
      rabbitmq:
        enabled: true
        default-exchange: "banking.events"
        connection:
          host: localhost
          port: 5672
          username: guest
          password: guest

  # CQRS Configuration
  cqrs:
    enabled: true
    command:
      timeout: 30s
      retries: 3
      metrics-enabled: true
    query:
      timeout: 15s
      caching-enabled: true
      cache-ttl: 5m

  # Cache Configuration
  cache:
    enabled: true
    provider: redis
    redis:
      host: localhost
      port: 6379

# Database Configuration
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/banking
    username: banking_user
    password: ${DB_PASSWORD}
    pool:
      initial-size: 5
      max-size: 20

# Liquibase for database migrations
spring:
  liquibase:
    enabled: true
    change-log: "classpath:db/changelog/db.changelog-master.yaml"
    default-schema: events
```

#### Database Schema Setup

```sql
-- PostgreSQL Event Store Schema
-- This should be created by the lib-common-eventsourcing auto-configuration
-- or via Liquibase migrations

CREATE SCHEMA IF NOT EXISTS events;

CREATE TABLE events.events (
    event_id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_version BIGINT NOT NULL,
    global_sequence BIGSERIAL UNIQUE,
    event_type VARCHAR(255) NOT NULL,
    event_data JSONB NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uk_events_aggregate_version UNIQUE (aggregate_id, aggregate_version)
);

-- Indexes for performance
CREATE INDEX idx_events_aggregate ON events.events(aggregate_id, aggregate_type);
CREATE INDEX idx_events_global_sequence ON events.events(global_sequence);
CREATE INDEX idx_events_type ON events.events(event_type);
CREATE INDEX idx_events_created_at ON events.events(created_at);
CREATE INDEX idx_events_metadata ON events.events USING gin(metadata);

-- Snapshots table (optional, for performance optimization)
CREATE TABLE events.snapshots (
    snapshot_id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_version BIGINT NOT NULL,
    snapshot_data JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT uk_snapshots_aggregate UNIQUE (aggregate_id, aggregate_type, aggregate_version)
);

CREATE INDEX idx_snapshots_aggregate ON events.snapshots(aggregate_id, aggregate_type);
CREATE INDEX idx_snapshots_version ON events.snapshots(aggregate_id, aggregate_version DESC);
```

#### Supporting Domain Classes

```java
// Supporting domain classes for the banking example

// Transaction Record
public record TransactionRecord(
    UUID transactionId,
    TransactionType type,
    BigDecimal amount,
    String description,
    Instant timestamp,
    BigDecimal balanceAfter
) {}

// Transaction Type Enum
public enum TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER_IN,
    TRANSFER_OUT
}

// Account Status Enum
public enum AccountStatus {
    ACTIVE,
    SUSPENDED,
    CLOSED
}

// Custom Exceptions
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(String message) {
        super(message);
    }
}

// Integration Event for Cross-Bounded Context Communication
@JsonTypeName("integration.transfer.completed")
public record TransferCompletedIntegrationEvent(
    String transferId,
    String sourceAccountId,
    String targetAccountId,
    BigDecimal amount,
    Currency currency,
    String userId,
    String tenantId,
    Instant completedAt
) implements Event {
    
    @Override
    public UUID getAggregateId() {
        // Integration events don't belong to a specific aggregate
        return UUID.randomUUID();
    }
    
    @Override
    public String getEventType() {
        return "integration.transfer.completed";
    }
    
    @Override
    public Instant getEventTimestamp() {
        return completedAt;
    }
    
    @Override
    public Map<String, Object> getMetadata() {
        return Map.of(
            "transferId", transferId,
            "userId", userId,
            "tenantId", tenantId
        );
    }
}
```

---

## 💰 Complete Banking Example

### Domain Model

#### Event Sourced Aggregate

```java
// Account aggregate using Firefly Event Sourcing
import com.firefly.common.eventsourcing.aggregate.AggregateRoot;
import com.firefly.common.eventsourcing.domain.Event;

@Component
public class Account extends AggregateRoot {
    
    private String accountNumber;
    private String customerId;
    private BigDecimal balance;
    private Currency currency;
    private AccountStatus status;
    private List<TransactionRecord> transactions;
    
    // Constructor for new accounts
    public Account(UUID accountId, String customerId, String accountNumber,
                  BigDecimal initialBalance, Currency currency) {
        super(accountId, "Account");
        applyChange(new AccountCreatedEvent(
            accountId, customerId, accountNumber, initialBalance, currency
        ));
    }
    
    // Constructor for reconstruction from events
    public Account(UUID accountId) {
        super(accountId, "Account");
    }
    
    // Business operations that generate events
    public void deposit(BigDecimal amount, String description) {
        validatePositiveAmount(amount);
        
        applyChange(new MoneyDepositedEvent(
            getId(), amount, description, Instant.now()
        ));
    }
    
    public void withdraw(BigDecimal amount, String description) {
        validatePositiveAmount(amount);
        
        if (amount.compareTo(balance) > 0) {
            throw new InsufficientFundsException(
                "Insufficient balance. Available: " + balance + ", Requested: " + amount);
        }
        
        applyChange(new MoneyWithdrawnEvent(
            getId(), amount, description, Instant.now()
        ));
    }
    
    public void transferTo(UUID targetAccountId, BigDecimal amount, String description) {
        validatePositiveAmount(amount);
        
        if (amount.compareTo(balance) > 0) {
            throw new InsufficientFundsException(
                "Insufficient balance for transfer. Available: " + balance + ", Requested: " + amount);
        }
        
        applyChange(new MoneyTransferredEvent(
            getId(), targetAccountId, amount, description, Instant.now()
        ));
    }
    
    public void receiveTransfer(UUID sourceAccountId, BigDecimal amount, String description) {
        applyChange(new MoneyReceivedEvent(
            getId(), sourceAccountId, amount, description, Instant.now()
        ));
    }
    
    // Event handlers for state reconstruction (must be named "on" + EventClassName)
    private void on(AccountCreatedEvent event) {
        this.accountNumber = event.getAccountNumber();
        this.customerId = event.getCustomerId();
        this.balance = event.getInitialBalance();
        this.currency = event.getCurrency();
        this.status = AccountStatus.ACTIVE;
        this.transactions = new ArrayList<>();
    }
    
    private void on(MoneyDepositedEvent event) {
        this.balance = this.balance.add(event.getAmount());
        this.transactions.add(new TransactionRecord(
            UUID.randomUUID(),
            TransactionType.DEPOSIT, 
            event.getAmount(), 
            event.getDescription(), 
            event.getEventTimestamp(),
            this.balance
        ));
    }
    
    private void on(MoneyWithdrawnEvent event) {
        this.balance = this.balance.subtract(event.getAmount());
        this.transactions.add(new TransactionRecord(
            UUID.randomUUID(),
            TransactionType.WITHDRAWAL, 
            event.getAmount(), 
            event.getDescription(), 
            event.getEventTimestamp(),
            this.balance
        ));
    }
    
    private void on(MoneyTransferredEvent event) {
        this.balance = this.balance.subtract(event.getAmount());
        this.transactions.add(new TransactionRecord(
            UUID.randomUUID(),
            TransactionType.TRANSFER_OUT, 
            event.getAmount(), 
            event.getDescription(), 
            event.getEventTimestamp(),
            this.balance
        ));
    }
    
    private void on(MoneyReceivedEvent event) {
        this.balance = this.balance.add(event.getAmount());
        this.transactions.add(new TransactionRecord(
            UUID.randomUUID(),
            TransactionType.TRANSFER_IN, 
            event.getAmount(), 
            event.getDescription(), 
            event.getEventTimestamp(),
            this.balance
        ));
    }
    
    // Helper methods
    private void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }
    
    // Getters
    public String getAccountNumber() { return accountNumber; }
    public String getCustomerId() { return customerId; }
    public BigDecimal getBalance() { return balance; }
    public Currency getCurrency() { return currency; }
    public AccountStatus getStatus() { return status; }
    public List<TransactionRecord> getTransactions() { return new ArrayList<>(transactions); }
}
```

#### Domain Events

```java
// Domain events implementing Firefly Event interface
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.firefly.common.eventsourcing.domain.Event;

// Account Created Event
@JsonTypeName("account.created")
public record AccountCreatedEvent(
    UUID aggregateId,
    String customerId,
    String accountNumber,
    BigDecimal initialBalance,
    Currency currency,
    Instant eventTimestamp
) implements Event {
    
    @Override
    public String getEventType() {
        return "account.created";
    }
    
    @Override
    public UUID getAggregateId() {
        return aggregateId;
    }
    
    @Override
    public Instant getEventTimestamp() {
        return eventTimestamp;
    }
    
    @Override
    public Map<String, Object> getMetadata() {
        return Map.of(
            "accountNumber", accountNumber,
            "currency", currency.getCurrencyCode()
        );
    }
}

// Money Deposited Event
@JsonTypeName("money.deposited")
public record MoneyDepositedEvent(
    UUID aggregateId,
    BigDecimal amount,
    String description,
    Instant eventTimestamp
) implements Event {
    
    @Override
    public String getEventType() {
        return "money.deposited";
    }
    
    @Override
    public UUID getAggregateId() {
        return aggregateId;
    }
    
    @Override
    public Instant getEventTimestamp() {
        return eventTimestamp;
    }
}

// Money Withdrawn Event
@JsonTypeName("money.withdrawn")
public record MoneyWithdrawnEvent(
    UUID aggregateId,
    BigDecimal amount,
    String description,
    Instant eventTimestamp
) implements Event {
    
    @Override
    public String getEventType() {
        return "money.withdrawn";
    }
    
    @Override
    public UUID getAggregateId() {
        return aggregateId;
    }
    
    @Override
    public Instant getEventTimestamp() {
        return eventTimestamp;
    }
}

// Money Transferred Event
@JsonTypeName("money.transferred")
public record MoneyTransferredEvent(
    UUID aggregateId,
    UUID targetAccountId,
    BigDecimal amount,
    String description,
    Instant eventTimestamp
) implements Event {
    
    @Override
    public String getEventType() {
        return "money.transferred";
    }
    
    @Override
    public UUID getAggregateId() {
        return aggregateId;
    }
    
    @Override
    public Instant getEventTimestamp() {
        return eventTimestamp;
    }
    
    @Override
    public Map<String, Object> getMetadata() {
        return Map.of("targetAccountId", targetAccountId.toString());
    }
}

// Money Received Event (for transfer target)
@JsonTypeName("money.received")
public record MoneyReceivedEvent(
    UUID aggregateId,
    UUID sourceAccountId,
    BigDecimal amount,
    String description,
    Instant eventTimestamp
) implements Event {
    
    @Override
    public String getEventType() {
        return "money.received";
    }
    
    @Override
    public UUID getAggregateId() {
        return aggregateId;
    }
    
    @Override
    public Instant getEventTimestamp() {
        return eventTimestamp;
    }
    
    @Override
    public Map<String, Object> getMetadata() {
        return Map.of("sourceAccountId", sourceAccountId.toString());
    }
}
```

### Command Side Implementation

#### Commands with Validation and Authorization

```java
// Transfer money command with comprehensive validation
@Data
@AllArgsConstructor
public class TransferMoneyCommand implements Command<TransferResult> {
    
    @NotNull(message = "Source account ID is required")
    @Pattern(regexp = "^ACC-\\d{6}$", message = "Invalid source account format")
    private final String sourceAccountId;
    
    @NotNull(message = "Target account ID is required")
    @Pattern(regexp = "^ACC-\\d{6}$", message = "Invalid target account format")
    private final String targetAccountId;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @DecimalMax(value = "1000000.00", message = "Amount exceeds maximum limit")
    private final BigDecimal amount;
    
    @NotNull(message = "Currency is required")
    private final Currency currency;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private final String description;
    
    // Custom business validation
    @Override
    public Mono<ValidationResult> customValidate() {
        if (sourceAccountId.equals(targetAccountId)) {
            return Mono.just(ValidationResult.failure("targetAccountId", 
                "Cannot transfer money to the same account"));
        }
        
        // Additional async validation could be added here
        return Mono.just(ValidationResult.success());
    }
    
    // Context-aware authorization with business rules
    @Override
    public Mono<AuthorizationResult> authorize(ExecutionContext context) {
        String userId = context.getUserId();
        String tenantId = context.getTenantId();
        boolean highValueTransfersEnabled = context.getFeatureFlag("high-value-transfers", false);
        
        return validateTransferLimits(userId, amount, tenantId)
            .flatMap(limitsValid -> {
                if (!limitsValid) {
                    return Mono.just(AuthorizationResult.failure("limits", 
                        "Transfer exceeds daily or monthly limits"));
                }
                
                // Check high-value transfer authorization
                if (amount.compareTo(new BigDecimal("10000")) > 0 && !highValueTransfersEnabled) {
                    return Mono.just(AuthorizationResult.failure("amount", 
                        "High-value transfers require premium features"));
                }
                
                return validateAccountOwnership(sourceAccountId, userId, tenantId);
            })
            .map(ownershipValid -> ownershipValid 
                ? AuthorizationResult.success()
                : AuthorizationResult.failure("ownership", "Account not owned by user"));
    }
    
    private Mono<Boolean> validateTransferLimits(String userId, BigDecimal amount, String tenantId) {
        // Implement daily/monthly limit validation
        return Mono.just(true); // Simplified for example
    }
    
    private Mono<Boolean> validateAccountOwnership(String accountId, String userId, String tenantId) {
        // Implement account ownership validation
        return Mono.just(true); // Simplified for example
    }
}

// Deposit money command
@Data
@AllArgsConstructor
public class DepositMoneyCommand implements Command<DepositResult> {
    
    @NotNull(message = "Account ID is required")
    @Pattern(regexp = "^ACC-\\d{6}$", message = "Invalid account format")
    private final String accountId;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @DecimalMax(value = "100000.00", message = "Deposit amount exceeds maximum limit")
    private final BigDecimal amount;
    
    @NotNull(message = "Currency is required")
    private final Currency currency;
    
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private final String description;
    
    @Override
    public Mono<AuthorizationResult> authorize(ExecutionContext context) {
        String userId = context.getUserId();
        String tenantId = context.getTenantId();
        
        return validateAccountOwnership(accountId, userId, tenantId)
            .map(ownershipValid -> ownershipValid 
                ? AuthorizationResult.success()
                : AuthorizationResult.failure("ownership", "Account not owned by user"));
    }
    
    private Mono<Boolean> validateAccountOwnership(String accountId, String userId, String tenantId) {
        // Implementation would check account ownership
        return Mono.just(true); // Simplified for example
    }
}
```

#### Command Handlers with Event Sourcing Integration

```java
// Transfer money command handler integrating with Firefly Event Sourcing
import com.firefly.common.eventsourcing.store.EventStore;
import com.firefly.common.eventsourcing.publisher.EventSourcingPublisher;
import com.firefly.common.eventsourcing.aggregate.AggregateRoot;

@CommandHandlerComponent(
    timeout = 30000,
    retries = 3,
    metrics = true,
    tracing = true,
    description = "Processes money transfers between accounts using event sourcing"
)
public class TransferMoneyHandler extends CommandHandler<TransferMoneyCommand, TransferResult> {
    
    private final EventStore eventStore;
    private final EventSourcingPublisher eventPublisher;
    
    @Autowired
    public TransferMoneyHandler(EventStore eventStore, 
                               EventSourcingPublisher eventPublisher) {
        this.eventStore = eventStore;
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    protected Mono<TransferResult> doHandle(TransferMoneyCommand command, ExecutionContext context) {
        UUID sourceAccountId = UUID.fromString(command.getSourceAccountId());
        UUID targetAccountId = UUID.fromString(command.getTargetAccountId());
        String transferId = UUID.randomUUID().toString();
        
        return performTransfer(sourceAccountId, targetAccountId, command, transferId, context)
            .doOnSuccess(result -> publishTransferEvents(result, context))
            .onErrorMap(this::mapBusinessExceptions);
    }
    
    private Mono<TransferResult> performTransfer(UUID sourceAccountId, 
                                               UUID targetAccountId,
                                               TransferMoneyCommand command,
                                               String transferId,
                                               ExecutionContext context) {
        
        return loadAccount(sourceAccountId)
            .zipWith(loadAccount(targetAccountId))
            .flatMap(tuple -> {
                Account sourceAccount = tuple.getT1();
                Account targetAccount = tuple.getT2();
                
                return executeTransfer(sourceAccount, targetAccount, command, transferId, context);
            });
    }
    
    private Mono<Account> loadAccount(UUID accountId) {
        return eventStore.loadEventStream(accountId, "Account")
            .map(eventStream -> {
                Account account = new Account(accountId);
                account.loadFromHistory(eventStream.getEvents());
                return account;
            })
            .switchIfEmpty(Mono.error(new AccountNotFoundException("Account not found: " + accountId)));
    }
    
    private Mono<TransferResult> executeTransfer(Account sourceAccount, 
                                               Account targetAccount,
                                               TransferMoneyCommand command,
                                               String transferId,
                                               ExecutionContext context) {
        
        // Execute business logic - events are generated internally
        sourceAccount.transferTo(
            targetAccount.getId(), 
            command.getAmount(), 
            command.getDescription()
        );
        
        // Mirror transaction on target account
        targetAccount.receiveTransfer(
            sourceAccount.getId(),
            command.getAmount(), 
            "Transfer from account " + sourceAccount.getAccountNumber()
        );
        
        // Prepare metadata for event publishing
        Map<String, Object> metadata = Map.of(
            "correlationId", context.getRequestId(),
            "userId", context.getUserId(),
            "tenantId", context.getTenantId(),
            "source", context.getSource(),
            "transferId", transferId
        );
        
        // Save both aggregates atomically
        return saveAggregate(sourceAccount, metadata)
            .then(saveAggregate(targetAccount, metadata))
            .thenReturn(TransferResult.builder()
                .transferId(transferId)
                .sourceAccountId(command.getSourceAccountId())
                .targetAccountId(command.getTargetAccountId())
                .amount(command.getAmount())
                .currency(command.getCurrency())
                .status(TransferStatus.COMPLETED)
                .timestamp(Instant.now())
                .build());
    }
    
    private Mono<Void> saveAggregate(Account account, Map<String, Object> metadata) {
        if (!account.hasUncommittedEvents()) {
            return Mono.empty();
        }
        
        return eventStore.appendEvents(
                account.getId(),
                account.getAggregateType(),
                account.getUncommittedEvents(),
                account.getVersion() - account.getUncommittedEventCount(),
                metadata
            )
            .flatMap(eventStream -> {
                // Publish domain events through EDA
                return eventPublisher.publishEvents(eventStream.getEvents())
                    .doOnSuccess(v -> account.markEventsAsCommitted());
            })
            .then();
    }
    
    private void publishTransferEvents(TransferResult result, ExecutionContext context) {
        // Publish high-level integration event for other bounded contexts
        TransferCompletedIntegrationEvent event = new TransferCompletedIntegrationEvent(
            result.getTransferId(),
            result.getSourceAccountId(),
            result.getTargetAccountId(),
            result.getAmount(),
            result.getCurrency(),
            context.getUserId(),
            context.getTenantId(),
            Instant.now()
        );
        
        Map<String, Object> metadata = Map.of(
            "eventType", "integration.transfer.completed",
            "correlationId", context.getRequestId(),
            "source", "banking-service"
        );
        
        eventPublisher.publishDomainEvent(event, metadata)
            .doOnError(error -> log.warn("Failed to publish transfer integration event", error))
            .subscribe(); // Fire and forget for integration events
    }
    
    private Throwable mapBusinessExceptions(Throwable error) {
        if (error instanceof InsufficientFundsException) {
            return new CommandExecutionException("INSUFFICIENT_FUNDS", 
                "Account has insufficient funds for this transfer", error);
        }
        if (error instanceof AccountNotFoundException) {
            return new CommandExecutionException("ACCOUNT_NOT_FOUND", 
                "One or more accounts not found", error);
        }
        return error;
    }
}

// Deposit money command handler
@CommandHandlerComponent(
    timeout = 15000,
    retries = 3,
    metrics = true,
    description = "Processes money deposits using event sourcing"
)
public class DepositMoneyHandler extends CommandHandler<DepositMoneyCommand, DepositResult> {
    
    private final EventStore eventStore;
    private final EventSourcingPublisher eventPublisher;
    
    @Autowired
    public DepositMoneyHandler(EventStore eventStore, EventSourcingPublisher eventPublisher) {
        this.eventStore = eventStore;
        this.eventPublisher = eventPublisher;
    }
    
    @Override
    protected Mono<DepositResult> doHandle(DepositMoneyCommand command, ExecutionContext context) {
        UUID accountId = UUID.fromString(command.getAccountId());
        
        return loadAccount(accountId)
            .flatMap(account -> {
                // Execute business logic
                account.deposit(command.getAmount(), command.getDescription());
                
                // Prepare metadata
                Map<String, Object> metadata = Map.of(
                    "correlationId", context.getRequestId(),
                    "userId", context.getUserId(),
                    "tenantId", context.getTenantId(),
                    "source", context.getSource(),
                    "operationType", "deposit"
                );
                
                // Save aggregate with new events
                return saveAggregate(account, metadata)
                    .thenReturn(DepositResult.builder()
                        .depositId(UUID.randomUUID().toString())
                        .accountId(command.getAccountId())
                        .amount(command.getAmount())
                        .currency(command.getCurrency())
                        .newBalance(account.getBalance())
                        .status(DepositStatus.COMPLETED)
                        .timestamp(Instant.now())
                        .build());
            });
    }
    
    private Mono<Account> loadAccount(UUID accountId) {
        return eventStore.loadEventStream(accountId, "Account")
            .map(eventStream -> {
                Account account = new Account(accountId);
                account.loadFromHistory(eventStream.getEvents());
                return account;
            })
            .switchIfEmpty(Mono.error(new AccountNotFoundException("Account not found: " + accountId)));
    }
    
    private Mono<Void> saveAggregate(Account account, Map<String, Object> metadata) {
        if (!account.hasUncommittedEvents()) {
            return Mono.empty();
        }
        
        return eventStore.appendEvents(
                account.getId(),
                account.getAggregateType(),
                account.getUncommittedEvents(),
                account.getVersion() - account.getUncommittedEventCount(),
                metadata
            )
            .flatMap(eventStream -> {
                // Publish domain events through EDA
                return eventPublisher.publishEvents(eventStream.getEvents())
                    .doOnSuccess(v -> account.markEventsAsCommitted());
            })
            .then();
    }
}
```

### Query Side Implementation

#### Queries with Smart Caching

```java
// Get account balance query with intelligent caching
@Data
@AllArgsConstructor
public class GetAccountBalanceQuery implements Query<AccountBalance> {
    
    @NotBlank(message = "Account ID is required")
    private final String accountId;
    
    @NotNull(message = "Currency is required")
    private final Currency currency;
    
    // Include timestamp for cache invalidation scenarios
    private final Instant asOfDate;
    
    @Override
    public String getCacheKey() {
        // Custom cache key for optimal cache utilization
        return String.format("balance:%s:%s:%s", 
            accountId, 
            currency.getCurrencyCode(),
            asOfDate != null ? asOfDate.toString() : "current"
        );
    }
}

// Transaction history query with pagination
@Data
@AllArgsConstructor
public class GetTransactionHistoryQuery implements Query<TransactionHistory> {
    
    @NotBlank(message = "Account ID is required")
    private final String accountId;
    
    @PastOrPresent(message = "From date cannot be in the future")
    private final LocalDate fromDate;
    
    @PastOrPresent(message = "To date cannot be in the future")
    private final LocalDate toDate;
    
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    private final int pageSize;
    
    @Min(value = 0, message = "Page number must be non-negative")
    private final int page;
    
    @Override
    public String getCacheKey() {
        return String.format("transactions:%s:%s:%s:%d:%d", 
            accountId, fromDate, toDate, pageSize, page);
    }
}
```

#### Query Handlers with Read Model Optimization

```java
// Account balance query handler with event sourcing integration
@QueryHandlerComponent(
    cacheable = true,
    cacheTtl = 300, // 5-minute cache
    cacheKeyFields = {"accountId", "currency", "asOfDate"},
    cacheKeyPrefix = "account_balance",
    autoEvictCache = true,
    evictOnCommands = {
        "TransferMoneyCommand", 
        "DepositMoneyCommand", 
        "WithdrawMoneyCommand"
    },
    metrics = true,
    tracing = true,
    timeout = 10000
)
public class GetAccountBalanceHandler extends QueryHandler<GetAccountBalanceQuery, AccountBalance> {
    
    private final EventStore eventStore;
    private final BalanceProjectionService projectionService;
    
    @Autowired
    public GetAccountBalanceHandler(EventStore eventStore, 
                                   BalanceProjectionService projectionService) {
        this.eventStore = eventStore;
        this.projectionService = projectionService;
    }
    
    @Override
    protected Mono<AccountBalance> doHandle(GetAccountBalanceQuery query, ExecutionContext context) {
        AccountId accountId = AccountId.of(query.getAccountId());
        
        // For current balance, use optimized projection
        if (query.getAsOfDate() == null) {
            return projectionService.getCurrentBalance(accountId, query.getCurrency());
        }
        
        // For historical balance, reconstruct from events
        return reconstructHistoricalBalance(accountId, query.getCurrency(), query.getAsOfDate());
    }
    
    private Mono<AccountBalance> reconstructHistoricalBalance(AccountId accountId, 
                                                            Currency currency, 
                                                            Instant asOfDate) {
        return eventStore.loadAggregateHistory(Account.class, accountId, asOfDate)
            .cast(Account.class)
            .map(account -> AccountBalance.builder()
                .accountId(accountId.getValue())
                .balance(account.getBalance())
                .currency(currency)
                .asOfDate(asOfDate)
                .lastTransactionDate(getLastTransactionDate(account))
                .transactionCount(account.getTransactions().size())
                .build());
    }
    
    private Instant getLastTransactionDate(Account account) {
        return account.getTransactions().stream()
            .map(Transaction::getTimestamp)
            .max(Instant::compareTo)
            .orElse(null);
    }
}

// Transaction history query handler with optimized read model
@QueryHandlerComponent(
    cacheable = true,
    cacheTtl = 600, // 10-minute cache for transaction history
    cacheKeyFields = {"accountId", "fromDate", "toDate", "page", "pageSize"},
    cacheKeyPrefix = "transaction_history",
    metrics = true,
    timeout = 15000,
    autoEvictCache = true,
    evictOnCommands = {
        "TransferMoneyCommand", 
        "DepositMoneyCommand", 
        "WithdrawMoneyCommand"
    }
)
public class GetTransactionHistoryHandler extends QueryHandler<GetTransactionHistoryQuery, TransactionHistory> {
    
    private final TransactionProjectionService projectionService;
    private final EventStore eventStore;
    
    @Autowired
    public GetTransactionHistoryHandler(TransactionProjectionService projectionService,
                                       EventStore eventStore) {
        this.projectionService = projectionService;
        this.eventStore = eventStore;
    }
    
    @Override
    protected Mono<TransactionHistory> doHandle(GetTransactionHistoryQuery query, ExecutionContext context) {
        AccountId accountId = AccountId.of(query.getAccountId());
        
        // Use optimized read model for better performance
        return projectionService.getTransactionHistory(
                accountId,
                query.getFromDate(),
                query.getToDate(),
                query.getPage(),
                query.getPageSize()
            )
            .switchIfEmpty(
                // Fallback to event reconstruction if projection is not available
                reconstructFromEvents(accountId, query)
            );
    }
    
    private Mono<TransactionHistory> reconstructFromEvents(AccountId accountId, 
                                                          GetTransactionHistoryQuery query) {
        Instant fromInstant = query.getFromDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = query.getToDate().atStartOfDay(ZoneOffset.UTC).plusDays(1).toInstant();
        
        return eventStore.loadEvents(accountId.getValue(), fromInstant, toInstant)
            .filter(event -> isTransactionEvent(event))
            .map(this::convertEventToTransaction)
            .collectList()
            .map(transactions -> {
                // Apply pagination
                List<TransactionSummary> paginatedTransactions = transactions.stream()
                    .sorted((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()))
                    .skip((long) query.getPage() * query.getPageSize())
                    .limit(query.getPageSize())
                    .collect(Collectors.toList());
                
                return TransactionHistory.builder()
                    .accountId(query.getAccountId())
                    .transactions(paginatedTransactions)
                    .totalCount(transactions.size())
                    .page(query.getPage())
                    .pageSize(query.getPageSize())
                    .fromDate(query.getFromDate())
                    .toDate(query.getToDate())
                    .build();
            });
    }
    
    private boolean isTransactionEvent(Event event) {
        String eventType = event.getEventType();
        return eventType.equals("MoneyDepositedEvent") ||
               eventType.equals("MoneyWithdrawnEvent") ||
               eventType.equals("MoneyTransferredEvent");
    }
    
    private TransactionSummary convertEventToTransaction(Event event) {
        // Convert domain events to transaction summaries
        // Implementation would handle each event type
        return TransactionSummary.builder()
            .transactionId(UUID.randomUUID().toString())
            .type(getTransactionType(event.getEventType()))
            .amount(extractAmount(event))
            .description(extractDescription(event))
            .timestamp(event.getTimestamp())
            .build();
    }
    
    private TransactionType getTransactionType(String eventType) {
        switch (eventType) {
            case "MoneyDepositedEvent": return TransactionType.DEPOSIT;
            case "MoneyWithdrawnEvent": return TransactionType.WITHDRAWAL;
            case "MoneyTransferredEvent": return TransactionType.TRANSFER_OUT;
            default: return TransactionType.OTHER;
        }
    }
    
    private BigDecimal extractAmount(Event event) {
        // Extract amount from event data
        // Implementation would parse event payload
        return BigDecimal.ZERO; // Simplified
    }
    
    private String extractDescription(Event event) {
        // Extract description from event data
        return "Transaction"; // Simplified
    }
}
```

### Read Model Projections

```java
// Service for maintaining optimized balance projections
@Service
public class BalanceProjectionService {
    
    private final R2dbcEntityTemplate template;
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    
    @EventHandler
    public void handle(MoneyDepositedEvent event) {
        updateBalanceProjection(event.getAccountId(), event.getAmount(), Operation.ADD);
    }
    
    @EventHandler
    public void handle(MoneyWithdrawnEvent event) {
        updateBalanceProjection(event.getAccountId(), event.getAmount(), Operation.SUBTRACT);
    }
    
    @EventHandler
    public void handle(MoneyTransferredEvent event) {
        updateBalanceProjection(event.getAccountId(), event.getAmount(), Operation.SUBTRACT);
    }
    
    public Mono<AccountBalance> getCurrentBalance(AccountId accountId, Currency currency) {
        String cacheKey = "current_balance:" + accountId.getValue() + ":" + currency.getCurrencyCode();
        
        return redisTemplate.opsForValue()
            .get(cacheKey)
            .map(this::deserializeBalance)
            .switchIfEmpty(
                loadBalanceFromDatabase(accountId, currency)
                    .flatMap(balance -> 
                        redisTemplate.opsForValue()
                            .set(cacheKey, serializeBalance(balance), Duration.ofMinutes(5))
                            .thenReturn(balance)
                    )
            );
    }
    
    private Mono<Void> updateBalanceProjection(AccountId accountId, BigDecimal amount, Operation operation) {
        // Update database projection
        return template.update(BalanceProjection.class)
            .matching(Query.query(Criteria.where("accountId").is(accountId.getValue())))
            .apply(operation == Operation.ADD ? 
                Update.update("balance", amount) : 
                Update.update("balance", amount.negate()))
            .then()
            .doOnSuccess(v -> invalidateBalanceCache(accountId));
    }
    
    private void invalidateBalanceCache(AccountId accountId) {
        String pattern = "current_balance:" + accountId.getValue() + ":*";
        redisTemplate.keys(pattern)
            .flatMap(redisTemplate::delete)
            .subscribe();
    }
    
    private Mono<AccountBalance> loadBalanceFromDatabase(AccountId accountId, Currency currency) {
        return template.select(BalanceProjection.class)
            .matching(Query.query(Criteria.where("accountId").is(accountId.getValue())))
            .one()
            .map(projection -> AccountBalance.builder()
                .accountId(accountId.getValue())
                .balance(projection.getBalance())
                .currency(currency)
                .asOfDate(null)
                .lastTransactionDate(projection.getLastUpdated())
                .transactionCount(projection.getTransactionCount())
                .build());
    }
    
    private enum Operation { ADD, SUBTRACT }
}
```

### REST API Integration

```java
// REST controller integrating CQRS with Event Sourcing
@RestController
@RequestMapping("/api/banking")
@Validated
public class BankingController {
    
    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final ExecutionContextService executionContextService;
    
    @Autowired
    public BankingController(CommandBus commandBus, 
                           QueryBus queryBus,
                           ExecutionContextService executionContextService) {
        this.commandBus = commandBus;
        this.queryBus = queryBus;
        this.executionContextService = executionContextService;
    }
    
    @PostMapping("/accounts/{accountId}/deposit")
    public Mono<ResponseEntity<DepositResult>> depositMoney(
            @PathVariable String accountId,
            @Valid @RequestBody DepositRequest request,
            @RequestHeader("Authorization") String authToken,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            ServerHttpRequest httpRequest) {
        
        return executionContextService.buildContext(authToken, tenantId, correlationId, httpRequest)
            .flatMap(context -> {
                DepositMoneyCommand command = new DepositMoneyCommand(
                    accountId,
                    request.getAmount(),
                    request.getCurrency(),
                    request.getDescription()
                );
                
                return commandBus.send(command, context);
            })
            .map(result -> ResponseEntity.ok(result))
            .onErrorResume(this::handleDepositError);
    }
    
    @PostMapping("/transfers")
    public Mono<ResponseEntity<TransferResult>> transferMoney(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader("Authorization") String authToken,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantId,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            ServerHttpRequest httpRequest) {
        
        return executionContextService.buildContext(authToken, tenantId, correlationId, httpRequest)
            .flatMap(context -> {
                TransferMoneyCommand command = new TransferMoneyCommand(
                    request.getSourceAccountId(),
                    request.getTargetAccountId(),
                    request.getAmount(),
                    request.getCurrency(),
                    request.getDescription()
                );
                
                return commandBus.send(command, context);
            })
            .map(result -> ResponseEntity.accepted().body(result))
            .onErrorResume(this::handleTransferError);
    }
    
    @GetMapping("/accounts/{accountId}/balance")
    public Mono<ResponseEntity<AccountBalance>> getAccountBalance(
            @PathVariable String accountId,
            @RequestParam(defaultValue = "USD") String currency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant asOfDate,
            @RequestHeader("Authorization") String authToken) {
        
        return executionContextService.buildContext(authToken, null, null, null)
            .flatMap(context -> {
                GetAccountBalanceQuery query = new GetAccountBalanceQuery(
                    accountId,
                    Currency.getInstance(currency),
                    asOfDate
                );
                
                return queryBus.query(query, context);
            })
            .map(balance -> ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)))
                .body(balance))
            .onErrorResume(this::handleBalanceQueryError);
    }
    
    @GetMapping("/accounts/{accountId}/transactions")
    public Mono<ResponseEntity<TransactionHistory>> getTransactionHistory(
            @PathVariable String accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestHeader("Authorization") String authToken) {
        
        return executionContextService.buildContext(authToken, null, null, null)
            .flatMap(context -> {
                GetTransactionHistoryQuery query = new GetTransactionHistoryQuery(
                    accountId, fromDate, toDate, pageSize, page
                );
                
                return queryBus.query(query, context);
            })
            .map(history -> ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(10)))
                .body(history))
            .onErrorResume(this::handleTransactionHistoryError);
    }
    
    // Error handling methods
    private Mono<ResponseEntity<DepositResult>> handleDepositError(Throwable error) {
        if (error instanceof ValidationException) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        if (error instanceof AuthorizationException) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
        }
        if (error instanceof CommandExecutionException) {
            CommandExecutionException cee = (CommandExecutionException) error;
            if ("ACCOUNT_NOT_FOUND".equals(cee.getErrorCode())) {
                return Mono.just(ResponseEntity.notFound().build());
            }
        }
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
    
    private Mono<ResponseEntity<TransferResult>> handleTransferError(Throwable error) {
        if (error instanceof ValidationException) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        if (error instanceof AuthorizationException) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
        }
        if (error instanceof CommandExecutionException) {
            CommandExecutionException cee = (CommandExecutionException) error;
            if ("INSUFFICIENT_FUNDS".equals(cee.getErrorCode())) {
                return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build());
            }
            if ("ACCOUNT_NOT_FOUND".equals(cee.getErrorCode())) {
                return Mono.just(ResponseEntity.notFound().build());
            }
        }
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
    
    private Mono<ResponseEntity<AccountBalance>> handleBalanceQueryError(Throwable error) {
        if (error instanceof ValidationException) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        if (error instanceof AuthorizationException) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
        }
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
    
    private Mono<ResponseEntity<TransactionHistory>> handleTransactionHistoryError(Throwable error) {
        if (error instanceof ValidationException) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        if (error instanceof AuthorizationException) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
        }
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
}
```

---

## 📊 Observability & Monitoring

### Comprehensive Metrics Integration

The combined CQRS + Event Sourcing architecture provides extensive observability:

#### CQRS Metrics (via lib-common-cqrs)
```bash
# Command processing metrics
curl http://localhost:8080/actuator/cqrs/commands

# Query performance and cache metrics  
curl http://localhost:8080/actuator/cqrs/queries

# Handler registry information
curl http://localhost:8080/actuator/cqrs/handlers
```

#### Event Sourcing Metrics (via lib-common-eda)
```bash
# Event store performance metrics
curl http://localhost:8080/actuator/events

# Event publishing metrics
curl http://localhost:8080/actuator/events/publishers

# Snapshot performance metrics  
curl http://localhost:8080/actuator/events/snapshots
```

#### Combined Metrics Response Example
```json
{
  "cqrs": {
    "commands": {
      "total_processed": 15420,
      "success_rate": 98.7,
      "avg_processing_time_ms": 125.3,
      "by_type": {
        "TransferMoneyCommand": {
          "processed": 8950,
          "failed": 12,
          "avg_processing_time_ms": 145.2
        },
        "DepositMoneyCommand": {
          "processed": 6470,
          "failed": 3,
          "avg_processing_time_ms": 95.8
        }
      }
    },
    "queries": {
      "total_processed": 45280,
      "cache_hit_rate": 87.4,
      "avg_processing_time_ms": 25.1
    }
  },
  "event_sourcing": {
    "event_store": {
      "events_persisted": 124580,
      "avg_persist_time_ms": 15.2,
      "events_replayed": 3420
    },
    "snapshots": {
      "snapshots_created": 1245,
      "avg_snapshot_time_ms": 85.3
    }
  }
}
```

---

## 🔧 Configuration Best Practices

### Production Configuration

```yaml
# application-prod.yml
firefly:
  # Event Sourcing Configuration
  eda:
    event-store:
      enabled: true
      database-type: postgresql
      connection-pool-size: 20
      schema: banking_events
      snapshots:
        enabled: true
        frequency: 50  # More frequent snapshots in production
        cleanup-enabled: true
      publishers:
        kafka:
          enabled: true
          topic-prefix: "banking.prod.events"
          retry-attempts: 5
          
  # CQRS Configuration
  cqrs:
    enabled: true
    command:
      timeout: 45s  # Longer timeout for production
      retries: 5    # More retries for resilience
      backoff-ms: 2000
      metrics-enabled: true
      validation-enabled: true
    query:
      timeout: 20s
      caching-enabled: true
      cache-ttl: 10m  # Longer cache in production
      metrics-enabled: true
    authorization:
      enabled: true
      custom:
        enabled: true
        timeout-ms: 10000

  # Cache Configuration
  cache:
    enabled: true
    provider: redis
    default-ttl: 600s
    redis:
      host: redis-cluster.banking.internal
      port: 6379
      password: ${REDIS_PASSWORD}
      timeout: 5000ms
      database: 0
      ssl: true

# Database Configuration
spring:
  r2dbc:
    url: r2dbc:postgresql://postgres-cluster.banking.internal:5432/banking_prod
    username: banking_app
    password: ${DATABASE_PASSWORD}
    pool:
      initial-size: 10
      max-size: 50
      max-idle-time: PT30M

# Observability
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,cqrs,events
  endpoint:
    health:
      show-details: always
    cqrs:
      enabled: true
      cache:
        time-to-live: 10s
  metrics:
    export:
      prometheus:
        enabled: true
        step: 10s
    distribution:
      percentiles-histogram:
        "[firefly.cqrs]": true
        "[firefly.events]": true
      percentiles:
        "[firefly.cqrs]": 0.5,0.95,0.99
        "[firefly.events]": 0.5,0.95,0.99

# Logging
logging:
  level:
    com.firefly.cqrs: INFO
    com.firefly.events: INFO
    com.banking: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId}] %logger{36} - %msg%n"
```

---

## 🧪 Testing Strategies

### Integration Testing

```java
@SpringBootTest
@Testcontainers
class BankingIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("banking_test")
            .withUsername("test")
            .withPassword("test");
    
    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
    
    @Autowired
    private CommandBus commandBus;
    
    @Autowired
    private QueryBus queryBus;
    
    @Autowired
    private EventStore eventStore;
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> 
            "r2dbc:postgresql://localhost:" + postgres.getFirstMappedPort() + "/banking_test");
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("firefly.cache.redis.host", redis::getHost);
        registry.add("firefly.cache.redis.port", redis::getFirstMappedPort);
    }
    
    @Test
    void shouldCompleteMoneyTransferWithEventSourcing() {
        // Given
        String sourceAccountId = "ACC-123456";
        String targetAccountId = "ACC-789012";
        BigDecimal transferAmount = new BigDecimal("1000.00");
        
        ExecutionContext context = ExecutionContext.builder()
            .userId("user-123")
            .tenantId("tenant-abc")
            .source("integration-test")
            .build();
            
        // Create initial deposits
        DepositMoneyCommand sourceDeposit = new DepositMoneyCommand(
            sourceAccountId, new BigDecimal("5000.00"), Currency.getInstance("USD"), "Initial deposit"
        );
        DepositMoneyCommand targetDeposit = new DepositMoneyCommand(
            targetAccountId, new BigDecimal("1000.00"), Currency.getInstance("USD"), "Initial deposit"
        );
        
        StepVerifier.create(
            commandBus.send(sourceDeposit, context)
                .then(commandBus.send(targetDeposit, context))
        ).verifyComplete();
        
        // When - Transfer money
        TransferMoneyCommand transferCommand = new TransferMoneyCommand(
            sourceAccountId, targetAccountId, transferAmount, Currency.getInstance("USD"), "Test transfer"
        );
        
        StepVerifier.create(commandBus.send(transferCommand, context))
            .assertNext(result -> {
                assertThat(result.getTransferId()).isNotNull();
                assertThat(result.getStatus()).isEqualTo(TransferStatus.COMPLETED);
                assertThat(result.getAmount()).isEqualTo(transferAmount);
            })
            .verifyComplete();
        
        // Then - Verify balances
        GetAccountBalanceQuery sourceBalanceQuery = new GetAccountBalanceQuery(
            sourceAccountId, Currency.getInstance("USD"), null
        );
        GetAccountBalanceQuery targetBalanceQuery = new GetAccountBalanceQuery(
            targetAccountId, Currency.getInstance("USD"), null  
        );
        
        StepVerifier.create(queryBus.query(sourceBalanceQuery, context))
            .assertNext(balance -> 
                assertThat(balance.getBalance()).isEqualTo(new BigDecimal("4000.00"))
            )
            .verifyComplete();
            
        StepVerifier.create(queryBus.query(targetBalanceQuery, context))
            .assertNext(balance -> 
                assertThat(balance.getBalance()).isEqualTo(new BigDecimal("2000.00"))
            )
            .verifyComplete();
            
        // And - Verify event history
        AccountId sourceId = AccountId.of(sourceAccountId);
        StepVerifier.create(eventStore.loadEvents(sourceId.getValue()))
            .expectNextCount(2) // AccountCreated + MoneyTransferred
            .verifyComplete();
    }
    
    @Test
    void shouldCacheQueryResults() {
        // Given
        String accountId = "ACC-999888";
        ExecutionContext context = ExecutionContext.builder()
            .userId("user-test")
            .tenantId("tenant-test")
            .build();
            
        DepositMoneyCommand deposit = new DepositMoneyCommand(
            accountId, new BigDecimal("2500.00"), Currency.getInstance("USD"), "Cache test deposit"
        );
        
        commandBus.send(deposit, context).block();
        
        GetAccountBalanceQuery balanceQuery = new GetAccountBalanceQuery(
            accountId, Currency.getInstance("USD"), null
        );
        
        // When - First query (cache miss)
        Mono<AccountBalance> firstQuery = queryBus.query(balanceQuery, context);
        Mono<AccountBalance> secondQuery = queryBus.query(balanceQuery, context);
        
        // Then - Both queries should return same result, second from cache
        StepVerifier.create(Mono.zip(firstQuery, secondQuery))
            .assertNext(tuple -> {
                AccountBalance first = tuple.getT1();
                AccountBalance second = tuple.getT2();
                assertThat(first.getBalance()).isEqualTo(second.getBalance());
                assertThat(first.getAccountId()).isEqualTo(accountId);
            })
            .verifyComplete();
    }
}
```

### Unit Testing Command Handlers

```java
@ExtendWith(MockitoExtension.class)
class TransferMoneyHandlerTest {
    
    @Mock
    private EventStore eventStore;
    
    @Mock
    private EventSourcingPublisher eventPublisher;
    
    @Mock
    private TransferIdGenerator transferIdGenerator;
    
    @InjectMocks
    private TransferMoneyHandler handler;
    
    @Test
    void shouldTransferMoneySuccessfully() {
        // Given
        String transferId = "TRF-123456";
        AccountId sourceAccountId = AccountId.of("ACC-111111");
        AccountId targetAccountId = AccountId.of("ACC-222222");
        
        Account sourceAccount = new Account(sourceAccountId);
        sourceAccount.deposit(new BigDecimal("5000.00"), "Initial balance");
        
        Account targetAccount = new Account(targetAccountId);
        targetAccount.deposit(new BigDecimal("1000.00"), "Initial balance");
        
        TransferMoneyCommand command = new TransferMoneyCommand(
            sourceAccountId.getValue(),
            targetAccountId.getValue(),
            new BigDecimal("1500.00"),
            Currency.getInstance("USD"),
            "Test transfer"
        );
        
        when(transferIdGenerator.generate()).thenReturn(transferId);
        when(eventStore.loadAggregate(Account.class, sourceAccountId))
            .thenReturn(Mono.just(sourceAccount));
        when(eventStore.loadAggregate(Account.class, targetAccountId))
            .thenReturn(Mono.just(targetAccount));
        when(eventStore.saveAggregate(any(Account.class)))
            .thenReturn(Mono.empty());
        
        ExecutionContext context = ExecutionContext.builder()
            .userId("user-123")
            .tenantId("tenant-abc")
            .build();
        
        // When
        Mono<TransferResult> result = handler.doHandle(command, context);
        
        // Then
        StepVerifier.create(result)
            .assertNext(transferResult -> {
                assertThat(transferResult.getTransferId()).isEqualTo(transferId);
                assertThat(transferResult.getStatus()).isEqualTo(TransferStatus.COMPLETED);
                assertThat(transferResult.getAmount()).isEqualTo(new BigDecimal("1500.00"));
                assertThat(transferResult.getSourceAccountId()).isEqualTo(sourceAccountId.getValue());
                assertThat(transferResult.getTargetAccountId()).isEqualTo(targetAccountId.getValue());
            })
            .verifyComplete();
            
        verify(eventStore).saveAggregate(sourceAccount);
        verify(eventStore).saveAggregate(targetAccount);
        
        // Verify source account balance
        assertThat(sourceAccount.getBalance()).isEqualTo(new BigDecimal("3500.00"));
        
        // Verify target account balance
        assertThat(targetAccount.getBalance()).isEqualTo(new BigDecimal("2500.00"));
    }
    
    @Test
    void shouldFailTransferWhenInsufficientFunds() {
        // Given
        AccountId sourceAccountId = AccountId.of("ACC-111111");
        AccountId targetAccountId = AccountId.of("ACC-222222");
        
        Account sourceAccount = new Account(sourceAccountId);
        sourceAccount.deposit(new BigDecimal("500.00"), "Low balance");
        
        Account targetAccount = new Account(targetAccountId);
        
        TransferMoneyCommand command = new TransferMoneyCommand(
            sourceAccountId.getValue(),
            targetAccountId.getValue(),
            new BigDecimal("1000.00"), // More than available
            Currency.getInstance("USD"),
            "Test insufficient funds"
        );
        
        when(eventStore.loadAggregate(Account.class, sourceAccountId))
            .thenReturn(Mono.just(sourceAccount));
        when(eventStore.loadAggregate(Account.class, targetAccountId))
            .thenReturn(Mono.just(targetAccount));
        
        ExecutionContext context = ExecutionContext.builder().build();
        
        // When & Then
        StepVerifier.create(handler.doHandle(command, context))
            .expectError(CommandExecutionException.class)
            .verify();
    }
}
```

---

## 🚀 Performance Optimization

### Caching Strategies

#### 1. Multi-Level Caching
```yaml
firefly:
  cache:
    enabled: true
    provider: redis
    default-ttl: 300s
    
    # Local cache for frequently accessed data
    caffeine:
      default:
        maximum-size: 1000
        expire-after-write: PT2M
        
    # Distributed cache for shared data
    redis:
      host: redis-cluster
      port: 6379
      timeout: 2000ms
```

#### 2. Cache-Aside Pattern for Read Models
```java
@Service
public class BalanceReadModelService {
    
    public Mono<AccountBalance> getBalance(String accountId, Currency currency) {
        String cacheKey = "balance:" + accountId + ":" + currency.getCurrencyCode();
        
        return cacheManager.get(cacheKey, AccountBalance.class)
            .switchIfEmpty(
                loadBalanceFromProjection(accountId, currency)
                    .flatMap(balance -> 
                        cacheManager.put(cacheKey, balance, Duration.ofMinutes(5))
                            .thenReturn(balance)
                    )
            );
    }
}
```

### Event Store Optimization

#### 1. Snapshot Configuration
```java
@Configuration
public class EventSourcingConfig {
    
    @Bean
    public SnapshotPolicy snapshotPolicy() {
        return SnapshotPolicy.builder()
            .frequency(50) // Snapshot every 50 events
            .asyncEnabled(true)
            .compressionEnabled(true)
            .build();
    }
}
```

#### 2. Database Indexing
```sql
-- Optimize event queries
CREATE INDEX idx_events_aggregate_version ON events(aggregate_id, version);
CREATE INDEX idx_events_aggregate_timestamp ON events(aggregate_id, created_at);

-- Optimize projection queries  
CREATE INDEX idx_balance_projection_account ON balance_projections(account_id);
CREATE INDEX idx_transaction_history_account_date ON transaction_projections(account_id, transaction_date);
```

### Query Performance

#### 1. Read Model Projections
```java
@EventHandler
public class TransactionProjectionHandler {
    
    @EventHandler
    public void handle(MoneyDepositedEvent event) {
        TransactionProjection projection = TransactionProjection.builder()
            .accountId(event.getAccountId())
            .transactionId(UUID.randomUUID().toString())
            .type(TransactionType.DEPOSIT)
            .amount(event.getAmount())
            .description(event.getDescription())
            .timestamp(event.getTimestamp())
            .build();
            
        transactionProjectionRepository.save(projection)
            .subscribe();
    }
}
```

---

## 🔧 Best Practices Summary

### Command Side Best Practices

1. **Keep Commands Simple**: Commands should represent clear business intentions
2. **Validate Early**: Use both annotation-based and custom validation
3. **Authorization at Command Level**: Implement business rules in `authorize()` method
4. **Idempotent Operations**: Design commands to be safely retryable
5. **Event-First Design**: Let business operations generate events naturally

### Query Side Best Practices

1. **Optimize for Read Patterns**: Design projections for specific query needs
2. **Cache Aggressively**: Use intelligent caching with proper invalidation
3. **Separate Read Models**: Use different models for different views
4. **Async Projections**: Update read models asynchronously from events
5. **Pagination**: Always paginate large result sets

### Integration Best Practices

1. **ExecutionContext Everywhere**: Propagate context for traceability
2. **Comprehensive Monitoring**: Use both CQRS and Event Sourcing metrics
3. **Error Boundaries**: Handle errors at appropriate architectural boundaries
4. **Graceful Degradation**: Provide fallbacks when projections are unavailable
5. **Testing Strategy**: Test both command and query sides independently

---

## 📚 Additional Resources

- **[CQRS Framework Documentation](../README.md)** - Complete CQRS library reference
- **[Event Sourcing Library Documentation](../../lib-common-eda/docs/)** - Event Sourcing implementation details  
- **[Domain Events Integration](../../lib-common-domain/docs/)** - Multi-messaging event publishing
- **[Cache Configuration Guide](../../lib-common-cache/docs/)** - Advanced caching strategies

---

*This integration guide demonstrates how to build robust, scalable applications using CQRS and Event Sourcing patterns with the Firefly enterprise libraries. The combination provides complete audit trails, optimized read performance, and clear separation of concerns.*