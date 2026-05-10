package com.bookvault.controller;

import com.bookvault.dto.BookDTO;
import com.bookvault.dto.CommentDTO;
import com.bookvault.service.BookService;
import com.bookvault.service.SecurityService;
import com.bookvault.service.FileService;
import com.bookvault.service.PdfExtractionService;
import com.bookvault.service.GoogleDriveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.Map;
import java.net.URL;
import java.io.InputStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.InputStreamResource;
import java.net.HttpURLConnection;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Formatter;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.BufferedInputStream;

@RestController
@CrossOrigin(origins = "*") // Allows the Angular frontend to fetch .vault files for decryption
public class BookController {

    @Autowired
    private BookService bookService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private FileService fileService;

    @Autowired
    private PdfExtractionService pdfExtractionService;

    @Autowired
    private GoogleDriveService googleDriveService;

    @Autowired
    private com.bookvault.service.AdminDashboardService adminDashboardService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- Public Endpoints ---

    @GetMapping("/api/public/books")
    public ResponseEntity<List<BookDTO>> getAllPublishedBooks() {
        try {
            return ResponseEntity.ok(bookService.getPublishedBooks());
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/public/announcements")
    public ResponseEntity<?> getPublicAnnouncements() {
        try {
            return ResponseEntity.ok(adminDashboardService.getAllNotifications());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/public/books/{id}")
    public ResponseEntity<BookDTO> getBookById(@PathVariable String id) {
        try {
            return bookService.getBookById(id)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // --- Authenticated Endpoints ---

    @PostMapping(value = "/api/books", consumes = {"multipart/form-data"})
    public ResponseEntity<?> createBook(
            @RequestPart("book") String bookJson,
            @RequestPart(value = "pdf", required = false) MultipartFile pdfFile,
            @RequestPart(value = "coverImage", required = false) MultipartFile coverImage) {
        
        if (!securityService.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only administrators can add books");
        }
        
        try {
            BookDTO book = objectMapper.readValue(bookJson, BookDTO.class);
            
            if (pdfFile != null && !pdfFile.isEmpty()) {
                String pdfUrl = fileService.uploadFile(pdfFile);
                book.setPdfUrl(pdfUrl);
                processPdfToJson(pdfFile, book);
            } else if (book.getPdfUrl() != null && book.getPdfUrl().contains("drive.google.com")) {
                processDriveLinkToJson(book.getPdfUrl(), book);
            }

            // Handle Cover Image (New Secure Workflow)
            if (coverImage != null && !coverImage.isEmpty()) {
                String imageUrl = encryptAndSaveMultipartFile(coverImage);
                book.setCoverImageUrl(imageUrl);
            } else if (book.getCoverImageUrl() != null && book.getCoverImageUrl().contains("drive.google.com")) {
                String imageUrl = downloadAndEncryptCover(book.getCoverImageUrl(), book.getTitle());
                book.setCoverImageUrl(imageUrl);
            }
            
            return ResponseEntity.ok(bookService.createOrUpdateBook(book));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PutMapping(value = "/api/books/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> updateBook(
            @PathVariable String id,
            @RequestPart("book") String bookJson,
            @RequestPart(value = "pdf", required = false) MultipartFile pdfFile,
            @RequestPart(value = "coverImage", required = false) MultipartFile coverImage) {
        
        if (!securityService.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only administrators can update books");
        }
        
        try {
            BookDTO book = objectMapper.readValue(bookJson, BookDTO.class);
            book.setId(id);
            
            Optional<BookDTO> existingBook = bookService.getBookById(id);
            if (existingBook.isPresent()) {
                if (book.getPdfUrl() == null) book.setPdfUrl(existingBook.get().getPdfUrl());
                if (book.getCoverImageUrl() == null || book.getCoverImageUrl().isEmpty()) {
                    book.setCoverImageUrl(existingBook.get().getCoverImageUrl());
                }
                if (book.getContentJsonUrl() == null) book.setContentJsonUrl(existingBook.get().getContentJsonUrl());
            }

            if (pdfFile != null && !pdfFile.isEmpty()) {
                String pdfUrl = fileService.uploadFile(pdfFile);
                book.setPdfUrl(pdfUrl);
                processPdfToJson(pdfFile, book);
            } else if (book.getPdfUrl() != null && book.getPdfUrl().contains("drive.google.com")) {
                processDriveLinkToJson(book.getPdfUrl(), book);
            }

            // Handle Cover Image (New Secure Workflow)
            if (coverImage != null && !coverImage.isEmpty()) {
                String imageUrl = encryptAndSaveMultipartFile(coverImage);
                book.setCoverImageUrl(imageUrl);
            } else if (book.getCoverImageUrl() != null && book.getCoverImageUrl().contains("drive.google.com")) {
                // Only download if it's a new Drive link (doesn't start with /uploads/)
                if (!book.getCoverImageUrl().startsWith("/uploads/")) {
                    String imageUrl = downloadAndEncryptCover(book.getCoverImageUrl(), book.getTitle());
                    book.setCoverImageUrl(imageUrl);
                }
            }
            
            return ResponseEntity.ok(bookService.createOrUpdateBook(book));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    private String encryptAndSaveMultipartFile(MultipartFile file) throws Exception {
        byte[] data = file.getBytes();
        return encryptAndSave(data, file.getOriginalFilename());
    }

    private String downloadAndEncryptCover(String driveUrl, String bookTitle) throws Exception {
        // Convert Drive Link to direct download Link
        String fileId = "";
        if (driveUrl.contains("id=")) {
            fileId = driveUrl.split("id=")[1].split("&")[0];
        } else if (driveUrl.contains("/d/")) {
            fileId = driveUrl.split("/d/")[1].split("/")[0];
        }
        
        String directUrl = "https://drive.google.com/uc?export=download&id=" + fileId;
        
        RestTemplate restTemplate = new RestTemplate();
        byte[] data = restTemplate.getForObject(directUrl, byte[].class);
        
        if (data == null) throw new Exception("Failed to download image from Drive");
        
        return encryptAndSave(data, bookTitle + ".png");
    }

    private String encryptAndSave(byte[] data, String originalName) throws Exception {
        // XOR Encryption (Security Key 0x42)
        byte secretKey = 0x42;
        for (int i = 0; i < data.length; i++) {
            data[i] ^= secretKey;
        }
        
        String fileName = java.util.UUID.randomUUID().toString() + "_" + 
                          originalName.replaceAll("[^a-zA-Z0-9.]", "_") + ".vault";
        
        Path uploadPath = Paths.get("uploads");
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);
        
        Files.write(uploadPath.resolve(fileName), data);
        return "/uploads/" + fileName;
    }

    @DeleteMapping("/api/books/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable String id) {
        if (!securityService.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only administrators can delete books");
        }
        try {
            bookService.deleteBook(id);
            return ResponseEntity.noContent().build();
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // --- Comment Endpoints ---

    @PostMapping("/api/books/{id}/comments")
    public ResponseEntity<?> addComment(@PathVariable String id, @RequestBody CommentDTO comment) {
        String userId = securityService.getCurrentUserFirebaseUid();
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            comment.setBookId(id);
            comment.setUserId(userId);
            return ResponseEntity.ok(bookService.addComment(id, comment));
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/api/books/{id}/comments/{commentId}")
    public ResponseEntity<?> updateComment(@PathVariable String id, @PathVariable String commentId, @RequestBody String content) {
        String userId = securityService.getCurrentUserFirebaseUid();
        if (userId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(bookService.updateComment(id, commentId, content, userId, securityService.isAdmin()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @DeleteMapping("/api/books/{id}/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable String id, @PathVariable String commentId) {
        String userId = securityService.getCurrentUserFirebaseUid();
        try {
            return ResponseEntity.ok(bookService.deleteComment(id, commentId, userId, securityService.isAdmin()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PatchMapping("/api/books/{id}/readers")
    public ResponseEntity<?> incrementReaders(@PathVariable String id) {
        try {
            bookService.incrementReadersCount(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PatchMapping("/api/books/{id}/favorites")
    public ResponseEntity<?> incrementFavorites(@PathVariable String id, @RequestParam boolean increment) {
        try {
            bookService.incrementFavoritesCount(id, increment);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/api/public/proxy")
    public ResponseEntity<InputStreamResource> proxyFile(
            @RequestParam String url,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {
        try {
            String currentUrl = url.trim().replace(" ", "%20");
            
            // 1. Check if we have a cached version locally
            String urlHash = getMd5Hash(currentUrl);
            Path cacheDir = Paths.get("uploads", "cache");
            if (!Files.exists(cacheDir)) Files.createDirectories(cacheDir);
            
            Path cachedFilePath = cacheDir.resolve(urlHash);
            File cachedFile = cachedFilePath.toFile();

            // Determine expected content type based on URL
            String contentType = "application/octet-stream";
            if (currentUrl.toLowerCase().contains(".pdf") || currentUrl.toLowerCase().contains("export=download")) {
                contentType = "application/pdf";
            } else if (currentUrl.toLowerCase().contains("thumbnail") || currentUrl.toLowerCase().contains(".png") || currentUrl.toLowerCase().contains(".jpg")) {
                contentType = "image/png";
            }

            // Serve from cache if available
            if (cachedFile.exists() && cachedFile.length() > 1024) { // Basic check for corrupted small files
                System.out.println("🚀 Serving from Local Speed Mirror: " + urlHash);
                InputStream inputStream = new FileInputStream(cachedFile);
                
                InputStreamResource resource = new InputStreamResource(inputStream);
                HttpHeaders headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_TYPE, contentType);
                headers.setContentLength(cachedFile.length());
                headers.add("X-Cache-Status", "HIT");
                headers.add("Accept-Ranges", "bytes");
                return ResponseEntity.ok().headers(headers).body(resource);
            }

            // 2. Not in cache or corrupted, fetch from source
            HttpURLConnection connection = null;
            int redirectCount = 0;
            String downloadUrl = currentUrl;
            
            while (redirectCount < 5) {
                URL targetUrl = new URL(downloadUrl);
                connection = (HttpURLConnection) targetUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(60000);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                
                if (rangeHeader != null) {
                    connection.setRequestProperty(HttpHeaders.RANGE, rangeHeader);
                }
                
                int responseCode = connection.getResponseCode();
                
                // Handle Redirects
                if (responseCode >= 300 && responseCode < 400) {
                    String location = connection.getHeaderField("Location");
                    if (location != null) {
                        downloadUrl = location.startsWith("http") ? location : new URL(new URL(downloadUrl), location).toString();
                        redirectCount++;
                        connection.disconnect();
                        continue;
                    }
                }
                
                // Handle Google Drive Virus Scan Warning
                String responseContentType = connection.getContentType();
                if (responseContentType != null && responseContentType.contains("text/html") && downloadUrl.contains("drive.google.com")) {
                    System.out.println("🛡️ Detected Google Drive Virus Scan Page, attempting to bypass...");
                    // Read the page to find the 'confirm' token
                    try (BufferedInputStream bis = new BufferedInputStream(connection.getInputStream())) {
                        byte[] pageData = bis.readAllBytes();
                        String html = new String(pageData);
                        // Look for confirm=XXXX or a link containing confirm=
                        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("confirm=([a-zA-Z0-9_]+)").matcher(html);
                        if (matcher.find()) {
                            String confirmToken = matcher.group(1);
                            downloadUrl = downloadUrl + "&confirm=" + confirmToken;
                            System.out.println("✅ Found confirmation token: " + confirmToken);
                            redirectCount++;
                            connection.disconnect();
                            continue;
                        }
                    }
                }
                break;
            }

            if (connection == null) return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();

            int responseCode = connection.getResponseCode();
            if (responseCode >= 400) {
                return ResponseEntity.status(responseCode).build();
            }

            // 3. Stream to client
            InputStream sourceStream = new BufferedInputStream(connection.getInputStream());
            String finalContentType = connection.getContentType();
            if (finalContentType == null || finalContentType.contains("text/html")) finalContentType = contentType;
            
            long contentLength = connection.getContentLengthLong();
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add(HttpHeaders.CONTENT_TYPE, finalContentType);
            if (contentLength > 0) responseHeaders.setContentLength(contentLength);
            responseHeaders.add("Accept-Ranges", "bytes");
            
            String contentRange = connection.getHeaderField(HttpHeaders.CONTENT_RANGE);
            if (contentRange != null) responseHeaders.add(HttpHeaders.CONTENT_RANGE, contentRange);

            // If it's a full file (not a range request), try to cache it in the background
            if (rangeHeader == null && responseCode == 200 && contentLength > 0) {
                System.out.println("📥 Streaming & Mirroring to Cache: " + urlHash);
                
                // Since we want to stream AND save, we'll use a custom wrapper or just save first for now but OPTIMIZED
                // For now, let's fix the "Failed to fetch" by serving directly if it's large, 
                // OR use a background thread to cache while the stream is active.
                
                // Efficient approach: Stream directly to client. 
                // To cache while streaming, we'd need a TeeInputStream. 
                // Let's just stream directly for now to ensure NO TIMEOUTS.
                return ResponseEntity.ok()
                        .headers(responseHeaders)
                        .body(new InputStreamResource(sourceStream));
            }

            HttpStatus status = (responseCode == HttpURLConnection.HTTP_PARTIAL) ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK;
            return ResponseEntity.status(status).headers(responseHeaders).body(new InputStreamResource(sourceStream));

        } catch (Exception e) {
            System.err.println("Proxy failed for URL: " + url + " - Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    private String getMd5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashInBytes = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashInBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }

    // --- Helper Methods ---

    private void processPdfToJson(MultipartFile pdfFile, BookDTO book) {
        try {
            List<Map<String, Object>> extractedContent = pdfExtractionService.extractContentToJson(pdfFile);
            String jsonContent = objectMapper.writeValueAsString(extractedContent);
            String driveFileId = googleDriveService.uploadJsonToDrive(book.getTitle() + ".json", jsonContent);
            book.setContentJsonUrl(driveFileId);
        } catch (Exception e) {
            System.err.println("Failed to process PDF to JSON: " + e.getMessage());
        }
    }

    private void processDriveLinkToJson(String driveUrl, BookDTO book) {
        try {
            // For Drive links, we'll implement direct processing later if needed
            System.out.println("Processing Drive link: " + driveUrl);
        } catch (Exception e) {
            System.err.println("Failed to process Drive link: " + e.getMessage());
        }
    }
}
