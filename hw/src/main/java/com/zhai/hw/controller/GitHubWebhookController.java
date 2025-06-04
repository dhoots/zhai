package com.zhai.hw.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhai.hw.exception.WebhookValidationException;
import com.zhai.hw.model.github.WorkflowJobEvent;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Controller for handling GitHub webhook events.
 */
@RestController
@RequestMapping("/webhook")
public class GitHubWebhookController {
    
    private static final Logger logger = LoggerFactory.getLogger(GitHubWebhookController.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_HEADER = "X-Hub-Signature-256";
    
    private final ObjectMapper objectMapper;
    
    @Value("${GITHUB_WEBHOOK_SECRET:}")
    private String webhookSecret;
    
    public GitHubWebhookController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * Handles GitHub webhook events.
     *
     * @param payload   The raw JSON payload from GitHub
     * @param signature The signature from the X-Hub-Signature-256 header
     * @return Response entity with status and message
     */
    @PostMapping(value = "/github", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleGitHubWebhook(
            @RequestBody String payload,
            @RequestHeader(value = SIGNATURE_HEADER, required = false) String signature) {
        
        try {
            // Validate webhook signature if secret is configured
            validateSignature(payload, signature);
            
            // Parse the payload
            WorkflowJobEvent event = objectMapper.readValue(payload, WorkflowJobEvent.class);
            
            // Log event details
            logger.info("Received GitHub webhook event: action={}, job_id={}, labels={}",
                    event.getAction(), event.getWorkflowJob().getId(), event.getWorkflowJob().getLabels());
            
            // Process based on action
            if ("queued".equals(event.getAction())) {
                logger.info("Processing queued workflow job event for repository: {}", 
                        event.getWorkflowJob().getRepository().getFullName());
                // Future implementation: Process the job
            } else {
                logger.info("Ignoring non-queued workflow job event: {}", event.getAction());
            }
            
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (WebhookValidationException e) {
            logger.error("Webhook validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing GitHub webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid payload format: " + e.getMessage());
        }
    }
    
    /**
     * Validates the webhook signature.
     *
     * @param payload   The raw payload
     * @param signature The signature from the X-Hub-Signature-256 header
     * @throws WebhookValidationException if validation fails
     */
    private void validateSignature(String payload, String signature) {
        // If secret is not configured, log warning and proceed
        if (!StringUtils.hasText(webhookSecret)) {
            logger.warn("GitHub webhook secret not configured. Skipping signature validation.");
            return;
        }
        
        // If signature is missing, reject
        if (!StringUtils.hasText(signature)) {
            throw new WebhookValidationException("Missing X-Hub-Signature-256 header");
        }
        
        try {
            // Calculate expected signature
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = "sha256=" + bytesToHex(digest);
            
            // Compare signatures
            if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), 
                                       signature.getBytes(StandardCharsets.UTF_8))) {
                throw new WebhookValidationException("Invalid signature");
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new WebhookValidationException("Error validating webhook signature", e);
        }
    }
    
    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes The byte array
     * @return The hexadecimal string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
