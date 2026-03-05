package com.alzheimer.admin;

import com.alzheimer.admin.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminDashboardController {

  private final RestClient userServiceClient;
  private final RestClient mmseServiceClient;
  private final RestClient medicalServiceClient;

  public AdminDashboardController() {
    this.userServiceClient = RestClient.builder().baseUrl("http://localhost:8082").build();
    this.mmseServiceClient = RestClient.builder().baseUrl("http://localhost:8085").build();
    this.medicalServiceClient = RestClient.builder().baseUrl("http://localhost:8083").build();
  }

  @GetMapping("/health")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemHealth() {
    Map<String, Object> health = Map.of(
        "status", "UP",
        "timestamp", LocalDateTime.now().toString()
    );
    return ResponseEntity.ok(ApiResponse.success("System health retrieved", health));
  }

  @GetMapping("/users-count")
  public ResponseEntity<ApiResponse<Long>> getUsersCount() {
    try {
      Long count = userServiceClient.get()
          .uri("/api/users/count")
          .retrieve()
          .body(Long.class);
      return ResponseEntity.ok(ApiResponse.success("User count retrieved", count == null ? 0L : count));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve user count", e.getMessage()));
    }
  }

  @GetMapping("/mmse-tests-count")
  public ResponseEntity<ApiResponse<Long>> getMMSETestsCount() {
    try {
      Long count = mmseServiceClient.get()
          .uri("/api/mmse/count/raw")
          .retrieve()
          .body(Long.class);
      return ResponseEntity.ok(ApiResponse.success("MMSE tests count retrieved", count == null ? 0L : count));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve MMSE tests count", e.getMessage()));
    }
  }

  @GetMapping("/dashboard")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
    try {
      long usersCount = getLongOrZeroFrom(userServiceClient, "/api/users/count");
      long activeUsers = getLongOrZeroFrom(userServiceClient, "/api/users/active/count");
      long mmseCount = getLongOrZeroFrom(mmseServiceClient, "/api/mmse/count/raw");
      long medicalCount;
      try {
        medicalCount = getLongOrZeroFrom(medicalServiceClient, "/api/medical-records/stats", "totalRecords");
      } catch (Exception ignored) {
        medicalCount = 0L;
      }

      Map<String, Object> dashboard = Map.of(
          "usersCount", usersCount,
          "activeUsersCount", activeUsers,
          "mmseTestsCount", mmseCount,
          "medicalRecordsCount", medicalCount,
          "generatedAt", LocalDateTime.now().toString()
      );

      return ResponseEntity.ok(ApiResponse.success("Dashboard retrieved successfully", dashboard));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve dashboard", e.getMessage()));
    }
  }

  // Compatibility endpoint for existing frontend calls
  @GetMapping("/super-dashboard")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getSuperDashboard() {
    return getDashboard();
  }

  @GetMapping("/stats")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
    return getDashboard();
  }

  @GetMapping("/users-with-mmse")
  public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUsersWithMMSE() {
    try {
      List<Map<String, Object>> users = fetchUsers();
      List<Map<String, Object>> mmseTests = fetchMmseTests();

      // Pre-group MMSE tests by patientName (case-insensitive)
      Map<String, List<Map<String, Object>>> testsByName = mmseTests.stream()
          .collect(Collectors.groupingBy(
              t -> String.valueOf(Optional.ofNullable(t.get("patientName")).orElse("")).trim().toLowerCase(),
              Collectors.toList()
          ));

      List<Map<String, Object>> enriched = users.stream()
          .map(u -> {
            Map<String, Object> map = new LinkedHashMap<>(u);
            String fullName = (String.valueOf(Optional.ofNullable(u.get("firstName")).orElse("")).trim() + " " +
                String.valueOf(Optional.ofNullable(u.get("lastName")).orElse("")).trim()).trim();

            List<Map<String, Object>> tests = testsByName.getOrDefault(fullName.toLowerCase(), List.of());
            Object userId = u.containsKey("userId") ? u.get("userId") : u.get("id");
            map.put("userId", userId); // frontend expects userId
            map.put("mmseTests", tests);
            return map;
          })
          .collect(Collectors.toList());

      return ResponseEntity.ok(ApiResponse.success("Users with MMSE tests retrieved successfully", enriched));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve users/MMSE data", e.getMessage()));
    }
  }

  @GetMapping("/mmse-tests")
  public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllMmseTests() {
    try {
      List<Map<String, Object>> tests = fetchMmseTests();

      // Sort by testDate descending when present
      List<Map<String, Object>> sorted = tests.stream()
          .sorted(Comparator.comparing(
              t -> String.valueOf(t.getOrDefault("testDate", "")),
              Comparator.reverseOrder()
          ))
          .collect(Collectors.toList());

      return ResponseEntity.ok(ApiResponse.success("MMSE tests retrieved successfully", sorted));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve MMSE tests", e.getMessage()));
    }
  }

  @GetMapping("/medical-records")
  public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMedicalRecords() {
    try {
      List<Map<String, Object>> records = fetchMedicalRecords();
      return ResponseEntity.ok(ApiResponse.success("Medical records retrieved successfully", records));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve medical records", e.getMessage()));
    }
  }

  @PutMapping("/medical-records/{id}")
  public ResponseEntity<ApiResponse<Map<String, Object>>> updateMedicalRecord(
      @PathVariable Long id,
      @RequestBody Map<String, Object> request
  ) {
    try {
      Map<?, ?> response = medicalServiceClient.put()
          .uri("/api/medical-records/{id}", id)
          .body(request)
          .retrieve()
          .body(Map.class);

      Object data = response == null ? null : response.get("data");
      if (!(data instanceof Map<?, ?>)) {
        return ResponseEntity.ok(ApiResponse.success("Medical record updated", Map.of()));
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> updated = (Map<String, Object>) data;
      return ResponseEntity.ok(ApiResponse.success("Medical record updated successfully", updated));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to update medical record", e.getMessage()));
    }
  }

  @DeleteMapping("/medical-records/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteMedicalRecord(@PathVariable Long id) {
    try {
      medicalServiceClient.delete()
          .uri("/api/medical-records/{id}", id)
          .retrieve()
          .toBodilessEntity();

      return ResponseEntity.ok(ApiResponse.success("Medical record deleted successfully", null));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to delete medical record", e.getMessage()));
    }
  }

  @GetMapping("/search")
  public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchUsers(@RequestParam String query) {
    try {
      if (query == null || query.trim().isEmpty()) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Search query cannot be empty", "Invalid query"));
      }

      String q = query.toLowerCase();
      List<Map<String, Object>> users = fetchUsers();
      List<Map<String, Object>> result = users.stream()
          .filter(u -> containsIgnoreCase(u, "firstName", q)
              || containsIgnoreCase(u, "lastName", q)
              || containsIgnoreCase(u, "email", q))
          .collect(Collectors.toList());

      return ResponseEntity.ok(ApiResponse.success("Search completed successfully", result));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Search failed", e.getMessage()));
    }
  }

  @GetMapping("/filter")
  public ResponseEntity<ApiResponse<List<Map<String, Object>>>> filterUsers(
      @RequestParam(required = false) String role,
      @RequestParam(required = false) Boolean active
  ) {
    try {
      List<Map<String, Object>> users = fetchUsers();

      if (role != null && !role.isBlank()) {
        String roleLower = role.toLowerCase();
        users = users.stream()
            .filter(u -> {
              Object r = u.get("role");
              return r != null && r.toString().toLowerCase().equals(roleLower);
            })
            .collect(Collectors.toList());
      }

      if (active != null) {
        users = users.stream()
            .filter(u -> Objects.equals(u.get("active"), active))
            .collect(Collectors.toList());
      }

      return ResponseEntity.ok(ApiResponse.success("Filter applied successfully", users));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Filter failed", e.getMessage()));
    }
  }

  @GetMapping("/export/users")
  public ResponseEntity<ApiResponse<List<Map<String, Object>>>> exportUsers() {
    try {
      List<Map<String, Object>> users = fetchUsers();
      List<Map<String, Object>> data = users.stream()
          .map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("ID", u.get("id"));
            map.put("Email", u.get("email"));
            map.put("Name", (u.get("firstName") == null ? "" : u.get("firstName")) + " " + (u.get("lastName") == null ? "" : u.get("lastName")));
            map.put("Role", u.get("role"));
            map.put("Status", Boolean.TRUE.equals(u.get("active")) ? "Active" : "Inactive");
            map.put("Phone", u.get("phone"));
            map.put("Created", u.get("createdAt"));
            return map;
          })
          .collect(Collectors.toList());

      return ResponseEntity.ok(ApiResponse.success("Users exported successfully", data));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Export failed", e.getMessage()));
    }
  }

  @GetMapping("/export/mmse")
  public ResponseEntity<ApiResponse<List<Map<String, Object>>>> exportMMSETests() {
    try {
      List<Map<String, Object>> tests = fetchMmseTests();
      List<Map<String, Object>> data = tests.stream()
          .map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("ID", t.get("id"));
            map.put("Patient", t.get("patientName"));
            map.put("Total Score", t.get("totalScore"));
            map.put("Interpretation", t.get("interpretation"));
            map.put("Orientation", t.get("orientationScore"));
            map.put("Registration", t.get("registrationScore"));
            map.put("Attention", t.get("attentionScore"));
            map.put("Recall", t.get("recallScore"));
            map.put("Language", t.get("languageScore"));
            map.put("Test Date", t.get("testDate"));
            map.put("Notes", t.get("notes"));
            return map;
          })
          .collect(Collectors.toList());

      return ResponseEntity.ok(ApiResponse.success("MMSE tests exported successfully", data));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Export failed", e.getMessage()));
    }
  }

  @GetMapping("/activity-log")
  public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActivityLog(@RequestParam(defaultValue = "50") int limit) {
    try {
      int safeLimit = Math.max(1, limit);
      List<Map<String, Object>> activities = new ArrayList<>();

      List<Map<String, Object>> users = fetchUsers();
      users.stream()
          .sorted(Comparator.comparing(u -> String.valueOf(u.get("createdAt")), Comparator.reverseOrder()))
          .limit(Math.max(1, safeLimit / 2))
          .forEach(user -> {
            Map<String, Object> activity = new HashMap<>();
            activity.put("type", "USER_CREATED");
            activity.put("description", "User created: " + user.get("firstName") + " " + user.get("lastName"));
            activity.put("timestamp", user.get("createdAt"));
            activity.put("email", user.get("email"));
            activities.add(activity);
          });

      List<Map<String, Object>> tests = fetchMmseTests();
      tests.stream()
          .sorted(Comparator.comparing(t -> String.valueOf(t.get("testDate")), Comparator.reverseOrder()))
          .limit(Math.max(1, safeLimit / 2))
          .forEach(test -> {
            Map<String, Object> activity = new HashMap<>();
            activity.put("type", "TEST_CREATED");
            activity.put("description", "MMSE test for " + test.get("patientName") + " (Score: " + test.get("totalScore") + ")");
            activity.put("timestamp", test.get("testDate"));
            activity.put("score", test.get("totalScore"));
            activities.add(activity);
          });

      List<Map<String, Object>> sorted = activities.stream()
          .sorted(Comparator.comparing(a -> String.valueOf(a.get("timestamp")), Comparator.reverseOrder()))
          .limit(safeLimit)
          .collect(Collectors.toList());

      return ResponseEntity.ok(ApiResponse.success("Activity log retrieved successfully", sorted));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(ApiResponse.error("Failed to retrieve activity log", e.getMessage()));
    }
  }

  @PostMapping("/backup")
  public ResponseEntity<ApiResponse<Map<String, String>>> createBackup() {
    Map<String, String> backupInfo = Map.of(
        "status", "success",
        "message", "Backup created successfully (stub)",
        "timestamp", LocalDateTime.now().toString(),
        "type", "full",
        "size", "N/A"
    );
    return ResponseEntity.ok(ApiResponse.success("Backup created successfully", backupInfo));
  }

  private long getLongOrZeroFrom(RestClient client, String uri) {
    try {
      Long val = client.get().uri(uri).retrieve().body(Long.class);
      return val == null ? 0L : val;
    } catch (Exception ignored) {
      return 0L;
    }
  }

  private long getLongOrZeroFrom(RestClient client, String uri, String statsKey) {
    try {
      Map<?, ?> response = client.get().uri(uri).retrieve().body(Map.class);
      if (response == null) {
        return 0L;
      }
      Object data = response.get("data");
      if (!(data instanceof Map<?, ?>)) {
        return 0L;
      }
      Object v = ((Map<?, ?>) data).get(statsKey);
      if (v == null) {
        return 0L;
      }
      return Long.parseLong(v.toString());
    } catch (Exception ignored) {
      return 0L;
    }
  }

  private List<Map<String, Object>> fetchUsers() {
    try {
      List<?> users = userServiceClient.get().uri("/api/users").retrieve().body(List.class);
      if (users == null) {
        return List.of();
      }
      return users.stream()
          .filter(Map.class::isInstance)
          .map(m -> (Map<String, Object>) m)
          .collect(Collectors.toList());
    } catch (Exception ignored) {
      return List.of();
    }
  }

  private List<Map<String, Object>> fetchMmseTests() {
    try {
      Map<?, ?> response = mmseServiceClient.get().uri("/api/mmse/results").retrieve().body(Map.class);
      if (response == null) {
        return List.of();
      }
      Object data = response.get("data");
      if (!(data instanceof List<?>)) {
        return List.of();
      }
      List<?> list = (List<?>) data;
      return list.stream()
          .filter(Map.class::isInstance)
          .map(m -> (Map<String, Object>) m)
          .collect(Collectors.toList());
    } catch (Exception ignored) {
      return List.of();
    }
  }

  private List<Map<String, Object>> fetchMedicalRecords() {
    try {
      Map<?, ?> response = medicalServiceClient.get().uri("/api/medical-records").retrieve().body(Map.class);
      if (response == null) {
        return List.of();
      }
      Object data = response.get("data");
      if (!(data instanceof List<?>)) {
        return List.of();
      }

      List<Map<String, Object>> users = fetchUsers();
      Map<String, Map<String, Object>> usersById = users.stream()
          .filter(u -> u.get("id") != null)
          .collect(Collectors.toMap(u -> String.valueOf(u.get("id")), u -> u, (a, b) -> a));

      List<?> list = (List<?>) data;
      return list.stream()
          .filter(Map.class::isInstance)
          .map(m -> (Map<String, Object>) m)
          .map(r -> {
            Map<String, Object> out = new LinkedHashMap<>(r);
            Object userId = r.get("userId");
            if (userId != null) {
              Map<String, Object> u = usersById.get(String.valueOf(userId));
              if (u != null) {
                String name = (String.valueOf(Optional.ofNullable(u.get("firstName")).orElse("")).trim() + " " +
                    String.valueOf(Optional.ofNullable(u.get("lastName")).orElse("")).trim()).trim();
                out.put("userName", name.isBlank() ? null : name);
                out.put("userEmail", u.get("email"));
              }
            }
            return out;
          })
          .collect(Collectors.toList());
    } catch (Exception ignored) {
      return List.of();
    }
  }

  private boolean containsIgnoreCase(Map<String, Object> map, String key, String needle) {
    Object v = map.get(key);
    return v != null && v.toString().toLowerCase().contains(needle);
  }
}

