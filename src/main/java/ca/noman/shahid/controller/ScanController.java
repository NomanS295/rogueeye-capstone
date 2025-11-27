package ca.noman.shahid.controller;

import ca.sheridancollege.koonerga.service.S3Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/scans")
@CrossOrigin(origins = "*")
public class ScanController {

    private final S3Service s3;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ScanController(S3Service s3, SimpMessagingTemplate messagingTemplate) {
        this.s3 = s3;
        this.messagingTemplate = messagingTemplate;
    }

    // === Primary Endpoint: /api/scans/results ===
    @GetMapping(value = "/results", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> results() {
        String data = s3.getLatestScanResults();
        try {
            messagingTemplate.convertAndSend("/topic/scans", data);
        } catch (Exception e) {
            System.err.println("[⚠️ WS ERROR] Could not send scans: " + e.getMessage());
        }
        return ResponseEntity.ok(data);
    }

    // === Primary Endpoint: /api/scans/alerts ===
    @GetMapping(value = "/alerts", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> alerts() {
        try {
            String alertsJson = s3.getLatestAlerts();
            if (alertsJson == null || alertsJson.isBlank()) {
                return ResponseEntity.ok("[]");
            }

            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> alerts = mapper.readValue(alertsJson, new TypeReference<>() {});

            // ✅ Sort newest → oldest (by detected_at)
            alerts.sort((a, b) -> {
                String t1 = (String) a.getOrDefault("detected_at", "");
                String t2 = (String) b.getOrDefault("detected_at", "");
                return t2.compareTo(t1); // descending
            });

            // ✅ Limit to max 100 safely
            int max = 100;
            if (alerts.size() > max) {
                alerts = new ArrayList<>(alerts.subList(0, max));
            }

            // ✅ Convert back to JSON
            String limitedJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(alerts);

            // ✅ Broadcast to WebSocket
            try {
                messagingTemplate.convertAndSend("/topic/alerts", limitedJson);
            } catch (Exception e) {
                System.err.println("[⚠️ WS ERROR] Could not send alerts: " + e.getMessage());
            }

            return ResponseEntity.ok(limitedJson);

        } catch (Exception e) {
            System.err.println("[⚠️ ALERTS ERROR] " + e.getMessage());
            return ResponseEntity.internalServerError().body("[]");
        }
    }

    // === Legacy Alias: /api/access/scans ===
    @GetMapping(value = "/access", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getLegacyAccessScans() {
        String data = s3.getLatestScanResults();
        try {
            messagingTemplate.convertAndSend("/topic/scans", data);
        } catch (Exception e) {
            System.err.println("[⚠️ WS ERROR] Could not send legacy scans: " + e.getMessage());
        }
        return ResponseEntity.ok(data);
    }
}

