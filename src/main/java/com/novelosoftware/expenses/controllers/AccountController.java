package com.novelosoftware.expenses.controllers;

import com.novelosoftware.expenses.dto.AccountRequest;
import com.novelosoftware.expenses.dto.AccountResponse;
import com.novelosoftware.expenses.services.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping
    public List<AccountResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public AccountResponse getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/user/{userId}")
    public List<AccountResponse> getByUser(@PathVariable String userId) {
        return service.getByUser(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@RequestBody AccountRequest request) {
        return service.create(request, "default_user_id");
    }

    @PutMapping("/{id}")
    public AccountResponse update(@PathVariable Long id, @RequestBody AccountRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
