package com.vlad774.borodbank.unit.service;

import com.vlad774.borodbank.model.Account;
import com.vlad774.borodbank.model.Transaction;
import com.vlad774.borodbank.repository.AccountRepository;
import com.vlad774.borodbank.repository.TransactionRepository;
import com.vlad774.borodbank.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AccountService accountService;

    // --- registerAccount ---

    @Test
    void registerAccount_savesAccountWithEncodedPasswordAndZeroBalance() {
        when(accountRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("$2a$hashed");
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.registerAccount("alice", "secret");

        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getPassword()).isEqualTo("$2a$hashed");
        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void registerAccount_throwsWhenUsernameAlreadyExists() {
        when(accountRepository.findByUsername("bob")).thenReturn(Optional.of(new Account()));

        assertThatThrownBy(() -> accountService.registerAccount("bob", "pass"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Username already exists");

        verify(accountRepository, never()).save(any());
    }

    // --- deposit ---

    @Test
    void deposit_addsAmountToBalance() {
        Account account = accountWithBalance("100.00");
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        accountService.deposit(account, new BigDecimal("50.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("150.00");
    }

    @Test
    void deposit_savesTransactionWithCorrectDescriptionAndAmount() {
        Account account = accountWithBalance("0.00");
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        accountService.deposit(account, new BigDecimal("200.00"));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("Deposit");
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("200.00");
    }

    // --- withdraw ---

    @Test
    void withdraw_subtractsAmountFromBalance() {
        Account account = accountWithBalance("300.00");
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        accountService.withdraw(account, new BigDecimal("100.00"));

        assertThat(account.getBalance()).isEqualByComparingTo("200.00");
    }

    @Test
    void withdraw_savesTransactionWithCorrectDescription() {
        Account account = accountWithBalance("300.00");
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        accountService.withdraw(account, new BigDecimal("100.00"));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getDescription()).isEqualTo("Withdrawal");
    }

    @Test
    void withdraw_throwsWhenBalanceInsufficient() {
        Account account = accountWithBalance("30.00");

        assertThatThrownBy(() -> accountService.withdraw(account, new BigDecimal("100.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Insufficient funds");
    }

    @Test
    void withdraw_doesNotModifyBalanceOrSaveOnInsufficientFunds() {
        Account account = accountWithBalance("30.00");

        assertThatThrownBy(() -> accountService.withdraw(account, new BigDecimal("100.00")))
                .isInstanceOf(RuntimeException.class);

        assertThat(account.getBalance()).isEqualByComparingTo("30.00");
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    // --- transferAmount ---

    @Test
    void transferAmount_throwsWhenSenderHasInsufficientFunds() {
        Account sender = accountWithBalance("50.00");

        assertThatThrownBy(() -> accountService.transferAmount(sender, "bob", new BigDecimal("100.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Insufficient funds");

        verify(accountRepository, never()).save(any());
    }

    @Test
    void transferAmount_throwsWhenRecipientNotFound() {
        Account sender = accountWithBalance("500.00");
        when(accountRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.transferAmount(sender, "nonexistent", new BigDecimal("100.00")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Recipient account not found");
    }

    @Test
    void transferAmount_updatesBothBalancesAndSavesFourRecords() {
        Account sender = accountWithBalance("500.00");
        Account receiver = accountWithBalance("0.00");
        when(accountRepository.findByUsername("receiver")).thenReturn(Optional.of(receiver));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        accountService.transferAmount(sender, "receiver", new BigDecimal("200.00"));

        assertThat(sender.getBalance()).isEqualByComparingTo("300.00");
        assertThat(receiver.getBalance()).isEqualByComparingTo("200.00");
        verify(accountRepository, times(2)).save(any());
        verify(transactionRepository, times(2)).save(any());
    }

    // --- helper ---

    private Account accountWithBalance(String amount) {
        Account account = new Account();
        account.setBalance(new BigDecimal(amount));
        return account;
    }
}
