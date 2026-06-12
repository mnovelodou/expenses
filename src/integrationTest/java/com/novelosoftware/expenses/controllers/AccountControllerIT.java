package com.novelosoftware.expenses.controllers;

import com.novelosoftware.expenses.BaseIT;
import com.novelosoftware.expenses.util.ExpenseCursor;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountControllerIT extends BaseIT {

    // -------------------------------------------------------------------------
    // POST /accounts
    // -------------------------------------------------------------------------

    @Test
    void create_happyPath_returns201WithWrappedAccount() throws Exception {
        mockMvc.perform(post("/accounts")
                .with(fullScopeJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "name": "Checking", "accountType": "DEBIT",
                      "currency": "USD", "initialAmount": 1000.00, "createdBy": "user-it",
                      "periodStart": "2026-06-01" } }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.value.accountId").isNumber())
            .andExpect(jsonPath("$.value.name").value("Checking"))
            .andExpect(jsonPath("$.value.accountType").value("DEBIT"))
            .andExpect(jsonPath("$.value.currency").value("USD"))
            .andExpect(jsonPath("$.value.initialAmount").value(1000.00))
            .andExpect(jsonPath("$.value.currentAmount").value(1000.00))
            .andExpect(jsonPath("$.value.createdBy").value("user-it"))
            .andExpect(jsonPath("$.value.periodStart").value("2026-06-01"));
    }

    @Test
    void create_withDistinctCurrentAmount_persistsBothAmountsIndependently() throws Exception {
        mockMvc.perform(post("/accounts")
                .with(fullScopeJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "name": "Imported", "accountType": "DEBIT",
                      "currency": "USD", "initialAmount": 100.00, "currentAmount": 250.00,
                      "createdBy": "user-it" } }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.value.initialAmount").value(100.00))
            .andExpect(jsonPath("$.value.currentAmount").value(250.00));
    }

    @Test
    void create_missingName_returns400() throws Exception {
        mockMvc.perform(post("/accounts")
                .with(fullScopeJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "name": "", "accountType": "DEBIT", "currency": "USD",
                      "initialAmount": 1000.00, "createdBy": "user-it" } }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void create_missingAccountType_returns400() throws Exception {
        mockMvc.perform(post("/accounts")
                .with(fullScopeJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "name": "Checking", "accountType": null, "currency": "USD",
                      "initialAmount": 1000.00, "createdBy": "user-it" } }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void create_invalidAccountTypeEnum_returns400WithAcceptedValues() throws Exception {
        mockMvc.perform(post("/accounts")
                .with(fullScopeJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "name": "Checking", "accountType": "SAVINGS", "currency": "USD",
                      "initialAmount": 1000.00, "createdBy": "user-it" } }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Invalid value 'SAVINGS'. Accepted values: DEBIT, CREDIT"));
    }

    @Test
    void create_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/accounts")
                .with(fullScopeJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ this is not valid json }"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void create_flatPayloadMissingValueWrapper_returns400() throws Exception {
        mockMvc.perform(post("/accounts")
                .with(fullScopeJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "Checking", "accountType": "DEBIT", "currency": "USD",
                      "initialAmount": 1000.00, "createdBy": "user-it" }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    // -------------------------------------------------------------------------
    // GET /accounts/{id}
    // -------------------------------------------------------------------------

    @Test
    void getById_found_returns200BareAccount() throws Exception {
        long id = createAccount("Savings", "user-it");

        mockMvc.perform(get("/accounts/{id}", id)
                .with(fullScopeJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value(id))
            .andExpect(jsonPath("$.name").value("Savings"))
            .andExpect(jsonPath("$.createdBy").value("user-it"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/accounts/{id}", 999999)
                .with(fullScopeJwt()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    // -------------------------------------------------------------------------
    // GET /accounts/user/{userId}
    // -------------------------------------------------------------------------

    @Test
    void getByUser_returnsAllAccountsForUser() throws Exception {
        createAccount("Account A", "user-list");
        createAccount("Account B", "user-list");

        mockMvc.perform(get("/accounts/user/{userId}", "user-list")
                .with(fullScopeJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void getByUser_unknownUser_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/accounts/user/{userId}", "no-such-user")
                .with(fullScopeJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void getByUser_paginationFirstPage_returnsSingleItem() throws Exception {
        createAccount("Account A", "user-page");
        createAccount("Account B", "user-page");

        mockMvc.perform(get("/accounts/user/{userId}", "user-page")
                .with(fullScopeJwt())
                .param("page", "0").param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getByUser_paginationSecondPage_returnsSingleItem() throws Exception {
        createAccount("Account A", "user-page2");
        createAccount("Account B", "user-page2");

        mockMvc.perform(get("/accounts/user/{userId}", "user-page2")
                .with(fullScopeJwt())
                .param("page", "1").param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getByUser_userIsolation_doesNotReturnOtherUsersAccounts() throws Exception {
        createAccount("Account A", "user-alpha");

        mockMvc.perform(get("/accounts/user/{userId}", "user-beta")
                .with(fullScopeJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
    }

    // -------------------------------------------------------------------------
    // PUT /accounts/{id}
    // -------------------------------------------------------------------------

    @Test
    void update_happyPath_returns200AndInitialAmountUnchanged() throws Exception {
        long id = createAccount("Original", "user-it");

        mockMvc.perform(put("/accounts/{id}", id)
                .with(fullScopeJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "name": "Updated", "accountType": "CREDIT",
                      "currency": "EUR", "currentAmount": 2000.00, "createdBy": "user-it" } }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.value.name").value("Updated"))
            .andExpect(jsonPath("$.value.accountType").value("CREDIT"))
            .andExpect(jsonPath("$.value.currency").value("EUR"))
            .andExpect(jsonPath("$.value.currentAmount").value(2000.00))
            .andExpect(jsonPath("$.value.initialAmount").value(1000.00)); // must not change
    }

    @Test
    void update_notFound_returns404() throws Exception {
        mockMvc.perform(put("/accounts/{id}", 999999)
                .with(fullScopeJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "name": "X", "accountType": "DEBIT", "currency": "USD",
                      "currentAmount": 100.00, "createdBy": "user-it" } }
                """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void update_missingName_returns400() throws Exception {
        long id = createAccount("Original", "user-it");

        mockMvc.perform(put("/accounts/{id}", id)
                .with(fullScopeJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "name": "", "accountType": "DEBIT", "currency": "USD",
                      "currentAmount": 100.00, "createdBy": "user-it" } }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    // -------------------------------------------------------------------------
    // DELETE /accounts/{id}
    // -------------------------------------------------------------------------

    @Test
    void delete_happyPath_returns204AndSubsequentGetReturns404() throws Exception {
        long id = createAccount("To Delete", "user-it");

        mockMvc.perform(delete("/accounts/{id}", id)
                .with(fullScopeJwt()))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/accounts/{id}", id)
                .with(fullScopeJwt()))
            .andExpect(status().isNotFound());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/accounts/{id}", 999999)
                .with(fullScopeJwt()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void delete_accountWithExpenses_returns412() throws Exception {
        long accountId = createAccount("Has Expenses", "user-it");
        createExpense(accountId, "user-it");

        mockMvc.perform(delete("/accounts/{id}", accountId)
                .with(fullScopeJwt()))
            .andExpect(status().isPreconditionFailed())
            .andExpect(jsonPath("$.code").value("PRECONDITION_FAILED"));
    }

    // -------------------------------------------------------------------------
    // GET /accounts/{id}/expenses
    // -------------------------------------------------------------------------

    @Test
    void listExpenses_accountWithExpenses_returns200WithItems() throws Exception {
        long accountId = createAccount("My Card", "user-ae");
        createExpenseOnDate(accountId, "user-ae", "2026-05-10");
        createExpenseOnDate(accountId, "user-ae", "2026-05-20");

        mockMvc.perform(get("/accounts/{id}/expenses", accountId)
                .with(fullScopeJwt())
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].accountId").value(accountId))
            .andExpect(jsonPath("$.content[0].expenseDate").value("2026-05-20")) // newest first
            .andExpect(jsonPath("$.content[1].expenseDate").value("2026-05-10"));
    }

    @Test
    void listExpenses_onlyReturnsExpensesForSpecifiedAccount() throws Exception {
        long accountA = createAccount("Card A", "user-ae");
        long accountB = createAccount("Card B", "user-ae");
        createExpenseOnDate(accountA, "user-ae", "2026-05-10");
        createExpenseOnDate(accountA, "user-ae", "2026-05-15");
        createExpenseOnDate(accountB, "user-ae", "2026-05-20"); // should NOT appear

        mockMvc.perform(get("/accounts/{id}/expenses", accountA)
                .with(fullScopeJwt())
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].accountId").value(accountA))
            .andExpect(jsonPath("$.content[1].accountId").value(accountA));
    }

    @Test
    void listExpenses_accountWithNoExpenses_returns200WithEmptyList() throws Exception {
        long accountId = createAccount("Empty Card", "user-ae");

        mockMvc.perform(get("/accounts/{id}/expenses", accountId)
                .with(fullScopeJwt())
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void listExpenses_unknownAccountId_returns404() throws Exception {
        mockMvc.perform(get("/accounts/{id}/expenses", 999999)
                .with(fullScopeJwt())
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void listExpenses_categoryFilter_scopesResults() throws Exception {
        long accountId = createAccount("Filter Card", "user-ae");
        createExpenseWithSubcategoryOnDate(accountId, "user-ae", "2026-05-10", "RESTAURANT");
        createExpenseWithSubcategoryOnDate(accountId, "user-ae", "2026-05-15", "GROCERIES");

        mockMvc.perform(get("/accounts/{id}/expenses", accountId)
                .with(fullScopeJwt())
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31")
                .param("subcategory", "GROCERIES"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].subCategory").value("GROCERIES"));
    }

    @Test
    void listExpenses_cursorPagination_secondPageFollowsFirst() throws Exception {
        long accountId = createAccount("Paged Card", "user-ae");
        createExpenseOnDate(accountId, "user-ae", "2026-05-10");
        createExpenseOnDate(accountId, "user-ae", "2026-05-15");
        createExpenseOnDate(accountId, "user-ae", "2026-05-20");

        String firstPageJson = mockMvc.perform(get("/accounts/{id}/expenses", accountId)
                .with(fullScopeJwt())
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31")
                .param("limit", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.nextCursor").isString())
            .andReturn().getResponse().getContentAsString();

        String nextCursor = objectMapper.readTree(firstPageJson).path("nextCursor").asText();

        mockMvc.perform(get("/accounts/{id}/expenses", accountId)
                .with(fullScopeJwt())
                .param("start_date", "2026-05-01")
                .param("end_date", "2026-05-31")
                .param("limit", "2")
                .param("cursor", nextCursor))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void update_newInitialAmount_persistsItIndependentlyOfCurrentAmount() throws Exception {
        long id = createAccount("Savings", "user-it"); // initialAmount=1000, currentAmount=1000

        mockMvc.perform(put("/accounts/{id}", id)
                .with(fullScopeJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "name": "Savings", "accountType": "DEBIT",
                      "currency": "USD", "initialAmount": 1500.00, "currentAmount": 800.00,
                      "createdBy": "user-it" } }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.value.initialAmount").value(1500.00))
            .andExpect(jsonPath("$.value.currentAmount").value(800.00));
    }

    @Test
    void update_newInitialAmountWithoutCurrentAmount_preservesStoredCurrentAmount() throws Exception {
        long id = createAccount("Savings", "user-it"); // initialAmount=1000, currentAmount=1000

        mockMvc.perform(put("/accounts/{id}", id)
                .with(fullScopeJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "name": "Savings", "accountType": "DEBIT",
                      "currency": "USD", "initialAmount": 1500.00, "createdBy": "user-it" } }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.value.initialAmount").value(1500.00))
            .andExpect(jsonPath("$.value.currentAmount").value(1000.00));
    }

    @Test
    void update_nullInitialAmount_preservesStoredInitialAmount() throws Exception {
        long id = createAccount("Savings", "user-it"); // initialAmount=1000, currentAmount=1000

        mockMvc.perform(put("/accounts/{id}", id)
                .with(fullScopeJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "name": "Savings", "accountType": "DEBIT",
                      "currency": "USD", "createdBy": "user-it" } }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.value.initialAmount").value(1000.00))
            .andExpect(jsonPath("$.value.currentAmount").value(1000.00));
    }

    // -------------------------------------------------------------------------
    // Multi-step flows
    // -------------------------------------------------------------------------

    @Test
    void createThenRead_roundTrip_fieldsMatch() throws Exception {
        long id = createAccount("Round Trip", "user-it");

        mockMvc.perform(get("/accounts/{id}", id)
                .with(fullScopeJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value(id))
            .andExpect(jsonPath("$.name").value("Round Trip"))
            .andExpect(jsonPath("$.initialAmount").value(1000.00))
            .andExpect(jsonPath("$.currentAmount").value(1000.00));
    }

    @Test
    void createThenUpdate_initialAmountNotClobbered() throws Exception {
        long id = createAccount("Original", "user-it");

        mockMvc.perform(put("/accounts/{id}", id)
                .with(fullScopeJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "name": "Updated", "accountType": "DEBIT",
                      "currency": "USD", "currentAmount": 500.00, "createdBy": "user-it" } }
                """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/accounts/{id}", id)
                .with(fullScopeJwt()))
            .andExpect(jsonPath("$.currentAmount").value(500.00))
            .andExpect(jsonPath("$.initialAmount").value(1000.00));
    }

    // -------------------------------------------------------------------------
    // period_start and gap
    // -------------------------------------------------------------------------

    @Test
    void create_missingPeriodStart_returns400() throws Exception {
        mockMvc.perform(post("/accounts")
                .with(fullScopeJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "name": "Checking", "accountType": "DEBIT",
                      "currency": "USD", "initialAmount": 1000.00, "createdBy": "user-it" } }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void update_withoutPeriodStart_preservesExistingPeriodStart() throws Exception {
        long id = createAccount("Period Card", "user-it");

        mockMvc.perform(put("/accounts/{id}", id)
                .with(fullScopeJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "name": "Period Card", "accountType": "DEBIT",
                      "currency": "USD", "currentAmount": 900.00, "createdBy": "user-it" } }
                """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/accounts/{id}", id)
                .with(fullScopeJwt()))
            .andExpect(jsonPath("$.periodStart").value("2026-06-01"));
    }

    @Test
    void getById_withIncludeGap_returnsGapField() throws Exception {
        long id = createAccount("Gap Card", "user-it");
        createExpenseOnDate(id, "user-it", "2026-06-10");

        mockMvc.perform(get("/accounts/{id}", id)
                .with(fullScopeJwt())
                .param("includeGap", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gap").exists());
    }

    @Test
    void getById_withoutIncludeGap_hasNoGapField() throws Exception {
        long id = createAccount("No Gap Card", "user-it");

        mockMvc.perform(get("/accounts/{id}", id)
                .with(fullScopeJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gap").doesNotExist());
    }

    @Test
    void getById_gapCalculation_matchesExpectedValue() throws Exception {
        long id = createAccount("Calc Card", "user-it"); // initialAmount=1000, currentAmount=1000
        createExpenseOnDate(id, "user-it", "2026-06-05"); // amount=42.50
        createExpenseOnDate(id, "user-it", "2026-06-10"); // amount=42.50

        // gap = currentAmount(1000) - initialAmount(1000) - expenses(85.00) = -85.00
        mockMvc.perform(get("/accounts/{id}", id)
                .with(fullScopeJwt())
                .param("includeGap", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gap").value(-85.00));
    }

    @Test
    void getByUser_withIncludeGap_returnsGapInEachAccount() throws Exception {
        createAccount("Account A", "user-gap-list");
        createAccount("Account B", "user-gap-list");

        mockMvc.perform(get("/accounts/user/{userId}", "user-gap-list")
                .with(fullScopeJwt())
                .param("includeGap", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].gap").exists())
            .andExpect(jsonPath("$.content[1].gap").exists());
    }
}
