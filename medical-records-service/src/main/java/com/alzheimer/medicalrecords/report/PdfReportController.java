package com.alzheimer.medicalrecords.report;

import com.alzheimer.medicalrecords.entity.*;
import com.alzheimer.medicalrecords.repository.*;
import com.alzheimer.medicalrecords.service.*;
import com.alzheimer.medicalrecords.user.*;
import com.alzheimer.medicalrecords.dto.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/reports")
public class PdfReportController {

    private final PdfReportService pdfReportService;

    public PdfReportController(PdfReportService pdfReportService) {
        this.pdfReportService = pdfReportService;
    }

    /**
     * Generate PDF by MEDICAL RECORD ID (existing endpoint).
     */
    @GetMapping("/medical-record/{medicalRecordId}/pdf")
    public ResponseEntity<byte[]> downloadByRecordId(@PathVariable Long medicalRecordId) {
        return generatePdf(() -> pdfReportService.generateReport(medicalRecordId),
                           "Record" + medicalRecordId);
    }

    /**
     * NEW: Generate PDF by USER ID — automatically finds the latest medical record.
     * This is what the family tree page should call (userId=1 → finds record automatically).
     * Endpoint: GET /api/reports/user/{userId}/pdf
     */
    @GetMapping("/user/{userId}/pdf")
    public ResponseEntity<byte[]> downloadByUserId(@PathVariable Long userId) {
        return generatePdf(() -> pdfReportService.generateReportByUserId(userId),
                           "User" + userId);
    }

    // ── shared helper ─────────────────────────────────────────────────────────

    @FunctionalInterface
    interface PdfSupplier { byte[] get() throws Exception; }

    private ResponseEntity<byte[]> generatePdf(PdfSupplier supplier, String label) {
        try {
            byte[] pdf = supplier.get();
            String filename = "Medical_Report_" + label + "_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")) + ".pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(pdf.length);
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            System.err.println("[PdfReportController] Not found: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            System.err.println("[PdfReportController] Error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
