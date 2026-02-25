package com.vlad774.borodbank.unit.controller;

import com.vlad774.borodbank.config.SecurityConfig;
import com.vlad774.borodbank.controller.BankController;
import com.vlad774.borodbank.model.Account;
import com.vlad774.borodbank.service.AccountService;
import com.vlad774.borodbank.service.FeatureFlagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BankController.class)
@Import(SecurityConfig.class)
class BankControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AccountService accountService;
    @MockBean private FeatureFlagService featureFlagService;

    // --- Public routes ---

    @Test
    void login_isPublicAndReturnsOk() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void register_isPublicAndReturnsOk() throws Exception {
        mockMvc.perform(get("/register").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    // --- Unauthenticated redirects ---

    @Test
    void dashboard_redirectsToLoginWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void transactions_redirectsToLoginWhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    // --- Authenticated dashboard ---

    @Test
    @WithMockUser(username = "alice")
    void dashboard_returnsOkWithAccountInModel() throws Exception {
        Account account = accountWithBalance("500.00");
        when(accountService.findAccountByUsername("alice")).thenReturn(account);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("account", account))
                .andExpect(model().attributeExists("featureFlags"));
    }

    // --- Register POST ---

    @Test
    void registerPost_redirectsToLoginOnSuccess() throws Exception {
        when(accountService.registerAccount(anyString(), anyString()))
                .thenReturn(new Account());

        mockMvc.perform(post("/register")
                        .param("username", "newuser")
                        .param("password", "pass123")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void registerPost_showsErrorWhenUsernameTaken() throws Exception {
        when(accountService.registerAccount(anyString(), anyString()))
                .thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/register")
                        .param("username", "taken")
                        .param("password", "pass")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    // --- Withdraw ---

    @Test
    @WithMockUser(username = "alice")
    void withdraw_redirectsToDashboardOnSuccess() throws Exception {
        Account account = accountWithBalance("500.00");
        when(accountService.findAccountByUsername("alice")).thenReturn(account);

        mockMvc.perform(post("/withdraw").param("amount", "100").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @WithMockUser(username = "alice")
    void withdraw_showsErrorOnInsufficientFunds() throws Exception {
        Account account = accountWithBalance("10.00");
        when(accountService.findAccountByUsername("alice")).thenReturn(account);
        doThrow(new RuntimeException("Insufficient funds"))
                .when(accountService).withdraw(any(), any());

        mockMvc.perform(post("/withdraw").param("amount", "1000").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));
    }

    // --- Transfer ---

    @Test
    @WithMockUser(username = "alice")
    void transfer_redirectsToDashboardOnSuccess() throws Exception {
        Account account = accountWithBalance("500.00");
        when(accountService.findAccountByUsername("alice")).thenReturn(account);
        when(featureFlagService.isTransferEnabled()).thenReturn(true);

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "bob")
                        .param("amount", "100")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @WithMockUser(username = "alice")
    void transfer_showsErrorWhenFeatureFlagDisabled() throws Exception {
        Account account = accountWithBalance("500.00");
        when(accountService.findAccountByUsername("alice")).thenReturn(account);
        when(featureFlagService.isTransferEnabled()).thenReturn(false);

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "bob")
                        .param("amount", "100")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));

        verify(accountService, never()).transferAmount(any(), anyString(), any());
    }

    @Test
    @WithMockUser(username = "alice")
    void transfer_showsErrorWhenRecipientNotFound() throws Exception {
        Account account = accountWithBalance("500.00");
        when(accountService.findAccountByUsername("alice")).thenReturn(account);
        when(featureFlagService.isTransferEnabled()).thenReturn(true);
        doThrow(new RuntimeException("Recipient account not found"))
                .when(accountService).transferAmount(any(), anyString(), any());

        mockMvc.perform(post("/transfer")
                        .param("toUsername", "ghost")
                        .param("amount", "100")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("error"));
    }

    // --- helper ---

    private Account accountWithBalance(String amount) {
        Account account = new Account();
        account.setBalance(new BigDecimal(amount));
        return account;
    }
}
