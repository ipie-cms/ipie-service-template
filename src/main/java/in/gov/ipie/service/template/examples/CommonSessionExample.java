package in.gov.ipie.service.template.examples;

import org.springframework.stereotype.Service;

import in.gov.ipie.common.session.SessionService;
import in.gov.ipie.common.session.SessionStatus;

/**
 * Example-only reference code for {@code common-session} - not wired into any real request flow.
 * Real business code injects {@link SessionService} the same way and calls the same four methods;
 * {@code SessionAutoConfiguration} already supplies the bean, backed by Redis once
 * {@code spring.data.redis.host} is set and by an in-memory store otherwise, so nothing here needs
 * to know which.
 *
 * <p><b>Most services never call this at all.</b> {@code SessionActivityFilter} already calls
 * {@link SessionService#touch(String)} for every authenticated request, and
 * {@code SessionController} already exposes status, extend and logout over HTTP. Inject
 * {@code SessionService} yourself only when a business rule needs the idle window - ending a
 * session early on a security-relevant change, or refusing an operation whose remaining window is
 * too short to finish it.
 *
 * <p><b>The identifier is the subject, not the person.</b> Every method keys on the same
 * {@code userId} the filter uses - the token subject. Passing anything else (an email, a business
 * id) silently creates a second session that nothing else in the platform will ever touch or
 * expire.
 */
@Service
public class CommonSessionExample {

    private final SessionService sessionService;

    public CommonSessionExample(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Reads the idle window without altering it.
     *
     * <p>{@link SessionStatus#active()} is false once the window has actually elapsed, so this is
     * the check to make before starting something that must not be abandoned half-done.
     * {@link SessionStatus#remainingSeconds()} and
     * {@link SessionStatus#warningThresholdSeconds()} are what a frontend uses to show the
     * "you will be signed out shortly" prompt.
     */
    public boolean hasTimeToFinish(String userId, long secondsNeeded) {
        SessionStatus status = sessionService.status(userId);
        return status.active() && status.remainingSeconds() >= secondsNeeded;
    }

    /**
     * "Stay logged in" - resets the idle window.
     *
     * <p>Distinct from {@link SessionService#touch(String)}, which the filter already does on every
     * request: extend is the deliberate answer to the warning prompt, and it resets to the
     * configured extend-by window rather than the ordinary idle timeout. Calling it on the user's
     * behalf, without them asking, defeats the point of having an idle timeout at all.
     */
    public SessionStatus keepAlive(String userId) {
        return sessionService.extend(userId);
    }

    /**
     * Ends the session immediately.
     *
     * <p>Worth calling from business code after a change that invalidates what the session was
     * authorised to do - a password change, a role revocation - so the next request is forced to
     * re-authenticate rather than continuing on an entitlement the user no longer holds.
     */
    public void endSession(String userId) {
        sessionService.logout(userId);
    }
}
