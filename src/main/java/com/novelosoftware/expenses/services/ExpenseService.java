package com.novelosoftware.expenses.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.novelosoftware.expenses.dto.BulkCreateExpensesRequest;
import com.novelosoftware.expenses.dto.CreateExpenseRequest;
import com.novelosoftware.expenses.dto.CreateExpenseResponse;
import com.novelosoftware.expenses.dto.CursorPageResponse;
import com.novelosoftware.expenses.dto.Expense;
import com.novelosoftware.expenses.dto.UpdateExpenseRequest;
import com.novelosoftware.expenses.dto.UpdateExpenseResponse;
import com.novelosoftware.expenses.entities.ExpenseEntity;
import com.novelosoftware.expenses.mappers.ExpenseMapper;
import com.novelosoftware.expenses.repositories.ExpenseRepository;
import com.novelosoftware.expenses.security.CurrentUser;
import com.novelosoftware.expenses.util.DateWindowResolver;
import com.novelosoftware.expenses.util.DateWindow;
import com.novelosoftware.expenses.util.ExpenseCursor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions.*;

/**
 * ExpenseService contains business logic related to expenses.
 */
@Service
public class ExpenseService {

    private final ExpenseRepository repo;
    private final AccountService accountService;
    private final CurrentUser currentUser;
    private final Clock clock;

    @Autowired
    public ExpenseService(ExpenseRepository repo, AccountService accountService, CurrentUser currentUser) {
        this(repo, accountService, currentUser, Clock.systemDefaultZone());
    }

