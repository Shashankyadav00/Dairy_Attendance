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

    // ✅ Get all entries for a shift
    @GetMapping("/{shift}")
    public List<MilkEntry> getEntriesByShift(@PathVariable String shift) {
        return repo.findByShiftOrderByDateDesc(shift);
    }

    // ✅ Add or update entry (with reset support)
    @PostMapping
    public MilkEntry addOrUpdateEntry(@RequestBody MilkEntry entry) {
        try {
            // Set default date
            if (entry.getDate() == null) {
                entry.setDate(LocalDate.now());
            }

            // Look for existing entry for same customer/date/shift
            Optional<MilkEntry> existing = repo.findByShiftAndDateAndCustomerName(
                    entry.getShift(),
                    entry.getDate(),
                    entry.getCustomerName()
            );

            // ✅ Reset: if litres = 0, delete existing entry if present
            if (entry.getLitres() == 0) {
                existing.ifPresent(repo::delete);
                System.out.println("🗑️ Reset entry for " + entry.getCustomerName() + " on " + entry.getDate());
                return entry; // Return dummy success
            }

            // ✅ Calculate amount
            entry.setAmount(entry.getLitres() * entry.getRate());

            MilkEntry saveEntry = existing.orElse(new MilkEntry());
            saveEntry.setCustomerName(entry.getCustomerName());
            saveEntry.setShift(entry.getShift());
            saveEntry.setDate(entry.getDate());
            saveEntry.setLitres(entry.getLitres());
            saveEntry.setRate(entry.getRate());
            saveEntry.setAmount(entry.getAmount());

            MilkEntry saved = repo.save(saveEntry);
            System.out.println("✅ Saved entry for " + saved.getCustomerName() + " (" + saved.getLitres() + " L)");

            return saved;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to save entry: " + e.getMessage());
        }
    }

    // ✅ Delete entry by ID (used in DayWiseEntry delete button)
    @DeleteMapping("/{id}")
    public String deleteEntry(@PathVariable Long id) {
        if (!repo.existsById(id)) {
            return "❌ Entry not found!";
        }
        repo.deleteById(id);
        System.out.println("🗑️ Deleted entry with ID: " + id);
        return "✅ Entry deleted successfully!";
    }
}
