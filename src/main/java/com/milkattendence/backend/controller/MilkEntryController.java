package com.milkattendence.backend.controller;

import com.milkattendence.backend.model.MilkEntry;
import com.milkattendence.backend.repository.MilkEntryRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/milk")
@CrossOrigin(origins = "*")
public class MilkEntryController {

    private final MilkEntryRepository repo;

    public MilkEntryController(MilkEntryRepository repo) {
        this.repo = repo;
    }

    /**
     * Get entries for a specific user and shift (range = latest month by frontend or specify range later).
     * Example: GET /api/milk?userId=5&shift=Morning&start=2025-12-01&end=2025-12-31
     */
    @GetMapping
    public List<MilkEntry> getEntriesByUserAndShift(
            @RequestParam Long userId,
            @RequestParam String shift,
            @RequestParam(required = false) LocalDate start,
            @RequestParam(required = false) LocalDate end
    ) {
        // If no range provided, return all entries for that user/shift
        if (start == null || end == null) {
            // Not ideal for large datasets, but kept simple — frontend should pass range when needed
            return repo.findByUserIdAndShiftAndDateBetweenOrderByDateAsc(userId, shift, LocalDate.MIN, LocalDate.MAX);
        }
        return repo.findByUserIdAndShiftAndDateBetweenOrderByDateAsc(userId, shift, start, end);
    }

    /**
     * Add or update entry for a user. Body must include userId.
     */
    @PostMapping
    public MilkEntry addOrUpdateEntry(@RequestBody MilkEntry entry) {
        if (entry.getUserId() == null) {
            throw new RuntimeException("userId is required");
        }

        if (entry.getDate() == null) {
            entry.setDate(LocalDate.now());
        }

        Optional<MilkEntry> existing = repo.findByUserIdAndShiftAndDateAndCustomerName(
                entry.getUserId(),
                entry.getShift(),
                entry.getDate(),
                entry.getCustomerName()
        );

        // Reset: delete if litres == 0
        if (entry.getLitres() == 0) {
            existing.ifPresent(repo::delete);
            return entry;
        }

        entry.setAmount(entry.getLitres() * entry.getRate());

        MilkEntry saveEntry = existing.orElse(new MilkEntry());
        saveEntry.setUserId(entry.getUserId());
        saveEntry.setCustomerName(entry.getCustomerName());
        saveEntry.setShift(entry.getShift());
        saveEntry.setDate(entry.getDate());
        saveEntry.setLitres(entry.getLitres());
        saveEntry.setRate(entry.getRate());
        saveEntry.setAmount(entry.getAmount());

        return repo.save(saveEntry);
    }

    @DeleteMapping("/{id}")
    public String deleteEntry(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            return "Entry not found";
        }
        repo.deleteById(id);
        return "Deleted";
    }
}
