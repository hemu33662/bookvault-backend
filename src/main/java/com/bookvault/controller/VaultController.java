package com.bookvault.controller;

import com.bookvault.dto.BookDTO;
import com.bookvault.service.BookService;
import com.bookvault.service.SecurityService;
import com.bookvault.repository.BookAccessRepository;
import com.bookvault.repository.UserRepository;
import com.bookvault.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/vault")
public class VaultController {

    @Autowired
    private BookService bookService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private BookAccessRepository bookAccessRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/{bookId}/{fileName}")
    public ResponseEntity<?> getSecureFile(@PathVariable String bookId, @PathVariable String fileName) {
        try {
            // 1. Get current User & Authorization
            String firebaseUid = securityService.getCurrentUserFirebaseUid();
            String email = securityService.getCurrentUserEmail();
            boolean isUser = securityService.isUser();
            boolean isAdmin = securityService.isAdmin(); // Keep for logging/super-access

            Optional<BookDTO> bookOpt = bookService.getBookById(bookId);
            if (bookOpt.isEmpty()) return ResponseEntity.notFound().build();
            BookDTO book = bookOpt.get();

            boolean isFree = book.getPrice() == null || book.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0 || "FREE".equals(book.getType());
            boolean isCover = fileName.toLowerCase().contains("cover") || fileName.toLowerCase().endsWith(".png") || fileName.toLowerCase().endsWith(".jpg");
            
            // Access logic: Free books and covers are public. 
            // Paid books require a specific purchase record (User must own the book).
            // Administrative bypass removed for the reader component per user request.
            boolean hasAccess = isFree || isCover || (isUser && hasAccess(firebaseUid, bookId));
            
            System.out.println("🛡️ Vault Access Check: [File=" + fileName + ", User=" + (email != null ? email : "Anonymous") + 
                               ", UID=" + (firebaseUid != null ? firebaseUid : "N/A") + 
                               ", isUser=" + isUser + ", isAdmin=" + isAdmin + ", isFree=" + isFree + ", isCover=" + isCover + 
                               ", hasAccess=" + hasAccess + "]");

            if (!hasAccess) {
                System.err.println("❌ Vault: Access Denied for " + (email != null ? email : "Anonymous") + " to " + fileName);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access Denied: Purchase required.");
            }

            // 2. Locate or Fetch File
            // We use a unique prefix to prevent collisions between different books
            String vaultFileName = "vault_" + bookId + "_" + fileName;
            Path filePath = Paths.get("uploads").resolve(vaultFileName);
            File file = filePath.toFile();

            // If file doesn't exist locally or is suspiciously small (likely corrupted/error page), capture it!
            if (!file.exists() || file.length() < 1024) {
                String sourceUrl = null;
                if (fileName.equals("cover.vault")) sourceUrl = book.getCoverImageUrl();
                else if (fileName.equals("book.vault")) sourceUrl = book.getPdfUrl();
                else {
                    Path directPath = Paths.get("uploads").resolve(fileName);
                    if (directPath.toFile().exists() && directPath.toFile().length() > 1024) {
                        filePath = directPath;
                        file = directPath.toFile();
                    } else {
                        sourceUrl = book.getPdfUrl() != null && book.getPdfUrl().contains(fileName) ? book.getPdfUrl() : book.getCoverImageUrl();
                    }
                }

                if (sourceUrl != null && (sourceUrl.contains("drive.google.com") || sourceUrl.contains("archive.org"))) {
                    System.out.println("🛡️ Vault Capturing Remote Asset: " + vaultFileName + " from " + sourceUrl);
                    boolean success = downloadAndVault(sourceUrl, filePath);
                    if (!success) {
                        return ResponseEntity.notFound().build();
                    }
                    file = filePath.toFile();
                } else if (!file.exists()) {
                    return ResponseEntity.notFound().build();
                }
            }

            // 3. Secure Stream
            InputStreamResource resource = new InputStreamResource(new FileInputStream(file));
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, vaultFileName.toLowerCase().contains("book") || vaultFileName.toLowerCase().endsWith(".vault") ? 
                       "application/octet-stream" : "image/png");
            headers.add("X-Vault-Secured", "true");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(file.length())
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private boolean downloadAndVault(String sourceUrl, Path targetPath) {
        // Initialize Cookie Manager for Google Drive sessions
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);

        try {
            Files.createDirectories(targetPath.getParent());
        } catch (Exception e) {
            System.err.println("Failed to create vault directory: " + e.getMessage());
        }

        HttpURLConnection connection = null;
        try {
            String downloadUrl = sourceUrl;
            
            // Normalize URLs for common platforms
            if (downloadUrl.contains("archive.org/details/")) {
                String identifier = downloadUrl.split("/details/")[1].split("/")[0].split("\\?")[0];
                // Point to the download directory so our discovery logic can find the PDF
                downloadUrl = "https://archive.org/download/" + identifier + "/";
                System.out.println("🛡️ Vault: Targeted Archive.org Download Directory: " + downloadUrl);
            } else if (downloadUrl.contains("drive.google.com/file/d/")) {
                String id = downloadUrl.split("/d/")[1].split("/")[0].split("\\?")[0];
                downloadUrl = "https://drive.google.com/uc?export=download&id=" + id;
            } else if (downloadUrl.contains("drive.google.com/open?id=")) {
                String id = downloadUrl.split("id=")[1].split("&")[0];
                downloadUrl = "https://drive.google.com/uc?export=download&id=" + id;
            }

            int redirectCount = 0;
            while (redirectCount < 15) {
                URL url = new URL(downloadUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setInstanceFollowRedirects(true);
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(300000);
                
                // Use a very common browser User-Agent
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36");
                connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8");
                connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9");

                int responseCode = connection.getResponseCode();
                String contentType = connection.getContentType();

                // If it's HTML, we need to look for the "confirm" token
                if (contentType != null && contentType.contains("text/html")) {
                    System.out.println("🛡️ Vault: Processing HTML response for " + downloadUrl);
                    try (BufferedInputStream bis = new BufferedInputStream(connection.getInputStream())) {
                        byte[] pageData = bis.readAllBytes();
                        String html = new String(pageData);
                        
                        // EXTENDED EXTRACTION: Try multiple patterns for confirm token
                        String confirmToken = null;
                        String[] patterns = {
                            "confirm=([a-zA-Z0-9_-]+)",
                            "confirm\"\\s*:\\s*\"([a-zA-Z0-9_-]+)\"",
                            "confirm=([^&\"'\\s>]+)",
                            "download_warning_[^=]+=([a-zA-Z0-9_-]+)"
                        };
                        
                        for (String p : patterns) {
                            Matcher matcher = Pattern.compile(p).matcher(html);
                            if (matcher.find()) {
                                confirmToken = matcher.group(1);
                                break;
                            }
                        }

                        // Archive.org Special Case: If we hit a directory listing, find the PDF
                        if (confirmToken == null && downloadUrl.contains("archive.org/")) {
                            Matcher pdfMatcher = Pattern.compile("href=\"([^\"]+\\.pdf)\"").matcher(html);
                            if (pdfMatcher.find()) {
                                String pdfPath = pdfMatcher.group(1);
                                if (!pdfPath.startsWith("http")) {
                                    // Resolve relative path
                                    String base = downloadUrl.substring(0, downloadUrl.lastIndexOf("/") + 1);
                                    downloadUrl = base + pdfPath;
                                } else {
                                    downloadUrl = pdfPath;
                                }
                                System.out.println("🛡️ Vault: Discovered Archive.org PDF: " + downloadUrl);
                                redirectCount++;
                                connection.disconnect();
                                continue;
                            }
                        }

                        if (confirmToken != null) {
                            System.out.println("🛡️ Vault: Found Confirm Token: " + confirmToken);
                            if (downloadUrl.contains("confirm=")) {
                                downloadUrl = downloadUrl.replaceAll("confirm=[^&]*", "confirm=" + confirmToken);
                            } else {
                                downloadUrl = (downloadUrl.contains("?") ? downloadUrl + "&" : downloadUrl + "?") + "confirm=" + confirmToken;
                            }
                            
                            redirectCount++;
                            connection.disconnect();
                            continue;
                        } else {
                            // DIAGNOSTIC: Save the HTML to a file so we can see what's happening
                            Path errorLog = Paths.get("uploads", "vault_error_" + System.currentTimeMillis() + ".html");
                            Files.write(errorLog, pageData);
                            System.err.println("❌ Vault: Security screen detected but no token found. Saved HTML to " + errorLog.getFileName());
                            
                            if (html.contains("Google Drive - Virus scan warning")) {
                                 System.err.println("❌ Vault: Virus scan warning detected but no confirm token found.");
                            } else if (html.contains("Automated queries") || html.contains("CAPTCHA")) {
                                 System.err.println("❌ Vault: Google detected automated traffic / CAPTCHA required.");
                            }
                            return false;
                        }
                    }
                }

                if (responseCode == 200) {
                    break; 
                } else if (responseCode >= 300 && responseCode < 400) {
                    String location = connection.getHeaderField("Location");
                    if (location != null) {
                        downloadUrl = location.startsWith("http") ? location : new URL(new URL(downloadUrl), location).toString();
                        redirectCount++;
                        connection.disconnect();
                        continue;
                    }
                }
                
                System.err.println("❌ Vault: Server returned code " + responseCode + " for " + downloadUrl);
                return false;
            }

            if (connection == null || connection.getResponseCode() != 200) return false;

            // Stream and Validate
            System.out.println("🚀 Vault: Starting Stream-to-Vault Capture...");
            try (InputStream is = new BufferedInputStream(connection.getInputStream());
                 java.io.FileOutputStream os = new java.io.FileOutputStream(targetPath.toFile())) {
                
                byte[] buffer = new byte[32768]; // Increased buffer size for efficiency
                int bytesRead;
                long totalBytes = 0;
                boolean headerChecked = false;
                
                while ((bytesRead = is.read(buffer)) != -1) {
                    if (!headerChecked && bytesRead >= 4) {
                        // Check for PDF (%PDF) or known HTML starts
                        boolean isPdf = buffer[0] == 0x25 && buffer[1] == 0x50 && buffer[2] == 0x44 && buffer[3] == 0x46;
                        boolean isHtml = (buffer[0] == 0x3C && buffer[1] == 0x21) || (buffer[0] == 0x3C && buffer[1] == 0x68); 
                        
                        if (isHtml) {
                            System.err.println("❌ Vault: Aborting capture - stream starts with HTML tag.");
                            os.close();
                            Files.deleteIfExists(targetPath);
                            return false;
                        }
                        headerChecked = true;
                    }

                    // XOR Encrypt
                    for (int i = 0; i < bytesRead; i++) {
                        buffer[i] ^= 0x42;
                    }
                    os.write(buffer, 0, bytesRead);
                    totalBytes += bytesRead;
                }
                
                if (totalBytes < 512) { // Lowered threshold slightly for very small valid assets
                    System.err.println("❌ Vault: Captured file is too small (" + totalBytes + " bytes). Possible corruption.");
                    os.close();
                    Files.deleteIfExists(targetPath);
                    return false;
                }
                
                System.out.println("✅ Vault: Successfully captured " + totalBytes + " bytes to " + targetPath.getFileName());
            }
            return true;

        } catch (Exception e) {
            System.err.println("❌ Vault Capture Failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private boolean hasAccess(String firebaseUid, String bookId) {
        if (firebaseUid == null) return false;
        try {
            // 1. Check with Firebase UID directly
            if (bookAccessRepository.findByUserIdAndBookId(firebaseUid, bookId).isPresent()) return true;
            
            // 2. Check internal User record
            Optional<com.bookvault.entity.User> userOpt = userRepository.findByFirebaseUid(firebaseUid);
            if (userOpt.isPresent()) {
                com.bookvault.entity.User user = userOpt.get();
                if (bookAccessRepository.findByUserIdAndBookId(user.getId(), bookId).isPresent()) return true;
                if (user.getEmail() != null && bookAccessRepository.findByUserIdAndBookId(user.getEmail(), bookId).isPresent()) return true;
            }
            
            // 3. Email fallback from JWT
            String email = securityService.getCurrentUserEmail();
            if (email != null && bookAccessRepository.findByUserIdAndBookId(email, bookId).isPresent()) return true;
            
            System.out.println("🛡️ Vault Access Denied: User " + firebaseUid + " does not own book " + bookId);
            return false;
        } catch (Exception e) {
            System.err.println("Access check failed for book " + bookId + ": " + e.getMessage());
            return false;
        }
    }
}
