package ca.sheridancollege.koonerga.infra;

import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;

@Component
public class MqttPublisher {

    private final ObjectMapper om = new ObjectMapper();
    private final String brokerUrl;
    private final String username;
    private final String password;
    private final String topicPrefix;
    private MqttClient client;

    public MqttPublisher(
        @Value("${rapd.mqtt.host}") String host,
        @Value("${rapd.mqtt.port}") int port,
        @Value("${rapd.mqtt.username}") String username,
        @Value("${rapd.mqtt.password}") String password,
        @Value("${rapd.mqtt.topicPrefix}") String topicPrefix
    ) throws MqttException {
        this.brokerUrl = "tcp://" + host + ":" + port;
        this.username = username;
        this.password = password;
        this.topicPrefix = topicPrefix;
        connect();
    }

    private void connect() throws MqttException {
        client = new MqttClient(brokerUrl, MqttClient.generateClientId());
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setCleanSession(true);
        opts.setUserName(username);
        opts.setPassword(password.toCharArray());
        client.connect(opts);
    }

    // ✅ Fixed: publishes to rapd/commands with {"action": "<command>"} payload
    public void sendCommand(String piId, String command, Map<String, Object> args) throws Exception {
        if (!client.isConnected()) connect();

        // Build payload as expected by Pi listener
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", command);
        if (args != null && !args.isEmpty()) {
            payload.put("args", args);
        }

        byte[] bytes = om.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8);

        // Send to topic 'rapd/commands'
        String topic = topicPrefix + "/commands";
        client.publish(topic, new MqttMessage(bytes));

        System.out.println("📡 Published to topic: " + topic + " | Payload: " + om.writeValueAsString(payload));
    }
}

