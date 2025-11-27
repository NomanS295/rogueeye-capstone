package ca.sheridancollege.koonerga.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ca.sheridancollege.koonerga.service.S3Service;

@Component
public class ScanAlertWebSocketHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final S3Service s3Service;

    @Autowired
    public ScanAlertWebSocketHandler(SimpMessagingTemplate messagingTemplate, S3Service s3Service) {
        this.messagingTemplate = messagingTemplate;
        this.s3Service = s3Service;
    }

    // 🔄 Send latest scan results every 10s
    @Scheduled(fixedRate = 10000)
    public void broadcastScanResults() {
        try {
            String scanJson = s3Service.getLatestScanResults();
            messagingTemplate.convertAndSend("/topic/scans", scanJson);
            System.out.println("[WebSocket] Sent scan data to /topic/scans");
        } catch (Exception e) {
            System.err.println("[WebSocket ERROR] Failed to send scans: " + e.getMessage());
        }
    }

    // 🔄 Send latest alerts every 15s
    @Scheduled(fixedRate = 15000)
    public void broadcastAlerts() {
        try {
            String alertJson = s3Service.getLatestAlerts();
            messagingTemplate.convertAndSend("/topic/alerts", alertJson);
            System.out.println("[WebSocket] Sent alert data to /topic/alerts");
        } catch (Exception e) {
            System.err.println("[WebSocket ERROR] Failed to send alerts: " + e.getMessage());
        }
    }
}

