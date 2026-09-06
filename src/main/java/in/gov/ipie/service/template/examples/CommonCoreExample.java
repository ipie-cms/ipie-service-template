package in.gov.ipie.service.template.examples;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import in.gov.ipie.common.core.exception.ConflictException;
import in.gov.ipie.common.core.exception.FieldError;
import in.gov.ipie.common.core.exception.NotFoundException;
import in.gov.ipie.common.core.exception.ValidationFailedException;
import in.gov.ipie.common.core.paging.Cursor;
import in.gov.ipie.common.core.paging.CursorPageRequest;
import in.gov.ipie.common.core.paging.CursorPageResult;
import in.gov.ipie.common.core.paging.PageRequest;
import in.gov.ipie.common.core.paging.PageResult;
import in.gov.ipie.service.template.exception.UserErrorCode;

/**
 * Example-only reference code for {@code common-core} - not wired into any real request flow.
 * {@code common-core} is the one package with no infrastructure behind it at all: exception base
 * types and paging primitives that domain and application code use directly, which is why it is
 * also the package a service touches most and thinks about least.
 *
 * <p>Two things here are easy to get wrong and cost more than they look:
 *
 * <ul>
 *   <li><b>Throw, never build a response.</b> {@link NotFoundException}/{@link ConflictException}/
 *       {@link ValidationFailedException} are translated to 404/409/400 by common-web's
 *       {@code GlobalExceptionHandler}. A service that catches one and returns a
 *       {@code ResponseEntity} itself has quietly forked the platform's error shape.</li>
 *   <li><b>Prefer a service-specific {@code ErrorCode}.</b> The single-argument constructors fall
 *       back to {@code CommonErrorCode.NOT_FOUND}/{@code CONFLICT}, which tells an API consumer
 *       only that something was missing. A code like {@code UserErrorCode.USER_NOT_FOUND} tells
 *       them what - and, once released, it is a contract whose meaning must not change.</li>
 * </ul>
 */
@Component
public class CommonCoreExample {

    /** The generic form - acceptable, but the caller learns nothing about <em>what</em> was missing. */
    public String requireFound(UUID id, Optional<String> found) {
        return found.orElseThrow(() -> new NotFoundException("No example record with id " + id));
    }

    /** The preferred form: a per-domain code the API consumer can branch on. */
    public String requireFoundWithDomainCode(UUID id, Optional<String> found) {
        return found.orElseThrow(
                () -> new NotFoundException(UserErrorCode.USER_NOT_FOUND, "No example record with id " + id));
    }

    /** A uniqueness clash the caller can fix by sending something else - 409, not 500. */
    public void rejectDuplicate(String reference) {
        throw new ConflictException("An example record with reference " + reference + " already exists");
    }

    /**
     * A cross-field rule bean validation cannot express (it sees one field at a time). Each
     * {@link FieldError} names the field the caller must change, and common-web surfaces the list
     * under {@code fieldErrors} - which is what makes a 400 actionable rather than just a refusal.
     */
    public void rejectInvalidRange(int from, int to) {
        if (from > to) {
            throw new ValidationFailedException(
                    "The range is inverted",
                    List.of(new FieldError("from", "must not be greater than 'to'")));
        }
    }

    /**
     * Offset paging: {@link PageRequest} in, {@link PageResult} out, both framework-agnostic so
     * the application layer never sees Spring Data's {@code Pageable}/{@code Page}. Use this when
     * the screen genuinely needs a total count and the data set is small - the {@code COUNT(*)} is
     * the cost, and it does not get cheaper at depth.
     */
    public PageResult<String> offsetPage(List<String> allRows, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        int from = Math.min(pageRequest.page() * pageRequest.size(), allRows.size());
        int to = Math.min(from + pageRequest.size(), allRows.size());
        return PageResult.of(allRows.subList(from, to), pageRequest.page(), pageRequest.size(), allRows.size());
    }

    /**
     * Keyset paging, for a listing that has to stay fast at any depth. The token handed back is an
     * opaque {@link Cursor} over {@code (createdAt, id)} - the two columns every aggregate in this
     * platform already carries - and the caller only ever passes it back verbatim.
     *
     * <p>There is deliberately no total count: producing one needs exactly the {@code COUNT(*)}
     * this style exists to avoid, so {@link CursorPageResult#hasMore()} is all a caller gets, and
     * all it needs to decide whether to ask for another page.
     */
    public CursorPageResult<String> keysetPage(List<String> rowsAfterCursor, int size, Cursor lastRowOnThisPage) {
        boolean hasMore = rowsAfterCursor.size() > size;
        List<String> content = hasMore ? rowsAfterCursor.subList(0, size) : rowsAfterCursor;
        return CursorPageResult.of(content, hasMore ? lastRowOnThisPage.encode() : null, hasMore);
    }

    /**
     * The read side of the same token. A tampered or truncated cursor raises
     * {@link ValidationFailedException} from inside {@code decode} rather than returning something
     * plausible - a decoded-but-wrong cursor would silently skip or repeat rows, which is far worse
     * than a 400.
     */
    public Optional<Cursor> resumeFrom(String cursorToken, int size) {
        return new CursorPageRequest(cursorToken, size).decodeCursor();
    }
}
