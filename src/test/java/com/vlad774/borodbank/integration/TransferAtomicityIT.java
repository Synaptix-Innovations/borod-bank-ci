package com.vlad774.borodbank.integration;

import com.vlad774.borodbank.model.Account;
import com.vlad774.borodbank.repository.AccountRepository;
import com.vlad774.borodbank.repository.TransactionRepository;
import com.vlad774.borodbank.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies @Transactional rollback behavior on AccountService write methods.
 *
 * In CI: uses MySQL sidecar (localhost:3306, profile "it").
 * Locally: docker-compose up -d mysql, then:
 *   ./mvnw test -Dtest="*IT" -Dspring.profiles.active=it -DfailIfNoTests=false
 */
@SpringBootTest
@ActiveProfiles("it")
class TransferAtomicityIT {

    @Autowired private AccountService accountService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TransactionRepository transactionRepository;

    @BeforeEach
    void cleanDatabase() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void transferAmount_doesNotDeductSenderBalanceWhenRecipientNotFound() {
        Account sender = accountService.registerAccount("sender_atomic", "pass");
        accountService.deposit(sender, new BigDecimal("500.00"));

        Account senderAfterDeposit = accountRepository.findByUsername("sender_atomic").orElseThrow();

        assertThatThrownBy(() ->
                accountService.transferAmount(senderAfterDeposit, "nonexistent_user", new BigDecimal("200.00"))
        ).isInstanceOf(RuntimeException.class)
                .hasMessage("Recipient account not found");

        Account reloaded = accountRepository.findByUsername("sender_atomic").orElseThrow();
        assertThat(reloaded.getBalance()).isEqualByComparingTo("500.00");
    }

    @Test
    void transferAmount_doesNotCreateTransactionsOnFailure() {
        Account sender = accountService.registerAccount("sender_tx", "pass");
        accountService.deposit(sender, new BigDecimal("300.00"));

        Account senderAfterDeposit = accountRepository.findByUsername("sender_tx").orElseThrow();

        assertThatThrownBy(() ->
                accountService.transferAmount(senderAfterDeposit, "nobody", new BigDecimal("100.00"))
        ).isInstanceOf(RuntimeException.class);

        long txCount = transactionRepository.findByAccountId(senderAfterDeposit.getId())
                .stream()
                .filter(t -> t.getDescription().startsWith("Transfer"))
                .count();
        assertThat(txCount).isZero();
    }
}
