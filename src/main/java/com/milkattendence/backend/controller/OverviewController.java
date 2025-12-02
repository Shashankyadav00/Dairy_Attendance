package com.milkattendence.backend.controller;

import com.milkattendence.backend.model.Customer;
import com.milkattendence.backend.model.MilkEntry;
import com.milkattendence.backend.repository.CustomerRepository;
import com.milkattendence.backend.repository.MilkEntryRepository;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@RestController
@RequestMapping("/api/overview")
@CrossOrigin(origins = "*")
public class OverviewController {

    private final MilkEntryRepository milkEntryRepo;
    private final CustomerRepository customerRepo;

    public OverviewController(MilkEntryRepository milkEntryRepo, CustomerRepository customerRepo) {
        this.milkEntryRepo = milkEntryRepo;
        this.customerRepo = customerRepo;
    }

    /**
     * ✅ GET: Monthly overview data (unchanged from your original)
     */
    @GetMapping
    public Map<String, Object> getOverview(
            @RequestParam String shift,
            @RequestParam int month,
            @RequestParam int year
    ) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Customer> customers = customerRepo.findByShift(shift);
        List<MilkEntry> entries = milkEntryRepo.findByShiftAndDateBetweenOrderByDateAsc(shift, start, end);

        int daysInMonth = ym.lengthOfMonth();
        Map<Integer, Map<Long, Map<String, Double>>> matrix = new HashMap<>();
        for (int d = 1; d <= daysInMonth; d++) matrix.put(d, new HashMap<>());

        // Fill the matrix
        for (MilkEntry e : entries) {
            int day = e.getDate().getDayOfMonth();
            Optional<Customer> maybe = customers.stream()
                    .filter(c ->
                            (c.getFullName() != null && c.getFullName().equalsIgnoreCase(e.getCustomerName())) ||
                            (c.getNickname() != null && c.getNickname().equalsIgnoreCase(e.getCustomerName()))
                    )
                    .findFirst();
            if (!maybe.isPresent()) continue;

            Long custId = maybe.get().getId();
            Map<Long, Map<String, Double>> dayMap = matrix.get(day);
            Map<String, Double> cell = dayMap.getOrDefault(custId, new HashMap<>());
            double litresPrev = cell.getOrDefault("litres", 0.0);
            double amountPrev = cell.getOrDefault("amount", 0.0);
            cell.put("litres", litresPrev + e.getLitres());
            cell.put("amount", amountPrev + e.getAmount());
            dayMap.put(custId, cell);
        }

        // Build customer list
        List<Map<String, Object>> customerCols = new ArrayList<>();
        for (Customer c : customers) {
            Map<String, Object> cm = new HashMap<>();
            cm.put("id", c.getId());
            cm.put("name", c.getFullName() != null ? c.getFullName() : c.getNickname());
            cm.put("pricePerLitre", c.getPricePerLitre());
            customerCols.add(cm);
        }

        // Totals
        Map<Long, Double> totalLitresPerCustomer = new HashMap<>();
        Map<Long, Double> totalAmountPerCustomer = new HashMap<>();
        double grandTotalAmount = 0;
        Map<Integer, Double> totalPerDay = new HashMap<>();

        for (int d = 1; d <= daysInMonth; d++) {
            double dayTotal = 0;
            Map<Long, Map<String, Double>> dayMap = matrix.get(d);
            for (Map.Entry<Long, Map<String, Double>> ent : dayMap.entrySet()) {
                Long cid = ent.getKey();
                double litres = ent.getValue().getOrDefault("litres", 0.0);
                double amount = ent.getValue().getOrDefault("amount", 0.0);
                totalLitresPerCustomer.put(cid, totalLitresPerCustomer.getOrDefault(cid, 0.0) + litres);
                totalAmountPerCustomer.put(cid, totalAmountPerCustomer.getOrDefault(cid, 0.0) + amount);
                dayTotal += amount;
                grandTotalAmount += amount;
            }
            totalPerDay.put(d, dayTotal);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("month", month);
        result.put("daysInMonth", daysInMonth);
        result.put("customers", customerCols);
        result.put("matrix", matrix);
        result.put("totalLitresPerCustomer", totalLitresPerCustomer);
        result.put("totalAmountPerCustomer", totalAmountPerCustomer);
        result.put("totalPerDay", totalPerDay);
        result.put("grandTotalAmount", grandTotalAmount);

        return result;
    }

    /**
     * ✅ POST: Add / Update / Reset entry from Overview table
     */
    @PostMapping("/add")
    public Map<String, Object> addOrUpdateEntry(@RequestBody MilkEntry entry) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (entry.getCustomerName() == null || entry.getCustomerName().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Customer name required");
                return response;
            }

            // Ensure date and shift are valid
            if (entry.getDate() == null) {
                response.put("status", "error");
                response.put("message", "Date required");
                return response;
            }

            // Find the matching customer for rate lookup
            Optional<Customer> custOpt = customerRepo.findByShift(entry.getShift()).stream()
                    .filter(c -> c.getFullName().equalsIgnoreCase(entry.getCustomerName()) ||
                            (c.getNickname() != null && c.getNickname().equalsIgnoreCase(entry.getCustomerName())))
                    .findFirst();

            if (!custOpt.isPresent()) {
                response.put("status", "error");
                response.put("message", "Customer not found for this shift");
                return response;
            }

            Customer cust = custOpt.get();
            double rate = cust.getPricePerLitre();
            double litres = entry.getLitres();

            // Check if existing entry exists
            Optional<MilkEntry> existing = milkEntryRepo.findByShiftAndDateAndCustomerName(
                    entry.getShift(),
                    entry.getDate(),
                    entry.getCustomerName()
            );

            // ✅ Reset logic — if litres == 0 → delete existing
            if (litres == 0) {
                existing.ifPresent(milkEntryRepo::delete);
                response.put("status", "reset");
                response.put("message", "Entry reset successfully");
                return response;
            }

            // ✅ Create or update entry
            MilkEntry saveEntry = existing.orElse(new MilkEntry());
            saveEntry.setCustomerName(entry.getCustomerName());
            saveEntry.setShift(entry.getShift());
            saveEntry.setDate(entry.getDate());
            saveEntry.setLitres(litres);
            saveEntry.setRate(rate);
            saveEntry.setAmount(litres * rate);

            milkEntryRepo.save(saveEntry);

            response.put("status", "success");
            response.put("message", "Entry added/updated successfully");
            response.put("amount", litres * rate);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
        }

        return response;
    }
}
