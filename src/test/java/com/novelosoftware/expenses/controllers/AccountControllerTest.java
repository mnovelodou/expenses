package com.novelosoftware.expenses.controllers;

import com.novelosoftware.expenses.dto.*;
import com.novelosoftware.expenses.enums.AccountType;
import com.novelosoftware.expenses.exceptions.AccountNotFoundException;
import com.novelosoftware.expenses.exceptions.GlobalExceptionHandler;
import com.novelosoftware.expenses.services.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@Import(GlobalExceptionHandler.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService service;

    @Test
    void getAll_returnsOk() throws Exception {
        when(service.getAll()).thenReturn(List.of(anAccount(1L)));

        mockMvc.perform(get("/accounts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].name").value("Checking"));
    }

    @Test
    void getById_returnsOk() throws Exception {
        when(service.getById(1L)).thenReturn(anAccount(1L));

        mockMvc.perform(get("/accounts/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(service.getById(99L)).thenThrow(new AccountNotFoundException(99L));

        mockMvc.perform(get("/accounts/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("Account not found: 99"));
    }

    @Test
    void create_returnsCreated() throws Exception {
        when(service.create(any(), any())).thenReturn(new CreateAccountResponse(anAccount(1L)));

        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "name": "Checking", "accountType": "DEBIT", "currency": "USD", "initialAmount": 1000.00 }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.value.id").value(1));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        when(service.update(eq(99L), any())).thenThrow(new AccountNotFoundException(99L));

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
        doThrow(new AccountNotFoundException(99L)).when(service).delete(99L);

        mockMvc.perform(delete("/accounts/99"))
            .andExpect(status().isNotFound());
    }

    private Account anAccount(Long id) {
        return new Account(id, "Checking", AccountType.DEBIT, "USD",
            new BigDecimal("1000.00"), new BigDecimal("1000.00"), "user-1");
    }
}
