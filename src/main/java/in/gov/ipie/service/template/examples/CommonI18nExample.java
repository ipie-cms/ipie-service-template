package in.gov.ipie.service.template.examples;

import java.util.List;
import java.util.Locale;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import in.gov.ipie.common.core.exception.IpieException;
import in.gov.ipie.common.i18n.IpieI18nProperties;
import in.gov.ipie.common.i18n.MessageResolver;

/**
 * Example-only reference code for {@code common-i18n} - not wired into any real request flow.
 *
 * <p><b>Most services never call any of this.</b> common-web's {@code GlobalExceptionHandler} is
 * already the main consumer of {@link MessageResolver}, so an exception thrown from business code
 * comes back to the caller localized without the throwing code knowing anything about locales. The
 * methods below are for the cases that sit outside that path - a message assembled for a
 * notification, a label built for an export - and for understanding what the automatic path is
 * doing on your behalf.
 *
 * <p><b>The resolver never fails.</b> Every call carries the default the caller would otherwise
 * have used - almost always the English message already on the exception - so a service with no
 * translation bundles at all behaves exactly as it did before this module existed. A missing key is
 * silence, not an error, which is deliberate: a half-translated deployment should degrade to
 * English, not to a stack trace.
 *
 * <p><b>The supported-locale list is platform-wide, not per service.</b> Keycloak, the notification
 * templates and the frontend all have to name the same language tags, so adding a language is a
 * change to {@code ipie.i18n.supported-locales} rather than to any service's code. A request for a
 * language outside the list, a malformed {@code Accept-Language}, and no header at all all resolve
 * identically to {@link IpieI18nProperties#getDefaultLocale()}.
 */
@Component
public class CommonI18nExample {

    private final MessageResolver messageResolver;
    private final IpieI18nProperties i18nProperties;

    public CommonI18nExample(MessageResolver messageResolver, IpieI18nProperties i18nProperties) {
        this.messageResolver = messageResolver;
        this.i18nProperties = i18nProperties;
    }

    /**
     * The {@link IpieException} overload - the common case. The key looked up is the exception's own
     * stable {@code errorCode().code()}, e.g. {@code USER_NOT_FOUND}, so a bundle entry is keyed by
     * the same string the API already returns to consumers.
     *
     * <p>Note what this means for the message itself: a translated entry is necessarily a generic
     * sentence, because an exception that bakes the offending id into its English message has no way
     * to pass it as an argument. Migrating a service's exceptions to structured arguments is a
     * per-service follow-up, not something this resolver can do for you.
     */
    public String localize(IpieException failure) {
        return messageResolver.resolve(failure.errorCode(), failure.getMessage());
    }

    /** The raw-key overload, for a message that has no exception behind it - a notification subject, say. */
    public String localize(String messageKey, String englishFallback) {
        return messageResolver.resolve(messageKey, englishFallback);
    }

    /**
     * The locale both overloads above resolve against. It is set per request by
     * {@code SupportedLocaleResolver} and read from {@link LocaleContextHolder}, which is a
     * thread-local - so a message assembled on a different thread (an {@code @Async} method, a
     * scheduled job, a broker consumer) sees the JVM default, not the requesting user's language.
     * Resolve while still on the request thread, or carry the locale explicitly.
     */
    public Locale currentRequestLocale() {
        return LocaleContextHolder.getLocale();
    }

    /** The canonical list, e.g. to render a language picker without a service hard-coding its own copy. */
    public List<Locale> supportedLocales() {
        return i18nProperties.getSupportedLocales();
    }
}
