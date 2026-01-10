package com.example.hcp.infra;

import com.example.hcp.global.exception.ApiException;
import com.example.hcp.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Component
public class FileStorageClient {

    private final Path uploadDir;
    private final String publicBaseUrl;

    public FileStorageClient(
            @Value("${app.storage.upload-dir}") String uploadDir,
            @Value("${app.storage.public-base-url}") String publicBaseUrl
    ) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl;
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create upload dir");
        }
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "EMPTY_FILE");
        }

        String original = file.getOriginalFilename();
        String ext = "";
        if (StringUtils.hasText(original) && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }

        String filename = UUID.randomUUID() + ext;
        Path target = uploadDir.resolve(filename);

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "FILE_SAVE_FAILED");
        }

        String url = publicBaseUrl.endsWith("/")
                ? publicBaseUrl + filename
                : publicBaseUrl + "/" + filename;

        return new StoredFile(
                url,
                original,
                file.getContentType(),
                file.getSize()
        );
    }

    public record StoredFile(
            String url,
            String originalName,
            String mimeType,
            long size
    ) {}
}
