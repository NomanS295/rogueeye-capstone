package ca.sheridancollege.koonerga.web;

import ca.sheridancollege.koonerga.infra.MqttPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/remote")
public class RemoteAdminController {

    private final MqttPublisher mqttPublisher;
    private final String PI_ID = "piagent"; // ✅ your Raspberry Pi client ID

    @Autowired
    public RemoteAdminController(MqttPublisher mqttPublisher) {
        this.mqttPublisher = mqttPublisher;
    }

    // ✅ Start Scan Command
    @PostMapping("/start-scan")
    public ResponseEntity<?> startScan() {
        try {
            mqttPublisher.sendCommand(PI_ID, "start_scan", new HashMap<>());
            System.out.println("🚀 [RemoteAdminController] Sent start_scan command to Pi.");
            return ResponseEntity.ok(Map.of("status", "Scan started", "command", "start_scan"));
        } catch (Exception e) {
            System.err.println("❌ Failed to send start_scan: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Stop Scan Command
    @PostMapping("/stop-scan")
    public ResponseEntity<?> stopScan() {
        try {
            mqttPublisher.sendCommand(PI_ID, "stop_scan", new HashMap<>());
            System.out.println("🛑 [RemoteAdminController] Sent stop_scan command to Pi.");
            return ResponseEntity.ok(Map.of("status", "Scan stopped", "command", "stop_scan"));
        } catch (Exception e) {
            System.err.println("❌ Failed to send stop_scan: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Update Allowlist
    @PostMapping("/allowlist")
    public ResponseEntity<?> updateAllowlist(@RequestBody Map<String, Object> payload) {
        try {
            mqttPublisher.sendCommand(PI_ID, "update_allowlist", payload);
            System.out.println("🧾 [RemoteAdminController] Sent allowlist update: " + payload);
            return ResponseEntity.ok(Map.of("status", "Allowlist updated", "payload", payload));
        } catch (Exception e) {
            System.err.println("❌ Failed to update allowlist: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Optional: Custom Command
    @PostMapping("/custom")
    public ResponseEntity<?> sendCustomCommand(@RequestBody Map<String, Object> payload) {
        try {
            mqttPublisher.sendCommand(PI_ID, "custom_command", payload);
            System.out.println("📨 [RemoteAdminController] Sent custom command: " + payload);
            return ResponseEntity.ok(Map.of("status", "Custom command sent", "payload", payload));
        } catch (Exception e) {
            System.err.println("❌ Failed to send custom command: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Health Check
    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of("status", "✅ RemoteAdminController active and ready"));
    }
}

