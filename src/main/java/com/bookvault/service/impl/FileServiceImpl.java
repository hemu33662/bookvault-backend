package com.bookvault.service.impl;

import com.bookvault.service.FileService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Security check: Only PDF and Images allowed
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        boolean isPdf = contentType != null && contentType.equalsIgnoreCase("application/pdf");
        boolean isImage = contentType != null && (contentType.startsWith("image/jpeg") || contentType.startsWith("image/png") || contentType.startsWith("image/webp"));

        if (!isPdf && !isImage) {
            throw new IllegalArgumentException("Only PDF and image files (JPEG, PNG, WebP) are allowed.");
        }

        if (originalFilename == null) {
            throw new IllegalArgumentException("Filename is missing.");
        }

        // Security check for executable extensions
        String[] forbiddenExtensions = {".exe", ".bat", ".cmd", ".sh", ".js", ".vbs"};
        for (String ext : forbiddenExtensions) {
            if (originalFilename.toLowerCase().endsWith(ext)) {
                throw new IllegalArgumentException("Executable or script files are strictly forbidden!");
            }
        }

        // Save to local storage for development
        String fileName = UUID.randomUUID().toString() + "_" + originalFilename;
        java.nio.file.Path uploadPath = java.nio.file.Paths.get("uploads");
        
        if (!java.nio.file.Files.exists(uploadPath)) {
            java.nio.file.Files.createDirectories(uploadPath);
        }

        try (java.io.InputStream inputStream = file.getInputStream()) {
            java.nio.file.Path filePath = uploadPath.resolve(fileName);
            byte[] data = inputStream.readAllBytes();
            
            // XOR Encryption (Security Key 0x42)
            for (int i = 0; i < data.length; i++) {
                data[i] ^= 0x42;
            }
            
            java.nio.file.Files.write(filePath, data);
        }

        // Return the path that will be served by our Static Resource Handler
        return "/uploads/" + fileName;
    }

    @Override
    public void deleteFile(String fileUrl) {
        // Simulate file deletion
        System.out.println("Deleting file from storage: " + fileUrl);
    }
}
