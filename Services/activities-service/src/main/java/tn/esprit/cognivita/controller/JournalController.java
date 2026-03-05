package tn.esprit.cognivita.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.cognivita.entity.JournalEntry;
import tn.esprit.cognivita.repository.JournalRepository;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/journal")  // ✅ PLUS DE /API ICI
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class JournalController {
    private final JournalRepository journalRepository;
    private static final Logger log = LoggerFactory.getLogger(JournalController.class);

    // GET all entries
    @GetMapping
    public ResponseEntity<List<JournalEntry>> getAllEntries() {
        return ResponseEntity.ok(journalRepository.findAllByOrderByDateDesc());
    }

    // GET entry by id
    @GetMapping("/{id}")
    public ResponseEntity<JournalEntry> getEntryById(@PathVariable Long id) {
        return journalRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // POST create entry
    @PostMapping
    public ResponseEntity<JournalEntry> createEntry(@RequestBody JournalEntry entry) {
        JournalEntry saved = journalRepository.save(entry);
        log.info("Created JournalEntry id={}", saved.getId());
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    // PUT update entry
    @PutMapping("/{id}")
    public ResponseEntity<JournalEntry> updateEntry(@PathVariable Long id, @RequestBody JournalEntry entry) {
        return journalRepository.findById(id)
                .map(existing -> {
                    existing.setDate(entry.getDate());
                    existing.setMood(entry.getMood());
                    existing.setEnergy(entry.getEnergy());
                    existing.setStress(entry.getStress());
                    existing.setSleepHours(entry.getSleepHours());
                    existing.setActivities(entry.getActivities());
                    existing.setNotes(entry.getNotes());
                    JournalEntry updated = journalRepository.save(existing);
                    log.info("Updated JournalEntry id={}", updated.getId());
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE entry
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id) {
        if (!journalRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        journalRepository.deleteById(id);
        log.info("Deleted JournalEntry id={}", id);
        return ResponseEntity.noContent().build();
    }

    // GET stats with optional date filters
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        LocalDate fromDate = (from == null || from.isBlank()) ? null : LocalDate.parse(from);
        LocalDate toDate = (to == null || to.isBlank()) ? null : LocalDate.parse(to);

        Map<String, Object> stats = new HashMap<>();
        stats.put("averageMood", journalRepository.averageMood(fromDate, toDate) != null ? journalRepository.averageMood(fromDate, toDate) : 0.0);
        stats.put("averageEnergy", journalRepository.averageEnergy(fromDate, toDate) != null ? journalRepository.averageEnergy(fromDate, toDate) : 0.0);
        stats.put("averageStress", journalRepository.averageStress(fromDate, toDate) != null ? journalRepository.averageStress(fromDate, toDate) : 0.0);
        stats.put("averageSleep", journalRepository.averageSleep(fromDate, toDate) != null ? journalRepository.averageSleep(fromDate, toDate) : 0.0);
        stats.put("entriesCount", journalRepository.count());

        return ResponseEntity.ok(stats);
    }
}