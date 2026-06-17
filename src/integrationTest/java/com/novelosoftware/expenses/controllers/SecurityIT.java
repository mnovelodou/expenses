package com.novelosoftware.expenses.controllers;

import com.novelosoftware.expenses.BaseIT;
import com.novelosoftware.expenses.TestJwtFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIT extends BaseIT {

    private static final String OWNER = "user-security-it";

    private long accountId;
    private long expenseId;

    @BeforeEach
    void setUp() throws Exception {
        accountId = createAccount("Security Test Account", OWNER);
        expenseId = createExpense(accountId, OWNER);
    }

    // -------------------------------------------------------------------------
    // 401 — no Authorization header
    // -------------------------------------------------------------------------

    @Test
    void getExpenses_noToken_returns401() throws Exception {
        mockMvc.perform(get("/expenses").param("user_id", "user-security-it"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getExpenseById_noToken_returns401() throws Exception {
        mockMvc.perform(get("/expenses/{id}", expenseId))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteExpense_noToken_returns401() throws Exception {
        mockMvc.perform(delete("/expenses/{id}", expenseId))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getAccount_noToken_returns401() throws Exception {
        mockMvc.perform(get("/accounts/{id}", accountId))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteAccount_noToken_returns401() throws Exception {
        mockMvc.perform(delete("/accounts/{id}", accountId))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createAccount_noToken_returns401() throws Exception {
        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "name": "Test", "accountType": "DEBIT",
                      "currency": "USD", "initialAmount": 100.00, "createdBy": "user-security-it" } }
                """))
            .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // 403 — valid token but wrong scope
    // -------------------------------------------------------------------------

    @Test
    void getExpenses_writeOnlyToken_returns403() throws Exception {
        mockMvc.perform(get("/expenses")
                .with(jwt("write:expenses"))
                .param("user_id", "user-security-it"))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteExpense_readOnlyToken_returns403() throws Exception {
        mockMvc.perform(delete("/expenses/{id}", expenseId)
                .with(jwt("read:expenses")))
            .andExpect(status().isForbidden());
    }

    @Test
    void getAccount_writeOnlyToken_returns403() throws Exception {
        mockMvc.perform(get("/accounts/{id}", accountId)
                .with(jwt("write:accounts")))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteAccount_readOnlyToken_returns403() throws Exception {
        mockMvc.perform(delete("/accounts/{id}", accountId)
                .with(jwt("read:accounts")))
            .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Smoke — correct scope grants access (not 401 or 403)
    // -------------------------------------------------------------------------

    @Test
    void getExpenses_readExpensesToken_isNotRejected() throws Exception {
        mockMvc.perform(get("/expenses")
                .with(jwtAs(OWNER, "read:expenses"))
                .param("user_id", "user-security-it")
                .param("start_date", "2026-01-01")
                .param("end_date", "2026-01-31"))
            .andExpect(status().is2xxSuccessful());
    }

    @Test
    void getExpenseById_readExpensesToken_isNotRejected() throws Exception {
        mockMvc.perform(get("/expenses/{id}", expenseId)
                .with(jwtAs(OWNER, "read:expenses")))
            .andExpect(status().is2xxSuccessful());
    }

    @Test
    void deleteExpense_writeExpensesToken_isNotRejected() throws Exception {
        mockMvc.perform(delete("/expenses/{id}", expenseId)
                .with(jwtAs(OWNER, "write:expenses")))
            .andExpect(status().isNoContent());
    }

    @Test
    void getAccount_readAccountsToken_isNotRejected() throws Exception {
        mockMvc.perform(get("/accounts/{id}", accountId)
                .with(jwtAs(OWNER, "read:accounts")))
            .andExpect(status().is2xxSuccessful());
    }

    @Test
    void deleteAccount_writeAccountsToken_isNotRejected() throws Exception {
        // account has an expense — expect 412, not 401/403
        mockMvc.perform(delete("/accounts/{id}", accountId)
                .with(jwtAs(OWNER, "write:accounts")))
            .andExpect(status().isPreconditionFailed());
    }

    @Test
    void getAccountExpenses_readExpensesToken_isNotRejected() throws Exception {
        mockMvc.perform(get("/accounts/{id}/expenses", accountId)
                .with(jwtAs(OWNER, "read:expenses"))
                .param("start_date", "2026-01-01")
                .param("end_date", "2026-01-31"))
            .andExpect(status().is2xxSuccessful());
    }

    @Test
    void getAccountExpenses_readAccountsToken_returns403() throws Exception {
        mockMvc.perform(get("/accounts/{id}/expenses", accountId)
                .with(jwt("read:accounts"))
                .param("start_date", "2026-01-01")
                .param("end_date", "2026-01-31"))
            .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // Real signed JWT — exercises the actual NimbusJwtDecoder path
    // -------------------------------------------------------------------------

    @Test
    void validSignedToken_isAccepted() throws Exception {
        // TestJwtFactory issues tokens with subject "test-user"; the resource must be owned by it.
        long ownAccount = createAccount("Signed Token Account", "test-user");
        long ownExpense = createExpense(ownAccount, "test-user");
        String token = TestJwtFactory.createToken("read:expenses");
        mockMvc.perform(get("/expenses/{id}", ownExpense)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void expiredSignedToken_returns401() throws Exception {
        String token = TestJwtFactory.createExpiredToken("read:expenses");
        mockMvc.perform(get("/expenses/{id}", expenseId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedToken_returns401() throws Exception {
        mockMvc.perform(get("/expenses/{id}", expenseId)
                .header("Authorization", "Bearer not.a.valid.jwt"))
            .andExpect(status().isUnauthorized());
    }
}
