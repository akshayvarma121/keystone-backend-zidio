package com.zidio.keystone.service.storage;

import lombok.extern.slf4j.Slf4j;
import java.io.*;
import java.nio.file.*;

@Slf4j
public class LocalDiskStorageService implements ObjectStorageService {

    private final Path rootPath = Paths.get(".data", "attachments");

    public LocalDiskStorageService() {
        try {
            Files.createDirectories(rootPath);
            log.info("Initialized local disk storage at {}", rootPath.toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize local storage", e);
        }
    }

    @Override
    public String upload(String key, InputStream data, String contentType, long length) {
        try {
            Path target = rootPath.resolve(key);
            Files.createDirectories(target.getParent());
            Files.copy(data, target, StandardCopyOption.REPLACE_EXISTING);
            return key;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to local disk", e);
        }
    }

    @Override
    public InputStream download(String key) {
        try {
            Path target = rootPath.resolve(key);
            if (!Files.exists(target)) {
                throw new RuntimeException("File not found on local disk: " + key);
            }
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file from local disk", e);
        }
    }
}
