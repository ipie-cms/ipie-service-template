package in.gov.ipie.service.template.examples;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import in.gov.ipie.common.core.paging.CursorPageRequest;
import in.gov.ipie.common.core.paging.PageRequest;
import in.gov.ipie.common.web.idempotency.Idempotent;
import in.gov.ipie.common.web.paging.CursorPageResponse;
import in.gov.ipie.common.web.paging.PageResponse;
import in.gov.ipie.common.web.util.HttpRequestUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Example-only reference code for {@code common-web} - a controller, because that is where every
 * part of this package is actually reached from. Mapped under {@code /api/v1/examples/web} so it
 * cannot collide with a real endpoint; delete this class when the service has its own.
 *
 * <p>The one thing in {@code common-web} that needs no example is the error contract.
 * {@code GlobalExceptionHandler} is already registered by {@code WebErrorAutoConfiguration} and
 * turns a thrown exception into an {@code ApiError} on its own - a controller that catches its own
 * exceptions to build an error body is working against it, not with it. Throw and let it answer.
 */
@RestController
@RequestMapping("/api/v1/examples/web")
public class CommonWebExample {

    /**
     * Offset paging: the shape to return whenever a client needs page numbers or a total.
     *
     * <p>{@link PageRequest} validates on construction and caps {@code size} at
     * {@link PageRequest#MAX_PAGE_SIZE}, which is what stops a caller asking for every row by
     * passing {@code size=1000000}. Build it from the query parameters rather than trusting them.
     *
     * <p>{@code PageResponse.from(result, mapper)} is the two-argument form, and is the one to
     * reach for: it maps entity to DTO while assembling the envelope, so an entity never leaves the
     * controller by accident.
     */
    @GetMapping("/page")
    public PageResponse<String> listByPage(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        PageRequest request = PageRequest.of(page, size);
        // A real controller passes `request` to an application service and maps the PageResult it
        // returns: return PageResponse.from(service.search(request), userMapper::toResponse);
        List<String> content = List.of("row-" + request.page() + "-a", "row-" + request.page() + "-b");
        return new PageResponse<>(content, request.page(), request.size(), 2, 1);
    }

    /**
     * Cursor paging: the shape to return for a large or continuously growing collection.
     *
     * <p>Preferred over offset paging once a table is big enough for it to matter. Offset paging
     * makes the database count and skip rows it will not return, so deep pages get slower the
     * further in they are, and a row inserted mid-scan shifts every later page. A cursor is
     * positional, so neither happens.
     */
    @GetMapping("/cursor")
    public CursorPageResponse<String> listByCursor(@RequestParam(required = false) String cursor,
                                                   @RequestParam(defaultValue = "20") int size) {
        CursorPageRequest request = (cursor == null || cursor.isBlank())
                ? CursorPageRequest.firstPage(size)
                : new CursorPageRequest(cursor, size);
        // Real code: return CursorPageResponse.from(service.scroll(request), userMapper::toResponse);
        return new CursorPageResponse<>(List.of("row-a", "row-b"), null, false);
    }

    /**
     * {@code @Idempotent} - a repeat of the same request under the same {@code Idempotency-Key}
     * header returns the first call's response instead of acting twice.
     *
     * <p>Belongs on any unsafe endpoint a client may retry: a payment, a submission, anything that
     * creates a record. A network timeout is indistinguishable from a slow success at the caller,
     * so a retry is not a client bug to be prevented but a normal event to be absorbed.
     *
     * <p>The annotation is a marker only. Each service supplies its own {@code IdempotencyAspect}
     * over its own key storage - adding the annotation without that wiring silently does nothing,
     * which is the failure to watch for here.
     */
    @Idempotent
    @PostMapping("/submit")
    public ResponseEntity<String> submitOnce(HttpServletRequest request) {
        // clientIp() reads the forwarded-for chain against the trusted-proxy configuration rather
        // than taking the socket address, so behind the gateway it yields the real caller instead
        // of the gateway itself - which is what makes per-caller rate limiting and audit meaningful.
        String callerIp = HttpRequestUtils.clientIp(request);
        return ResponseEntity.ok("accepted from " + callerIp);
    }
}
