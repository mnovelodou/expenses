package com.novelosoftware.expenses.controllers;

import com.novelosoftware.expenses.BaseIT;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AccountControllerIT extends BaseIT {

    // -------------------------------------------------------------------------
    // POST /accounts
    // -------------------------------------------------------------------------

    @Test
    void create_happyPath_returns201WithWrappedAccount() throws Exception {
        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "name": "Checking", "accountType": "DEBIT",
                      "currency": "USD", "initialAmount": 1000.00, "createdBy": "user-it" } }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.value.accountId").isNumber())
            .andExpect(jsonPath("$.value.name").value("Checking"))
            .andExpect(jsonPath("$.value.accountType").value("DEBIT"))
            .andExpect(jsonPath("$.value.currency").value("USD"))
            .andExpect(jsonPath("$.value.initialAmount").value(1000.00))
            .andExpect(jsonPath("$.value.currentAmount").value(1000.00))
            .andExpect(jsonPath("$.value.createdBy").value("user-it"));
    }

    @Test
    void create_missingName_returns400() throws Exception {
        mockMvc.perform(post("/accounts")
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
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ this is not valid json }"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void create_flatPayloadMissingValueWrapper_returns400() throws Exception {
        mockMvc.perform(post("/accounts")
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

        mockMvc.perform(get("/accounts/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value(id))
            .andExpect(jsonPath("$.name").value("Savings"))
            .andExpect(jsonPath("$.createdBy").value("user-it"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/accounts/{id}", 999999))
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

        mockMvc.perform(get("/accounts/user/{userId}", "user-list"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void getByUser_unknownUser_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/accounts/user/{userId}", "no-such-user"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void getByUser_paginationFirstPage_returnsSingleItem() throws Exception {
        createAccount("Account A", "user-page");
        createAccount("Account B", "user-page");

        mockMvc.perform(get("/accounts/user/{userId}", "user-page")
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
                .param("page", "1").param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getByUser_userIsolation_doesNotReturnOtherUsersAccounts() throws Exception {
        createAccount("Account A", "user-alpha");

        mockMvc.perform(get("/accounts/user/{userId}", "user-beta"))
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

        mockMvc.perform(delete("/accounts/{id}", id))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/accounts/{id}", id))
            .andExpect(status().isNotFound());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/accounts/{id}", 999999))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void delete_accountWithExpenses_returns412() throws Exception {
        long accountId = createAccount("Has Expenses", "user-it");
        createExpense(accountId, "user-it");

        mockMvc.perform(delete("/accounts/{id}", accountId))
            .andExpect(status().isPreconditionFailed())
            .andExpect(jsonPath("$.code").value("PRECONDITION_FAILED"));
    }

    // -------------------------------------------------------------------------
    // Multi-step flows
    // -------------------------------------------------------------------------

    @Test
    void createThenRead_roundTrip_fieldsMatch() throws Exception {
        long id = createAccount("Round Trip", "user-it");

        mockMvc.perform(get("/accounts/{id}", id))
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
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "name": "Updated", "accountType": "DEBIT",
                      "currency": "USD", "currentAmount": 500.00, "createdBy": "user-it" } }
                """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/accounts/{id}", id))
            .andExpect(jsonPath("$.currentAmount").value(500.00))
            .andExpect(jsonPath("$.initialAmount").value(1000.00));
    }
}
