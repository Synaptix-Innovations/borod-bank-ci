package com.vlad774.borodbank;

import com.vlad774.borodbank.model.Account;
import com.vlad774.borodbank.model.Transaction;
import com.vlad774.borodbank.repository.AccountRepository;
import com.vlad774.borodbank.repository.TransactionRepository;
import com.vlad774.borodbank.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests — run against a real MySQL 8.0 instance.
 *
 * In CI: MySQL sidecar container in Jenkins pod (localhost:3306).
 * Locally: docker-compose up -d mysql, then:
 *   mvn test -Dtest="*IT" -Dspring.profiles.active=it -DfailIfNoTests=false
 *
 * Excluded from default mvn test run via maven-surefire-plugin configuration in pom.xml.
 */
@SpringBootTest
@ActiveProfiles("it")
class AccountServiceIT {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void cleanDatabase() {
        // Transactions reference accounts — delete transactions first (FK constraint)
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void shouldRegisterAccountWithZeroBalance() {
        Account account = accountService.registerAccount("alice", "password123");

        assertNotNull(account.getId());
        assertEquals("alice", account.getUsername());
        assertEquals(0, BigDecimal.ZERO.compareTo(account.getBalance()));
    }

    @Test
    void shouldRejectDuplicateUsername() {
        accountService.registerAccount("bob", "password123");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.registerAccount("bob", "otherpassword"));
        assertEquals("Username already exists", ex.getMessage());
    }

    @Test
    void shouldDepositAndPersistTransaction() {
        Account account = accountService.registerAccount("carol", "password123");
        accountService.deposit(account, new BigDecimal("100.00"));

        Account updated = accountRepository.findByUsername("carol").orElseThrow();
        assertEquals(0, new BigDecimal("100.00").compareTo(updated.getBalance()));

        List<Transaction> transactions = transactionRepository.findByAccountId(updated.getId());
        assertEquals(1, transactions.size());
        assertEquals("Deposit", transactions.get(0).getDescription());
        assertEquals(0, new BigDecimal("100.00").compareTo(transactions.get(0).getAmount()));
    }

    @Test
    void shouldWithdrawAndPersistTransaction() {
        Account account = accountService.registerAccount("dave", "password123");
        accountService.deposit(account, new BigDecimal("200.00"));

        Account afterDeposit = accountRepository.findByUsername("dave").orElseThrow();
        accountService.withdraw(afterDeposit, new BigDecimal("50.00"));

        Account updated = accountRepository.findByUsername("dave").orElseThrow();
        assertEquals(0, new BigDecimal("150.00").compareTo(updated.getBalance()));

        List<Transaction> transactions = transactionRepository.findByAccountId(updated.getId());
        assertEquals(2, transactions.size());
    }

    @Test
    void shouldRejectWithdrawWhenInsufficientFunds() {
        Account account = accountService.registerAccount("eve", "password123");
        accountService.deposit(account, new BigDecimal("30.00"));

        Account afterDeposit = accountRepository.findByUsername("eve").orElseThrow();
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> accountService.withdraw(afterDeposit, new BigDecimal("100.00")));
        assertEquals("Insufficient funds", ex.getMessage());

        // Balance must remain unchanged after failed withdraw
        Account unchanged = accountRepository.findByUsername("eve").orElseThrow();
        assertEquals(0, new BigDecimal("30.00").compareTo(unchanged.getBalance()));
    }

    @Test
    void shouldTransferBetweenAccountsAndRecordBothTransactions() {
        Account sender = accountService.registerAccount("frank", "password123");
        Account receiver = accountService.registerAccount("grace", "password123");
        accountService.deposit(sender, new BigDecimal("500.00"));

        Account senderAfterDeposit = accountRepository.findByUsername("frank").orElseThrow();
        accountService.transferAmount(senderAfterDeposit, "grace", new BigDecimal("200.00"));

        Account updatedSender = accountRepository.findByUsername("frank").orElseThrow();
        Account updatedReceiver = accountRepository.findByUsername("grace").orElseThrow();

        assertEquals(0, new BigDecimal("300.00").compareTo(updatedSender.getBalance()));
        assertEquals(0, new BigDecimal("200.00").compareTo(updatedReceiver.getBalance()));

        // Transfer generates one transaction per side
        List<Transaction> senderTx = transactionRepository.findByAccountId(updatedSender.getId());
        List<Transaction> receiverTx = transactionRepository.findByAccountId(updatedReceiver.getId());

        assertEquals(2, senderTx.size()); // deposit + transfer out
        assertEquals(1, receiverTx.size()); // transfer in

        assertTrue(senderTx.stream().anyMatch(t -> t.getDescription().startsWith("Transfer Out")));
        assertTrue(receiverTx.stream().anyMatch(t -> t.getDescription().startsWith("Transfer In")));
    }

    @Test
    void shouldReturnFullTransactionHistory() {
        Account account = accountService.registerAccount("henry", "password123");
        accountService.deposit(account, new BigDecimal("100.00"));

        Account afterDeposit = accountRepository.findByUsername("henry").orElseThrow();
        accountService.deposit(afterDeposit, new BigDecimal("50.00"));

        Account afterSecondDeposit = accountRepository.findByUsername("henry").orElseThrow();
        accountService.withdraw(afterSecondDeposit, new BigDecimal("30.00"));

        Account final_ = accountRepository.findByUsername("henry").orElseThrow();
        List<Transaction> history = accountService.getTransactionHistory(final_);

        assertEquals(3, history.size());
    }
}
