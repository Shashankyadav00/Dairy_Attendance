package com.milkattendence.backend.repository;

import com.milkattendence.backend.model.MilkEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MilkEntryRepository extends JpaRepository<MilkEntry, Long> {
    List<MilkEntry> findByShiftOrderByDateDesc(String shift);

    List<MilkEntry> findByShiftAndDateBetweenOrderByDateAsc(String shift, LocalDate start, LocalDate end);

    // ✅ This method must be present exactly like this
    Optional<MilkEntry> findByShiftAndDateAndCustomerName(String shift, LocalDate date, String customerName);
}
