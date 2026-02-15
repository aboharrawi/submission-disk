package com.mharawi.submissiondisk.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import xyz.capybara.clamav.ClamavClient;
import xyz.capybara.clamav.commands.scan.result.ScanResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class VirusScanService {

    private final ClamavClient clamavClient;
    private final boolean clamavEnabled;

    public VirusScanService(
            @Value("${clamav.host:localhost}") String clamavHost,
            @Value("${clamav.port:3310}") int clamavPort,
            @Value("${clamav.enabled:true}") boolean clamavEnabled) {
        this.clamavEnabled = clamavEnabled;
        if (clamavEnabled) {
            this.clamavClient = new ClamavClient(clamavHost, clamavPort);
            log.info("ClamAV client initialized: {}:{}", clamavHost, clamavPort);
        } else {
            this.clamavClient = null;
            log.warn("ClamAV scanning is DISABLED");
        }
    }

    /**
     * Scan a file for viruses using ClamAV
     *
     * @param filePath Path to the file to scan
     * @return ScanResult containing the scan outcome
     * @throws IOException if there's an error reading the file or connecting to ClamAV
     */
    public ScanResult scanFile(String filePath) throws IOException {
        if (!clamavEnabled) {
            log.warn("ClamAV is disabled, skipping virus scan for: {}", filePath);
            // Return null when disabled - caller should handle null as OK/skipped
            return null;
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + filePath);
        }

        log.info("Starting virus scan for file: {}", filePath);

        try (InputStream inputStream = Files.newInputStream(path)) {
            ScanResult result = clamavClient.scan(inputStream);

            if (result instanceof ScanResult.OK) {
                log.info("Virus scan PASSED for file: {}", filePath);
            } else if (result instanceof ScanResult.VirusFound virusFound) {
                log.error("VIRUS DETECTED in file {}: {}", filePath, virusFound);
            } else {
                log.warn("Virus scan returned unexpected result for file {}: {}", filePath, result);
            }

            return result;
        } catch (Exception e) {
            log.error("Error scanning file {} with ClamAV", filePath, e);
            throw new IOException("Virus scan failed: " + e.getMessage(), e);
        }
    }

    /**
     * Check if a file is clean (no viruses detected)
     *
     * @param filePath Path to the file to check
     * @return true if file is clean, false if virus found
     * @throws IOException if there's an error during scanning
     */
    public boolean isFileClean(String filePath) throws IOException {
        ScanResult result = scanFile(filePath);
        // null means ClamAV is disabled, treat as clean
        return result == null || result instanceof ScanResult.OK;
    }

    /**
     * Ping ClamAV server to check if it's available
     *
     * @return true if ClamAV is responding, false otherwise
     */
    public boolean ping() {
        if (!clamavEnabled || clamavClient == null) {
            return false;
        }

        try {
            clamavClient.ping();
            return true;
        } catch (Exception e) {
            log.error("Failed to ping ClamAV server", e);
            return false;
        }
    }
}

