package com.milkattendence.backend.repository;

import com.milkattendence.backend.model.MilkEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MilkEntryRepository extends JpaRepository<MilkEntry, Long> {

    // Fetch entries for a specific user + shift ordered by latest first (used in DayWise page)
    List<MilkEntry> findByUserIdAndShiftOrderByDateDesc(Long userId, String shift);

    // Fetch entries for a specific user + shift + month range (used in Overview)
    List<MilkEntry> findByUserIdAndShiftAndDateBetweenOrderByDateAsc(
            Long userId,
            String shift,
            LocalDate start,
            LocalDate end
    );

    // Find a single entry for a user by shift + date + customerName (used for quick update / reset)
    Optional<MilkEntry> findByUserIdAndShiftAndDateAndCustomerName(
            Long userId,
            String shift,
            LocalDate date,
            String customerName
    );
}
