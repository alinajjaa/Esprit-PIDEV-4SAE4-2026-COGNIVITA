package com.alzheimer.backend.mmse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/mmse")
public class MMSEController {

  @Autowired private MMSERepository mmseRepository;

  @PostMapping("/submit")
  public ResponseEntity<?> submitMMSETest(@RequestBody MMSETestRequest request) {
    try {
      MMSETest test = new MMSETest();
      test.setPatientName(request.getPatient_name());
      test.setOrientationScore(request.getOrientation_score());
      test.setRegistrationScore(request.getRegistration_score());
      test.setAttentionScore(request.getAttention_score());
      test.setRecallScore(request.getRecall_score());
      test.setLanguageScore(request.getLanguage_score());
      test.setTestDate(request.getTest_date() != null ? LocalDate.parse(request.getTest_date()) : LocalDate.now());
      test.setNotes(request.getNotes());

      MMSETest saved = mmseRepository.save(test);
      return ResponseEntity.ok(new ApiResponse(true, "MMSE test submitted successfully", saved));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiResponse(false, "Error submitting test: " + e.getMessage(), null));
    }
  }

  @GetMapping("/results")
  public ResponseEntity<?> getMMSEResults() {
    try {
      List<MMSETest> tests = mmseRepository.findAll();
      return ResponseEntity.ok(new ApiResponse(true, "MMSE results retrieved", tests));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiResponse(false, "Error: " + e.getMessage(), null));
    }
  }

  @GetMapping("/results/{patientName}")
  public ResponseEntity<?> getPatientResults(@PathVariable String patientName) {
    try {
      List<MMSETest> tests = mmseRepository.findByPatientNameIgnoreCase(patientName);
      if (tests.isEmpty())
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new ApiResponse(false, "No results found for: " + patientName, null));
      return ResponseEntity.ok(new ApiResponse(true, "Patient results retrieved", tests));
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ApiResponse(false, "Error: " + e.getMessage(), null));
    }
  }

  public static class MMSETestRequest {
    private String patient_name;
    private int orientation_score, registration_score, attention_score, recall_score, language_score, total_score;
    private String interpretation, test_date, notes;

    public String getPatient_name() { return patient_name; }
    public int getOrientation_score() { return orientation_score; }
    public int getRegistration_score() { return registration_score; }
    public int getAttention_score() { return attention_score; }
    public int getRecall_score() { return recall_score; }
    public int getLanguage_score() { return language_score; }
    public int getTotal_score() { return total_score; }
    public String getInterpretation() { return interpretation; }
    public String getTest_date() { return test_date; }
    public String getNotes() { return notes; }
    public void setPatient_name(String v) { patient_name = v; }
    public void setOrientation_score(int v) { orientation_score = v; }
    public void setRegistration_score(int v) { registration_score = v; }
    public void setAttention_score(int v) { attention_score = v; }
    public void setRecall_score(int v) { recall_score = v; }
    public void setLanguage_score(int v) { language_score = v; }
    public void setTotal_score(int v) { total_score = v; }
    public void setInterpretation(String v) { interpretation = v; }
    public void setTest_date(String v) { test_date = v; }
    public void setNotes(String v) { notes = v; }
  }

  public static class ApiResponse {
    private final boolean success; private final String message; private final Object data;
    public ApiResponse(boolean s, String m, Object d) { success=s; message=m; data=d; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
  }
}
