package com.firefly.common.cqrs.command;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Test Command Handler: Transfer Money
 * 
 * <p>This demonstrates the consolidated CQRS approach:
 * <ul>
 *   <li>@CommandHandler annotation for configuration and Spring registration</li>
 *   <li>Extends CommandHandler&lt;Command, Result&gt; for automatic type detection</li>
 *   <li>Only implement doHandle() - everything else is automatic</li>
 *   <li>Built-in validation, logging, metrics, error handling</li>
 * </ul>
 */
@CommandHandlerComponent(timeout = 15000, retries = 2, metrics = true)
public class TransferMoneyHandler extends CommandHandler<TransferMoneyCommand, TransferResult> {

    @Override
    protected Mono<TransferResult> doHandle(TransferMoneyCommand command) {
        // Only business logic - validation, logging, metrics handled automatically!
        String transactionId = "TXN-" + System.currentTimeMillis();
        TransferResult result = new TransferResult(
            transactionId,
            command.getFromAccount(),
            command.getToAccount(),
            command.getAmount(),
            "COMPLETED"
        );
        return Mono.just(result);
    }

    // No getCommandType() needed - automatically detected from generics!
}