    /** Package-private constructor for unit tests that need a controllable clock. */
    ExpenseService(ExpenseRepository repo, AccountService accountService, CurrentUser currentUser, Clock clock) {
        this.repo = repo;
        this.accountService = accountService;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    /**
     * Creates multiple expenses atomically.
     *
     * @param requests list of CreateExpenseRequest payloads (1–200 items)
     * @return the created expenses wrapped in CreateExpenseResponse objects
     */
    @Transactional
    public List<CreateExpenseResponse> bulkCreate(BulkCreateExpensesRequest request) {
        if (request == null || request.expenses() == null || request.expenses().isEmpty()) {
            throw createValidationException("expenses list must not be empty");
        }
        if (request.expenses().size() > 200) {
            throw createValidationException("expenses list must not exceed 200 items");
        }

        String callerSub = currentUser.requireSubject();
        List<ExpenseEntity> entities = request.expenses().stream()
            .map(req -> {
                if (req == null || req.value() == null) {
                    throw createValidationException("Expense payload not provided");
                }
                expenseWriteValidations(req.value(), callerSub, true);
                return defaultTransactionAmount(ExpenseMapper.toEntity(req.value()));
            })
            .toList();

        return repo.bulkInsert(entities).stream()
            .map(e -> new CreateExpenseResponse(ExpenseMapper.toDto(e)))
            .toList();
    }

    /**
     * Creates an expense
     * @param request CreateExpenseRequest payload
     * @return the created Expense wrapped into a CreateExpenseResponse.
     */
    public CreateExpenseResponse create(CreateExpenseRequest request) {
        if (request == null || request.value() == null) {
            throw createValidationException("Expense payload not provided");
        }

        Expense expense = request.value();
        expenseWriteValidations(expense, currentUser.requireSubject(), true);

        ExpenseEntity expenseEntity = defaultTransactionAmount(ExpenseMapper.toEntity(expense));
        ExpenseEntity createdEntity = repo.create(expenseEntity);
        return new CreateExpenseResponse(ExpenseMapper.toDto(createdEntity));
    }

    public Expense getById(Long id) {
        String callerSub = currentUser.requireSubject();
        ExpenseEntity entity = repo.get(id).orElseThrow(() -> createExpenseNotFoundException(id));
        if (!callerSub.equals(entity.createdBy())) {
            // Hide existence of resources the caller does not own.
            throw createExpenseNotFoundException(id);
        }
        return ExpenseMapper.toDto(entity);
    }

    public void delete(Long id) {
        String callerSub = currentUser.requireSubject();
        // Fetch first so ownership can be verified before deleting.
        ExpenseEntity entity = repo.get(id).orElseThrow(() -> createExpenseNotFoundException(id));
        if (!callerSub.equals(entity.createdBy())) {
            throw createExpenseNotFoundException(id);
        }
        if (!repo.delete(id)) {
            throw createExpenseNotFoundException(id);
        }
    }

    public UpdateExpenseResponse update(Long id, UpdateExpenseRequest request) {
        if (request == null || request.value() == null)  {
            throw createValidationException("Expense payload not provided");
        }

        if (id == null) {
            throw createValidationException("ID not provided");
        }

        String callerSub = currentUser.requireSubject();
        Expense expense = request.value();
        // amount and transactionAmount are partial on update: a null value preserves the stored
        // value rather than failing validation, so amount is not required here.
        expenseWriteValidations(expense, callerSub, false);

        // Payload is validated; bring previous version and make sure the caller owns it.
        ExpenseEntity oldExpense = repo.get(id).orElseThrow(() -> createExpenseNotFoundException(id));

        if (!callerSub.equals(oldExpense.createdBy())) {
            // Hide existence of resources the caller does not own.
            throw createExpenseNotFoundException(id);
        }

        // Preserve the stored amount / transactionAmount when the request omits them. The
        // create-time "null transactionAmount defaults to amount" rule does NOT apply on update.
        ExpenseEntity newExpense = mergeUpdateValues(ExpenseMapper.toEntity(expense), oldExpense);
        ExpenseEntity updatedEntity = repo.update(id, newExpense).orElseThrow(() -> createExpenseNotFoundException(id));
        return new UpdateExpenseResponse(ExpenseMapper.toDto(updatedEntity));
    }

    /**
     * Returns a cursor-paginated list of expenses for the given user within the specified date window,
     * with optional filters on category, subcategory, and accountId.
     *
     * <p>Date defaults (each param is resolved independently):
     * <ul>
     *   <li>Both absent → previous calendar month</li>
     *   <li>Only {@code startDate} → {@code endDate = startDate + 1 month}</li>
     *   <li>Only {@code endDate}   → {@code startDate = endDate - 1 month}</li>
     * </ul>
     *
     * <p>{@code category} and {@code subcategory} are mutually exclusive — supplying both
     * results in a validation error. {@code accountId} may be combined with either.
     *
     * @param userId      optional; the user whose expenses to list, defaults to the caller
     * @param startDate   optional start of the date window
     * @param endDate     optional end of the date window
     * @param limit       optional page size; defaults to 20, max 100
     * @param cursorToken optional opaque cursor from a previous response
     * @param category    optional category filter; mutually exclusive with subcategory
     * @param subcategory optional subcategory filter; mutually exclusive with category
     * @param accountId   optional account filter
     * @param transactionAmount optional exact-match filter on the original transaction amount
     * @return a page of expenses with an optional next-page cursor
     */
    public CursorPageResponse<Expense> listByUser(String userId,
                                                   LocalDate startDate,
                                                   LocalDate endDate,
                                                   Integer limit,
                                                   String cursorToken,
                                                   String category,
                                                   String subcategory,
                                                   Long accountId,
                                                   BigDecimal transactionAmount) {
        // The requested user is optional and defaults to the caller. When supplied it must
        // match the caller; a mismatch is hidden as a 404 (ADMIN cross-user access is deferred).
        String callerSub = currentUser.requireSubject();
        String requestedUser = (userId == null || userId.isBlank()) ? callerSub : userId;
        if (!requestedUser.equals(callerSub)) {
            throw createExpenseNotFoundException("No expenses found for the requested user");
        }

        return listForOwner(requestedUser, startDate, endDate, limit, cursorToken, category, subcategory, accountId, transactionAmount);
    }

    /**
     * Lists expenses for a specific account after verifying the caller owns the account.
     * The owner is derived from the account, so ownership is enforced before any expense is read.
     *
     * @param accountId the account whose expenses to list
     * @return a page of expenses with an optional next-page cursor
     */
    public CursorPageResponse<Expense> listByAccount(Long accountId,
                                                      LocalDate startDate,
                                                      LocalDate endDate,
                                                      Integer limit,
                                                      String cursorToken,
                                                      String category,
                                                      String subcategory) {
        // Ownership-checked: a non-owned or missing account is hidden as 404.
        String owner = accountService.getById(accountId, false).createdBy();
        // The transaction_amount filter is exposed only on GET /expenses, so account listing passes null.
        return listForOwner(owner, startDate, endDate, limit, cursorToken, category, subcategory, accountId, null);
    }

    /**
     * Core listing logic shared by {@link #listByUser} and {@link #listByAccount}. The {@code userId}
     * is assumed already resolved and authorized by the caller.
     */
    private CursorPageResponse<Expense> listForOwner(String userId,
                                                     LocalDate startDate,
                                                     LocalDate endDate,
                                                     Integer limit,
                                                     String cursorToken,
                                                     String category,
                                                     String subcategory,
                                                     Long accountId,
                                                     BigDecimal transactionAmount) {
        // --- Validate mutually exclusive filters ---
        if (category != null && subcategory != null) {
            throw createValidationException("category and subcategory are mutually exclusive; provide at most one");
        }

        // --- Resolve and validate date window ---
        DateWindow window = DateWindowResolver.resolve(startDate, endDate, clock);

        // --- Resolve limit ---
        int resolvedLimit = (limit == null) ? 20 : limit;
        if (resolvedLimit <= 0) {
            throw createValidationException("limit must be greater than 0");
        }
        if (resolvedLimit > 100) {
            throw createValidationException("limit must not exceed 100");
        }

        // --- Decode cursor ---
        ExpenseCursor.DecodedCursor cursor = null;
        if (cursorToken != null && !cursorToken.isBlank()) {
            cursor = ExpenseCursor.decode(cursorToken); // throws InvalidCursorException if malformed
            if (cursor.date().isBefore(window.startDate()) || cursor.date().isAfter(window.endDate())) {
                throw createInvalidCursorException("Cursor date is outside the requested date range");
            }
        }

        // --- Fetch and map ---
        List<ExpenseEntity> entities = repo.findByFiltersCursor(
            userId, window.startDate(), window.endDate(),
            category, subcategory, accountId, transactionAmount,
            resolvedLimit, cursor);
        List<Expense> expenses = entities.stream().map(ExpenseMapper::toDto).toList();

        // --- Build next cursor ---
        String nextCursor = null;
        if (expenses.size() == resolvedLimit) {
            Expense last = expenses.get(expenses.size() - 1);
            nextCursor = ExpenseCursor.encode(last.expenseDate(), last.expenseId());
        }

        return new CursorPageResponse<>(expenses, nextCursor, resolvedLimit);
    }

    /**
     * Creation-only default: when the caller omits {@code transactionAmount}, set it to the line's
     * own {@code amount}. Never derived from splits or amount edits.
     */
    private static ExpenseEntity defaultTransactionAmount(ExpenseEntity entity) {
        if (entity.transactionAmount() != null) {
            return entity;
        }
        return entity.toBuilder().transactionAmount(entity.amount()).build();
    }

    /**
     * Merges a partial update onto the stored entity: a null {@code amount} or
     * {@code transactionAmount} in the incoming payload preserves the existing stored value.
     * Unlike creation, a null {@code transactionAmount} is NOT re-defaulted to {@code amount}.
     */
    private static ExpenseEntity mergeUpdateValues(ExpenseEntity incoming, ExpenseEntity existing) {
        var builder = incoming.toBuilder();
        if (incoming.amount() == null) {
            builder.amount(existing.amount());
        }
        if (incoming.transactionAmount() == null) {
            builder.transactionAmount(existing.transactionAmount());
        }
        return builder.build();
    }

    /**
     * Validates an expense write payload.
     *
     * @param amountRequired when {@code true} a null {@code amount} is rejected (create/bulk);
     *                       when {@code false} a null {@code amount} is allowed (update treats it
     *                       as "leave unchanged")
     */
    private void expenseWriteValidations(Expense expense, String callerSub, boolean amountRequired) {
        if (expense.expenseDate() == null) {
            throw createValidationException("expenseDate cannot be null");
        }

        if (expense.accountId() == null) {
            throw createValidationException("accountId cannot be null");
        }

        if (amountRequired && expense.amount() == null) {
            throw createValidationException("amount cannot be null");
        }

        // Reject values that exceed the NUMERIC(15,2) column so a precision overflow surfaces as a
        // 400 rather than a database 500, and so a too-precise value is not silently rounded on write.
        validateMonetaryScale(expense.amount(), "amount");
        validateMonetaryScale(expense.transactionAmount(), "transactionAmount");

        if (expense.description() == null || expense.description().isEmpty()) {
            throw createValidationException("description cannot be null");
        }

        if (expense.subCategory() == null) {
            throw createValidationException("subcategory cannot be null");
        }

        if (expense.createdBy() == null || expense.createdBy().isEmpty()) {
            throw createValidationException("createdBy cannot be null");
        }

        // A caller may only write expenses on their own behalf.
        if (!callerSub.equals(expense.createdBy())) {
            throw createUnauthorizedExpenseException("Cannot write expenses on behalf of another user");
        }

        // Resolve the account through the ownership-checked accessor so a missing account and
        // someone else's account are both hidden as 404 (no existence disclosure).
        accountService.getById(expense.accountId(), false);
    }

    /** Monetary columns are NUMERIC(15,2): up to 13 integer digits and 2 fractional digits. */
    private static final int MONETARY_SCALE = 2;
    private static final int MONETARY_INTEGER_DIGITS = 13;

    /**
     * Rejects a monetary value that would not fit the {@code NUMERIC(15,2)} column: more than two
     * fractional digits (which the database would silently round) or more than thirteen integer
     * digits (which the database would reject with a server error). Null is allowed and skipped.
     */
    private void validateMonetaryScale(BigDecimal value, String field) {
        if (value == null) {
            return;
        }
        if (value.scale() > MONETARY_SCALE) {
            throw createValidationException(field + " must have at most " + MONETARY_SCALE + " decimal places");
        }
        // precision() - scale() is the number of integer digits (an upper bound when scale < 0).
        if (value.precision() - value.scale() > MONETARY_INTEGER_DIGITS) {
            throw createValidationException(field + " must have at most " + MONETARY_INTEGER_DIGITS + " integer digits");
        }
    }
}
