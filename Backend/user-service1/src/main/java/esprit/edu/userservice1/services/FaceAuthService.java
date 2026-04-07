package esprit.edu.userservice1.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

@Service
public class FaceAuthService {

    private static final String PYTHON_URL = "http://127.0.0.1:5001/api/face";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public boolean registerFace(Long userId, List<Double> embedding) {
        try {
            Map<String, Object> body = Map.of(
                    "user_id",   userId,
                    "embedding", embedding
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    PYTHON_URL + "/register", entity, Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return Boolean.TRUE.equals(response.getBody().get("success"));
            }
            return false;

        } catch (Exception e) {
            System.err.println("❌ registerFace error: " + e.getMessage());
            return false;
        }
    }

    public Map<String, Object> verifyFace(Long userId, List<Double> embedding) {
        try {
            Map<String, Object> body = Map.of(
                    "user_id",   userId,
                    "embedding", embedding
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    PYTHON_URL + "/verify", entity, Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody(); // ✅ retourner Map complet
            }

            return Map.of("match", false);

        } catch (Exception e) {
            System.err.println("❌ verifyFace error: " + e.getMessage());
            return Map.of("match", false);
        }
    }

    public boolean isPythonServiceAlive() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    "http://127.0.0.1:5001/health", Map.class
            );
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            return false;
        }
    }
}