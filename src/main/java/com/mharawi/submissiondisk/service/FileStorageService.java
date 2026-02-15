package com.mharawi.submissiondisk.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class FileStorageService {

    @Value("${submission.storage.path:uploads}")
    private String storageBasePath;

    public String storeFile(MultipartFile file) throws IOException {
        validateZipFile(file);

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path storagePath = Paths.get(storageBasePath);

        // Create directory if it doesn't exist
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
        }

        Path filePath = storagePath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return filePath.toString();
    }

    public void deleteFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Files.deleteIfExists(path);
    }

    public String calculateChecksum(MultipartFile file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream inputStream = file.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private void validateZipFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("File must be a ZIP archive");
        }

        // Validate that it's actually a ZIP file by trying to read it
        try (ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry = zipInputStream.getNextEntry();
            if (entry == null) {
                throw new IllegalArgumentException("ZIP file is empty or corrupted");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid ZIP file format", e);
        }
    }

    public boolean isValidZipFile(MultipartFile file) {
        try {
            validateZipFile(file);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

