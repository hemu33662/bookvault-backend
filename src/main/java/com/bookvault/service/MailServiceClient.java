package com.bookvault.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class MailServiceClient {

    @Value("${mail-service.url}")
    private String mailUrl;

    @Value("${mail-service.api-key}")
    private String apiKey;

    @Value("${mail-service.secret-key}")
    private String secretKey;

    @Value("${mail-service.sender}")
    private String senderEmail;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean sendEmail(String to, String subject, String body, String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            userId = "SYSTEM";
        }
        
        try {
            // Trim to avoid whitespace issues
            String cleanApiKey = apiKey.trim();
            String cleanSecretKey = secretKey.trim();
            String cleanUserId = userId.trim();
            
            long timestamp = System.currentTimeMillis();
            String timestampStr = String.valueOf(timestamp);
            
            // Re-generate signature with trimmed values
            String dataToSign = cleanApiKey + cleanUserId + timestampStr;
            
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(cleanSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getEncoder().encodeToString(hmacBytes);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-KEY", cleanApiKey);
            headers.set("X-USER-ID", cleanUserId);
            headers.set("X-TIMESTAMP", timestampStr);
            headers.set("X-SIGNATURE", signature);

            MultiValueMap<String, String> bodyMap = new LinkedMultiValueMap<>();
            bodyMap.add("from", senderEmail);
            bodyMap.add("to", to);
            bodyMap.add("subject", subject);
            bodyMap.add("body", body);

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(bodyMap, headers);

            ResponseEntity<String> response = restTemplate.exchange(mailUrl, org.springframework.http.HttpMethod.POST, requestEntity, String.class);
            
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            System.err.println("CRITICAL: Failed to send email to " + to);
            System.err.println("Reason: " + e.getMessage());
            System.err.println("API Key length: " + (apiKey != null ? apiKey.length() : "null"));
            System.err.println("Secret Key length: " + (secretKey != null ? secretKey.length() : "null"));
            return false;
        }
    }

    private String generateHmacSignature(String apiKey, String userId, long timestamp, String secretKey) throws Exception {
        // Keeping for compatibility but using inline logic above for better debugging
        String data = apiKey + userId + timestamp;
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
