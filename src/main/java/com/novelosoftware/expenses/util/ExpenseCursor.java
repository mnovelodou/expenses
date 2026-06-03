package com.novelosoftware.expenses.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions;

import java.time.LocalDate;
import java.util.Base64;

/**
 * Encodes and decodes opaque cursor tokens for expense pagination.
 *
 * <p>The token is a URL-safe base64 encoding of a small JSON payload:
 * {@code {"d":"<expense_date>","id":<expense_id>}}.
 * Clients must treat the token as opaque and never construct one themselves.
 */
public final class ExpenseCursor {

    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    private ExpenseCursor() {}

    /**
     * Encodes a cursor position into an opaque token.
     *
     * @param date the expense_date of the last item on the page
     * @param id   the expense_id of the last item on the page
     * @return URL-safe base64-encoded cursor token
     */
    public static String encode(LocalDate date, Long id) {
        try {
            String json = MAPPER.writeValueAsString(new Payload(date.toString(), id));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode cursor", e);
        }
    }

    /**
     * Decodes an opaque cursor token back into a position.
     *
     * @param token the token returned by a previous response
     * @return the decoded cursor position
     * @throws com.novelosoftware.expenses.exceptions.ExpenseServiceExceptions.InvalidCursorException
     *         if the token cannot be decoded or contains an invalid date
     */
    public static DecodedCursor decode(String token) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(token);
            Payload payload = MAPPER.readValue(bytes, Payload.class);
            if (payload.d() == null || payload.id() == null) {
                throw ExpenseServiceExceptions.createInvalidCursorException("Cursor is missing required fields");
            }
            if (payload.id() <= 0) {
                throw ExpenseServiceExceptions.createInvalidCursorException("Cursor contains an invalid id");
            }
            LocalDate date = LocalDate.parse(payload.d());
            return new DecodedCursor(date, payload.id());
        } catch (ExpenseServiceExceptions.InvalidCursorException e) {
            throw e;
        } catch (Exception e) {
            throw ExpenseServiceExceptions.createInvalidCursorException("Cursor is malformed or cannot be decoded");
        }
    }

    /** A decoded cursor position: the date and id of the last fetched expense. */
    public record DecodedCursor(LocalDate date, Long id) {}

    private record Payload(
        @JsonProperty("d") String d,
        @JsonProperty("id") Long id
    ) {}
}
