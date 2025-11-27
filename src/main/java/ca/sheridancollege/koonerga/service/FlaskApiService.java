package ca.sheridancollege.koonerga.service;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import ca.sheridancollege.koonerga.domain.Alert;
import ca.sheridancollege.koonerga.domain.ScanResult;

@Service
public class FlaskApiService {

    private final RestTemplate restTemplate;
    private final String flaskApiBaseUrl;

    public FlaskApiService(RestTemplate restTemplate,
                           @Value("${flask.api.base-url}") String flaskApiBaseUrl) {
        this.restTemplate = restTemplate;
        this.flaskApiBaseUrl = flaskApiBaseUrl;
    }

    public List<ScanResult> getRecentScans() {
        String url = flaskApiBaseUrl + "/scans";
        try {
            System.out.println("[INFO] Fetching scans from: " + url);
            ResponseEntity<List<ScanResult>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<ScanResult>>() {});
            return response.getBody();
        } catch (Exception e) {
            System.err.println("[WARN] Flask API unavailable (" + e.getMessage() + "). Returning empty list.");
            return Collections.emptyList();
        }
    }

    public List<Alert> getRecentAlerts() {
        String url = flaskApiBaseUrl + "/alerts";
        try {
            System.out.println("[INFO] Fetching alerts from: " + url);
            ResponseEntity<List<Alert>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<Alert>>() {});
            return response.getBody();
        } catch (Exception e) {
            System.err.println("[WARN] Flask API unavailable (" + e.getMessage() + "). Returning empty list.");
            return Collections.emptyList();
        }
    }
}

