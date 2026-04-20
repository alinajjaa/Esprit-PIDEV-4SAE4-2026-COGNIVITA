package com.alzheimer.medicalrecords.controller;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;
import com.alzheimer.medicalrecords.service.*;
import com.alzheimer.medicalrecords.user.*;
import com.alzheimer.medicalrecords.dto.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * PredictionController — proxies MRI/image uploads to the Python CNN service.
 *
 * FIX: FASTAPI_URL was hardcoded to http://127.0.0.1:8000/predict which fails
 * inside Docker (127.0.0.1 inside the container is the container itself, not the
 * Python service). It is now read from an environment variable so Docker Compose
 * can inject the correct container hostname.
 */
@RestController
@RequestMapping("/api")
public class PredictionController {

    @Value("${cnn.service.url:http://cnn-service:8000}/predict")
    private String fastApiUrl;

    @PostMapping("/predict")
    public ResponseEntity<String> predict(@RequestParam("file") MultipartFile file) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", file.getResource());

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(fastApiUrl, request, String.class);

            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error calling AI service: " + e.getMessage());
        }
    }
}
