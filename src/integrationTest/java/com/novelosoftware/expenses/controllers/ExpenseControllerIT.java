package com.novelosoftware.expenses.controllers;

import com.novelosoftware.expenses.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExpenseControllerIT extends BaseIT {

    private static final String USER = "user-expense-it";

    private long firstAccountId;
    private long secondAccountId;

    @BeforeEach
    void setUp() throws Exception {
        firstAccountId = createAccount("Test Account", USER);
        secondAccountId = createAccount("Second account", USER);
    }

    // -------------------------------------------------------------------------
    // POST /expenses
    // -------------------------------------------------------------------------

    @Test
    void create_happyPath_returns201WithWrappedExpense() throws Exception {
        mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": %d,
                      "amount": 42.50, "description": "Test tacos",
                      "subCategory": "RESTAURANT", "createdBy": "user-expense-it" } }
                """.formatted(firstAccountId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.value.expenseId").isNumber())
            .andExpect(jsonPath("$.value.expenseDate").value("2026-05-27"))
            .andExpect(jsonPath("$.value.accountId").value(firstAccountId))
            .andExpect(jsonPath("$.value.amount").value(42.50))
            .andExpect(jsonPath("$.value.description").value("Test tacos"))
            .andExpect(jsonPath("$.value.subCategory").value("RESTAURANT"))
            .andExpect(jsonPath("$.value.createdBy").value("user-expense-it"));
    }

    @Test
    void create_accountDoesNotExist_returns404() throws Exception {
        mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": 999999,
                      "amount": 42.50, "description": "Test tacos",
                      "subCategory": "RESTAURANT", "createdBy": "user-expense-it" } }
                """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void create_accountBelongsToDifferentUser_returns403() throws Exception {
        mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": %d,
                      "amount": 42.50, "description": "Test tacos",
                      "subCategory": "RESTAURANT", "createdBy": "user-B" } }
                """.formatted(firstAccountId)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void create_missingExpenseDate_returns400() throws Exception {
        mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": null, "accountId": %d,
                      "amount": 42.50, "description": "Test tacos",
                      "subCategory": "RESTAURANT", "createdBy": "user-expense-it" } }
                """.formatted(firstAccountId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("expenseDate cannot be null"));
    }

    @Test
    void create_missingAccountId_returns400() throws Exception {
        mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": null,
                      "amount": 42.50, "description": "Test tacos",
                      "subCategory": "RESTAURANT", "createdBy": "user-expense-it" } }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void create_missingAmount_returns400() throws Exception {
        mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": %d,
                      "amount": null, "description": "Test tacos",
                      "subCategory": "RESTAURANT", "createdBy": "user-expense-it" } }
                """.formatted(firstAccountId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void create_missingDescription_returns400() throws Exception {
        mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": %d,
                      "amount": 42.50, "description": "",
                      "subCategory": "RESTAURANT", "createdBy": "user-expense-it" } }
                """.formatted(firstAccountId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void create_missingSubCategory_returns400() throws Exception {
        mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": %d,
                      "amount": 42.50, "description": "Test tacos",
                      "subCategory": null, "createdBy": "user-expense-it" } }
                """.formatted(firstAccountId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void create_missingCreatedBy_returns400() throws Exception {
        mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": %d,
                      "amount": 42.50, "description": "Test tacos",
                      "subCategory": "RESTAURANT", "createdBy": "" } }
                """.formatted(firstAccountId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void create_invalidSubCategoryEnum_returns400WithAcceptedValues() throws Exception {
        mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": %d,
                      "amount": 42.50, "description": "Test tacos",
                      "subCategory": "INVALID_ENUM", "createdBy": "user-expense-it" } }
                """.formatted(firstAccountId)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("INVALID_ENUM")));
    }

    @Test
    void create_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ unclosed"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    // -------------------------------------------------------------------------
    // Multi-step flow: verify CategoryMapper → DB → ExpenseMapper round-trip
    // -------------------------------------------------------------------------

    @Test
    void create_subCategoryDerivedAndRoundTripped() throws Exception {
        mockMvc.perform(post("/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "expenseDate": "2026-05-27", "accountId": %d,
                      "amount": 15.00, "description": "Lunch",
                      "subCategory": "GROCERIES", "createdBy": "user-expense-it" } }
                """.formatted(firstAccountId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.value.subCategory").value("GROCERIES"));
    }

    @Test
    void update_happyPath() throws Exception {
        Long expenseId = createExpense(firstAccountId, USER);
        mockMvc.perform(put("/expenses/" + expenseId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "value": {
                            "expenseId": %d,
                            "expenseDate": "2026-01-15",
                            "accountId": %d,
                            "amount": 42.50,
                            "description": "Test expense",
                            "subCategory": "RESTAURANT",
                            "createdBy": "%s"
                        }
                    }
                """.formatted(expenseId, secondAccountId, USER)))
            .andExpect(status().is2xxSuccessful())
            .andExpect(jsonPath("$.value.accountId").value(secondAccountId));
    }

    @Test
    void update_unauthorizedOwnerChange() throws Exception {
        Long expenseId = createExpense(firstAccountId, USER);
        mockMvc.perform(put("/expenses/" + expenseId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "value": {
                            "expenseId": %d,
                            "expenseDate": "2026-01-15",
                            "accountId": %d,
                            "amount": 42.50,
                            "description": "Test expense",
                            "subCategory": "RESTAURANT",
                            "createdBy": "%s"
                        }
                    }
                """.formatted(expenseId, firstAccountId, "another-user")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value("User does not own the given account"));
    }

    @Test
    void update_unauthorizedAccountOnwerChange() throws Exception {
        Long expenseId = createExpense(firstAccountId, USER);
        Long thirdAccount = createAccount("third-person-account", "another-user-id");
        mockMvc.perform(put("/expenses/" + expenseId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "value": {
                            "expenseId": %d,
                            "expenseDate": "2026-01-15",
                            "accountId": %d,
                            "amount": 42.50,
                            "description": "Test expense",
                            "subCategory": "RESTAURANT",
                            "createdBy": "%s"
                        }
                    }
                """.formatted(expenseId, thirdAccount, USER)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value("User does not own the given account"));
    }

    @Test
    void update_expenseDoesNotExists() throws Exception {
        mockMvc.perform(put("/expenses/999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "value": {
                            "expenseId": 999999,
                            "expenseDate": "2026-01-15",
                            "accountId": %d,
                            "amount": 42.50,
                            "description": "Test expense",
                            "subCategory": "RESTAURANT",
                            "createdBy": "%s"
                        }
                    }
                """.formatted(secondAccountId, USER)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Expense id 999999 not found"));
    }

    @Test
    void update_accountNotFound() throws Exception {
        long expenseId = createExpense(firstAccountId, USER);
        mockMvc.perform(put("/expenses/" + expenseId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "value": {
                            "expenseId": %d,
                            "expenseDate": "2026-01-15",
                            "accountId": %d,
                            "amount": 42.50,
                            "description": "Test expense",
                            "subCategory": "RESTAURANT",
                            "createdBy": "%s"
                        }
                    }
                """.formatted(expenseId, 999999, USER)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Account with ID 999999 not found."));
    }
}
