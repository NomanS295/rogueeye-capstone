package ca.sheridancollege.koonerga.web.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import ca.sheridancollege.koonerga.domain.Alert;
import ca.sheridancollege.koonerga.domain.ScanResult;
import ca.sheridancollege.koonerga.service.FlaskApiService;
import ca.sheridancollege.koonerga.service.S3Service; // ✅ Added import

public class HomeControllerTest {

    private FlaskApiService flaskApiService;
    private SimpMessagingTemplate messagingTemplate;
    private S3Service s3Service;
    private HomeController homeController;

    @BeforeEach
    void setUp() {
        flaskApiService = Mockito.mock(FlaskApiService.class);
        messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
        s3Service = Mockito.mock(S3Service.class); // ✅ new mock
        homeController = new HomeController(flaskApiService, messagingTemplate, s3Service); // ✅ updated constructor
    }

    @Test
    void testGetRecentScans() {
        ScanResult scanResult1 = new ScanResult();
        scanResult1.setEssid("Device1");
        scanResult1.setBssid("00:11:22:33:44:55");
        scanResult1.setChannel(1);
        scanResult1.setAvgPower(-45);
        scanResult1.setAuth("WPA2");
        scanResult1.setEnc("AES");
        scanResult1.setScannedAt(LocalDateTime.now());
        scanResult1.setWhitelistId(1L);

        ScanResult scanResult2 = new ScanResult();
        scanResult2.setEssid("Device2");
        scanResult2.setBssid("AA:BB:CC:DD:EE:FF");
        scanResult2.setChannel(6);
        scanResult2.setAvgPower(-50);
        scanResult2.setAuth("WPA3");
        scanResult2.setEnc("TKIP");
        scanResult2.setScannedAt(LocalDateTime.now());
        scanResult2.setWhitelistId(2L);

        List<ScanResult> scanResults = Arrays.asList(scanResult1, scanResult2);

        when(flaskApiService.getRecentScans()).thenReturn(scanResults);

        ResponseEntity<List<ScanResult>> response = homeController.getRecentScans();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(scanResults, response.getBody());
        verify(flaskApiService, times(1)).getRecentScans();
    }

    @Test
    void testGetRecentAlerts() {
        Alert alert1 = new Alert();
        Alert alert2 = new Alert();
        List<Alert> alerts = Arrays.asList(alert1, alert2);

        when(flaskApiService.getRecentAlerts()).thenReturn(alerts);

        ResponseEntity<List<Alert>> response = homeController.getRecentAlerts();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(alerts, response.getBody());
        verify(flaskApiService, times(1)).getRecentAlerts();
    }
}

