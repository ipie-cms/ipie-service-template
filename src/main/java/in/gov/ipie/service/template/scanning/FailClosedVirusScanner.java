package in.gov.ipie.service.template.scanning;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import in.gov.ipie.common.filestorage.scanning.ScanResult;
import in.gov.ipie.common.filestorage.scanning.VirusScanner;

/**
 * Default {@link VirusScanner} when no real scanner is configured - always returns {@link
 * in.gov.ipie.common.filestorage.scanning.ScanStatus#ERROR}, which {@code DocumentService} treats as
 * "reject the upload" (see {@code ScanUnavailableException}). Deliberately the opposite of {@code
 * LoggingEventPublisher}'s fallback: a missing event broker is safe to degrade to logging, a
 * missing virus scanner is never safe to degrade to "assume clean" (master standards doc,
 * file-upload rules, section 3).
 */
public class FailClosedVirusScanner implements VirusScanner {

    private static final Logger LOG = LoggerFactory.getLogger(FailClosedVirusScanner.class);

    @Override
    public ScanResult scan(InputStream content, long sizeBytes) {
        LOG.warn("No virus scanner configured (ipie.scanning.clamav.host unset) - failing closed, rejecting upload");
        return ScanResult.error("No virus scanner configured");
    }
}
