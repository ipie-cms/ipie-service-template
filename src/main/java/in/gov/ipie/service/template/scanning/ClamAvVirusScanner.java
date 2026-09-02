package in.gov.ipie.service.template.scanning;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import in.gov.ipie.common.filestorage.scanning.ScanResult;
import in.gov.ipie.common.filestorage.scanning.VirusScanner;

/**
 * Real ClamAV binding for the {@link VirusScanner} port - speaks ClamAV's {@code INSTREAM}
 * protocol directly over TCP (no extra client library needed: the protocol is a command, then the
 * content in 4-byte-length-prefixed chunks, then a zero-length chunk to signal end of stream).
 * Self-hosted, no cloud/organizational approval needed - see {@code VirusScanConfig}'s Javadoc for
 * how to swap in a cloud-native scanner later, purely via configuration.
 */
public class ClamAvVirusScanner implements VirusScanner {

    private static final Logger LOG = LoggerFactory.getLogger(ClamAvVirusScanner.class);
    private static final int CHUNK_SIZE = 8192;

    private final String host;
    private final int port;
    private final int timeoutMillis;

    public ClamAvVirusScanner(String host, int port, int timeoutMillis) {
        this.host = host;
        this.port = port;
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public ScanResult scan(InputStream content, long sizeBytes) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);

            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeBytes("zINSTREAM\0");

            byte[] buffer = new byte[CHUNK_SIZE];
            int read;
            while ((read = content.read(buffer)) != -1) {
                out.writeInt(read);
                out.write(buffer, 0, read);
            }
            out.writeInt(0);
            out.flush();

            return interpretResponse(readNullTerminatedResponse(socket.getInputStream()));
        } catch (IOException e) {
            LOG.warn("ClamAV scan failed, treating as unavailable: {}", e.getMessage());
            return ScanResult.error(e.getMessage());
        }
    }

    private String readNullTerminatedResponse(InputStream in) throws IOException {
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1 && b != 0) {
            response.write(b);
        }
        return response.toString(StandardCharsets.UTF_8);
    }

    private ScanResult interpretResponse(String response) {
        if (response.contains("FOUND")) {
            String signature = response.replace("stream:", "").replace("FOUND", "").trim();
            return ScanResult.infected(signature);
        }
        if (response.contains("OK")) {
            return ScanResult.clean();
        }
        return ScanResult.error(response);
    }
}
