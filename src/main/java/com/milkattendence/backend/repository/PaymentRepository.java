package com.milkattendence.backend.repository;

import com.milkattendence.backend.model.Payment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // -------------------------------------------------------------
    // USER-SPECIFIC PAYMENT FETCHING
    // -------------------------------------------------------------

    // Load payments for a shift, date, and specific user
    List<Payment> findByShiftAndDateAndUserId(String shift, LocalDate date, Long userId);

    // Find specific payment for a user
    @Query("""
        SELECT p FROM Payment p
        WHERE LOWER(p.customerName) = LOWER(:customerName)
          AND p.shift = :shift
          AND p.date = :date
          AND p.userId = :userId
    """)
    List<Payment> findAllMatchingForUser(
            @Param("shift") String shift,
            @Param("date") LocalDate date,
            @Param("customerName") String customerName,
            @Param("userId") Long userId
    );

    // -------------------------------------------------------------
    // EMAIL REMINDER (APPLIES TO ALL USERS)
    // -------------------------------------------------------------
    List<Payment> findByShiftAndPaidFalseAndDate(String shift, LocalDate date);

}
