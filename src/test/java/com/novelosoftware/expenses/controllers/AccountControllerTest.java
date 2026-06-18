package com.novelosoftware.expenses.controllers;

import com.novelosoftware.expenses.dto.*;
import com.novelosoftware.expenses.exceptions.GlobalExceptionHandler;
import com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions;
import com.novelosoftware.expenses.services.AccountService;
import com.novelosoftware.expenses.services.ExpenseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.novelosoftware.expenses.exceptions.AccountServiceExceptions.*;

/**
 * Test class for {@link AccountController.class}
 */
@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService service;

    @MockitoBean
    private ExpenseService expenseService;

    @Test
    void getByUser_returnsPaginatedAccounts() throws Exception {
        var page = new PageResponse<>(List.of(anAccount(1L)), 0, 20, 1L, 1);
        when(service.findByUser("user-1", 0, 20)).thenReturn(page);

        mockMvc.perform(get("/accounts/user/user-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].accountId").value(1))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getById_returnsOk() throws Exception {
        when(service.getById(1L, false)).thenReturn(anAccount(1L));

        mockMvc.perform(get("/accounts/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value(1));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(service.getById(99L, false)).thenThrow(createAccountNotFoundException(99L));

        mockMvc.perform(get("/accounts/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Account with ID 99 not found."));
    }

    @Test
    void create_returnsCreated() throws Exception {
        when(service.create(any())).thenReturn(new CreateAccountResponse(anAccount(1L)));

        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "Checking", "accountType": "DEBIT", "currency": "USD", "initialAmount": 1000.00 }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.value.accountId").value(1));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        when(service.update(eq(99L), any())).thenThrow(createAccountNotFoundException(99L));

        mockMvc.perform(put("/accounts/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "Checking", "accountType": "DEBIT", "currency": "USD", "currentAmount": 1000.00 }
                """))
            .andExpect(status().isNotFound());
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/accounts/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        doThrow(createAccountNotFoundException(99L)).when(service).delete(99L);

        mockMvc.perform(delete("/accounts/99"))
            .andExpect(status().isNotFound());
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
    void create_invalidEnumValue_returns400() throws Exception {
        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "value": { "name": "Checking", "accountType": "SAVINGS", "currency": "USD" } }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("Invalid value 'SAVINGS'. Accepted values: DEBIT, CREDIT"));
    }

    // -------------------------------------------------------------------------
    // GET /accounts/{id}/expenses
    // -------------------------------------------------------------------------

    @Test
    void listExpenses_accountExists_returns200WithPage() throws Exception {
        Expense expense = new Expense(10L, LocalDate.of(2026, 5, 10), 1L,
            new BigDecimal("42.50"), new BigDecimal("42.50"), "Tacos", SubCategory.RESTAURANT, "user-1");
        CursorPageResponse<Expense> page = new CursorPageResponse<>(List.of(expense), null, 20);

        // Ownership is enforced inside listByAccount, so the controller just delegates.
        when(expenseService.listByAccount(eq(1L), isNull(), isNull(), isNull(), isNull(),
            isNull(), isNull())).thenReturn(page);

        mockMvc.perform(get("/accounts/1/expenses"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].expenseId").value(10));
    }

    @Test
    void listExpenses_accountNotFound_returns404() throws Exception {
        when(expenseService.listByAccount(eq(99L), any(), any(), any(), any(), any(), any()))
            .thenThrow(createAccountNotFoundException(99L));

        mockMvc.perform(get("/accounts/99/expenses"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void listExpenses_categoryAndSubcategoryBothProvided_returns400() throws Exception {
        when(expenseService.listByAccount(eq(1L), any(), any(), any(), any(),
            eq("Food"), eq("Groceries")))
            .thenThrow(ExpenseServiceExceptions.createValidationException(
                "category and subcategory are mutually exclusive; provide at most one"));

        mockMvc.perform(get("/accounts/1/expenses")
                .param("category", "Food")
                .param("subcategory", "Groceries"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    private Account anAccount(Long id) {
        return new Account(id, "Checking", AccountType.DEBIT, "USD",
            new BigDecimal("1000.00"), new BigDecimal("1000.00"), "user-1", null, null);
    }
}
