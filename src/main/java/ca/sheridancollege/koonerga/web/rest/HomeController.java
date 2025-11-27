package ca.sheridancollege.koonerga.web.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import ca.sheridancollege.koonerga.domain.Alert;
import ca.sheridancollege.koonerga.domain.ScanResult;
import ca.sheridancollege.koonerga.service.FlaskApiService;
import ca.sheridancollege.koonerga.service.S3Service;

import java.util.List;

@RestController
@RequestMapping("/api/access")
public class HomeController {

    private final FlaskApiService flaskApiService;
    private final SimpMessagingTemplate messagingTemplate;
    private final S3Service s3Service;

    public HomeController(
            FlaskApiService flaskApiService,
            SimpMessagingTemplate messagingTemplate,
            S3Service s3Service) {
        this.flaskApiService = flaskApiService;
        this.messagingTemplate = messagingTemplate;
        this.s3Service = s3Service;
    }

    // --- Flask API endpoints (live Pi data)
    @GetMapping("/scans")
    public ResponseEntity<List<ScanResult>> getRecentScans() {
        return ResponseEntity.ok(flaskApiService.getRecentScans());
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<Alert>> getRecentAlerts() {
        return ResponseEntity.ok(flaskApiService.getRecentAlerts());
    }

    // --- S3 endpoints (historical data)
    @GetMapping("/scans-s3")
    public ResponseEntity<String> getScansFromS3() {
        return ResponseEntity.ok(s3Service.getLatestScanResults());
    }

    @GetMapping("/alerts-s3")
    public ResponseEntity<String> getAlertsFromS3() {
        return ResponseEntity.ok(s3Service.getLatestAlerts());
    }
}

