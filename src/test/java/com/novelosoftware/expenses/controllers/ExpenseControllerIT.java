package com.novelosoftware.expenses.controllers;

import com.novelosoftware.expenses.BaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ExpenseControllerIT extends BaseIT {

    private long accountId;

    @BeforeEach
    void setUp() throws Exception {
        accountId = createAccount("Test Account", "user-expense-it");
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
                """.formatted(accountId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.value.expenseId").isNumber())
            .andExpect(jsonPath("$.value.expenseDate").value("2026-05-27"))
            .andExpect(jsonPath("$.value.accountId").value(accountId))
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
                """.formatted(accountId)))
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
                """.formatted(accountId)))
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
                """.formatted(accountId)))
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
                """.formatted(accountId)))
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
                """.formatted(accountId)))
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
                """.formatted(accountId)))
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
                """.formatted(accountId)))
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
                """.formatted(accountId)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.value.subCategory").value("GROCERIES"));
    }
}
