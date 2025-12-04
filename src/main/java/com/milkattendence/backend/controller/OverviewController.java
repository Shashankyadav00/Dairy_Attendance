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

    // GET overview for specific user
    @GetMapping
    public Map<String, Object> getOverview(
            @RequestParam String shift,
            @RequestParam int month,
            @RequestParam int year,
            @RequestParam Long userId
    ) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<Customer> customers = customerRepo.findByShiftAndUserId(shift, userId);

        List<MilkEntry> entries = milkEntryRepo.findByUserIdAndShiftAndDateBetweenOrderByDateAsc(
                userId, shift, start, end
        );

        int daysInMonth = ym.lengthOfMonth();
        Map<Integer, Map<Long, Map<String, Double>>> matrix = new HashMap<>();
        for (int d = 1; d <= daysInMonth; d++) matrix.put(d, new HashMap<>());

        for (MilkEntry e : entries) {
            int day = e.getDate().getDayOfMonth();

            Optional<Customer> match = customers.stream()
                    .filter(c ->
                            (c.getFullName() != null && c.getFullName().equalsIgnoreCase(e.getCustomerName())) ||
                            (c.getNickname() != null && c.getNickname().equalsIgnoreCase(e.getCustomerName()))
                    )
                    .findFirst();

            if (match.isEmpty()) continue;

            Long custId = match.get().getId();
            Map<Long, Map<String, Double>> dayMap = matrix.get(day);
            Map<String, Double> cell = dayMap.getOrDefault(custId, new HashMap<>());

            cell.put("litres", cell.getOrDefault("litres", 0.0) + e.getLitres());
            cell.put("amount", cell.getOrDefault("amount", 0.0) + e.getAmount());
            dayMap.put(custId, cell);
        }

        List<Map<String, Object>> customerList = new ArrayList<>();
        for (Customer c : customers) {
            Map<String, Object> cm = new HashMap<>();
            cm.put("id", c.getId());
            cm.put("name", c.getFullName() != null ? c.getFullName() : c.getNickname());
            cm.put("pricePerLitre", c.getPricePerLitre());
            customerList.add(cm);
        }

        Map<Long, Double> totalLitresPerCustomer = new HashMap<>();
        Map<Long, Double> totalAmountPerCustomer = new HashMap<>();
        Map<Integer, Double> totalPerDay = new HashMap<>();
        double grandTotalAmount = 0;

        for (int d = 1; d <= daysInMonth; d++) {
            double dayTotal = 0;
            for (Map.Entry<Long, Map<String, Double>> ent : matrix.get(d).entrySet()) {
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

        Map<String, Object> response = new HashMap<>();
        response.put("year", year);
        response.put("month", month);
        response.put("daysInMonth", daysInMonth);
        response.put("customers", customerList);
        response.put("matrix", matrix);
        response.put("totalLitresPerCustomer", totalLitresPerCustomer);
        response.put("totalAmountPerCustomer", totalAmountPerCustomer);
        response.put("totalPerDay", totalPerDay);
        response.put("grandTotalAmount", grandTotalAmount);

        return response;
    }

    // Add/update/reset entry for specific user
    @PostMapping("/add")
    public Map<String, Object> addOrUpdateEntry(@RequestBody MilkEntry entry) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (entry.getUserId() == null) {
                response.put("status", "error");
                response.put("message", "userId is required");
                return response;
            }
            if (entry.getCustomerName() == null || entry.getCustomerName().isEmpty()) {
                response.put("status", "error");
                response.put("message", "Customer name required");
                return response;
            }
            if (entry.getDate() == null) {
                response.put("status", "error");
                response.put("message", "Date required");
                return response;
            }

            Optional<Customer> custOpt = customerRepo.findByShiftAndUserId(entry.getShift(), entry.getUserId())
                    .stream()
                    .filter(c ->
                            c.getFullName().equalsIgnoreCase(entry.getCustomerName()) ||
                                    (c.getNickname() != null && c.getNickname().equalsIgnoreCase(entry.getCustomerName()))
                    )
                    .findFirst();

            if (custOpt.isEmpty()) {
                response.put("status", "error");
                response.put("message", "Customer not found for this user");
                return response;
            }

            double rate = custOpt.get().getPricePerLitre();
            double litres = entry.getLitres();

            Optional<MilkEntry> existing = milkEntryRepo.findByUserIdAndShiftAndDateAndCustomerName(
                    entry.getUserId(),
                    entry.getShift(),
                    entry.getDate(),
                    entry.getCustomerName()
            );

            if (litres == 0) {
                existing.ifPresent(milkEntryRepo::delete);
                response.put("status", "reset");
                response.put("message", "Entry reset successfully");
                return response;
            }

            MilkEntry saveEntry = existing.orElse(new MilkEntry());
            saveEntry.setUserId(entry.getUserId());
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
