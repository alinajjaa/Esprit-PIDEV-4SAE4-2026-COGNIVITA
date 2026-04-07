package esprit.edu.userservice1.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;;

@ExtendWith(MockitoExtension.class)
class FaceAuthServiceTest {

    @InjectMocks
    private FaceAuthService faceAuthService;

    @Mock
    private RestTemplate restTemplate;

    // Injecter le mock RestTemplate dans le service via reflection
    // (car RestTemplate est instancié dans le service avec `new`)
    @BeforeEach
    void injectRestTemplate() throws Exception {
        Field field = FaceAuthService.class.getDeclaredField("restTemplate");
        field.setAccessible(true);
        field.set(faceAuthService, restTemplate);
    }

    private static final List<Double> EMBEDDING = List.of(0.1, 0.2, 0.3, 0.4);

    // ══════════════════════════════════════════
    // REGISTER FACE
    // ══════════════════════════════════════════

    @Test
    void registerFace_ShouldReturnTrue_WhenPythonReturnsSuccess() {
        Map<String, Object> pythonResponse = Map.of("success", true);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(pythonResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(
                contains("/register"), any(HttpEntity.class), eq(Map.class)
        )).thenReturn(responseEntity);

        boolean result = faceAuthService.registerFace(1L, EMBEDDING);

        assertTrue(result);
    }

    @Test
    void registerFace_ShouldReturnFalse_WhenPythonReturnsSuccessFalse() {
        Map<String, Object> pythonResponse = Map.of("success", false);
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(pythonResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(
                contains("/register"), any(HttpEntity.class), eq(Map.class)
        )).thenReturn(responseEntity);

        boolean result = faceAuthService.registerFace(1L, EMBEDDING);

        assertFalse(result);
    }

    @Test
    void registerFace_ShouldReturnFalse_WhenPythonReturnsNonOkStatus() {
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);

        when(restTemplate.postForEntity(
                contains("/register"), any(HttpEntity.class), eq(Map.class)
        )).thenReturn(responseEntity);

        boolean result = faceAuthService.registerFace(1L, EMBEDDING);

        assertFalse(result);
    }

    @Test
    void registerFace_ShouldReturnFalse_WhenPythonThrowsException() {
        when(restTemplate.postForEntity(
                contains("/register"), any(HttpEntity.class), eq(Map.class)
        )).thenThrow(new RestClientException("Connection refused"));

        boolean result = faceAuthService.registerFace(1L, EMBEDDING);

        assertFalse(result);
    }

    // ══════════════════════════════════════════
    // VERIFY FACE
    // ══════════════════════════════════════════

    @Test
    void verifyFace_ShouldReturnMatchTrue_WhenFaceRecognized() {
        Map<String, Object> pythonResponse = Map.of(
                "match",            true,
                "score",            0.95,
                "confidence_level", "HIGH"
        );
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(pythonResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(
                contains("/verify"), any(HttpEntity.class), eq(Map.class)
        )).thenReturn(responseEntity);

        Map<String, Object> result = faceAuthService.verifyFace(1L, EMBEDDING);

        assertTrue((Boolean) result.get("match"));
        assertEquals(0.95, result.get("score"));
        assertEquals("HIGH", result.get("confidence_level"));
    }

    @Test
    void verifyFace_ShouldReturnMatchFalse_WhenFaceNotRecognized() {
        Map<String, Object> pythonResponse = Map.of(
                "match",            false,
                "score",            0.2,
                "confidence_level", "REFUSED"
        );
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(pythonResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(
                contains("/verify"), any(HttpEntity.class), eq(Map.class)
        )).thenReturn(responseEntity);

        Map<String, Object> result = faceAuthService.verifyFace(1L, EMBEDDING);

        assertFalse((Boolean) result.get("match"));
    }

    @Test
    void verifyFace_ShouldReturnMatchFalse_WhenPythonReturnsNonOkStatus() {
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);

        when(restTemplate.postForEntity(
                contains("/verify"), any(HttpEntity.class), eq(Map.class)
        )).thenReturn(responseEntity);

        Map<String, Object> result = faceAuthService.verifyFace(1L, EMBEDDING);

        assertFalse((Boolean) result.get("match"));
    }

    @Test
    void verifyFace_ShouldReturnMatchFalse_WhenExceptionThrown() {
        when(restTemplate.postForEntity(
                contains("/verify"), any(HttpEntity.class), eq(Map.class)
        )).thenThrow(new RestClientException("Timeout"));

        Map<String, Object> result = faceAuthService.verifyFace(1L, EMBEDDING);

        assertFalse((Boolean) result.get("match"));
    }

    // ══════════════════════════════════════════
    // IS PYTHON SERVICE ALIVE
    // ══════════════════════════════════════════

    @Test
    void isPythonServiceAlive_ShouldReturnTrue_WhenHealthOk() {
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(Map.of("status", "running"), HttpStatus.OK);

        when(restTemplate.getForEntity(
                contains("/health"), eq(Map.class)
        )).thenReturn(responseEntity);

        assertTrue(faceAuthService.isPythonServiceAlive());
    }

    @Test
    void isPythonServiceAlive_ShouldReturnFalse_WhenPythonDown() {
        when(restTemplate.getForEntity(
                contains("/health"), eq(Map.class)
        )).thenThrow(new RestClientException("Connection refused"));

        assertFalse(faceAuthService.isPythonServiceAlive());
    }

    @Test
    void isPythonServiceAlive_ShouldReturnFalse_WhenNonOkStatus() {
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(null, HttpStatus.SERVICE_UNAVAILABLE);

        when(restTemplate.getForEntity(
                contains("/health"), eq(Map.class)
        )).thenReturn(responseEntity);

        assertFalse(faceAuthService.isPythonServiceAlive());
    }
}