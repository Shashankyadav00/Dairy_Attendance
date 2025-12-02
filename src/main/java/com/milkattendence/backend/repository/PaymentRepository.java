package com.milkattendence.backend.repository;

import com.milkattendence.backend.model.Payment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // ✅ Fetch all payments for a specific shift and date
    List<Payment> findByShiftAndDate(String shift, LocalDate date);

    // ✅ Fetch all unpaid payments for a given date
    List<Payment> findByPaidFalseAndDate(LocalDate date);

    // ✅ Fetch unpaid payments for a given shift and date
    List<Payment> findByShiftAndPaidFalseAndDate(String shift, LocalDate date);

    // ✅ Find record by customer, shift, and date
    @Query("""
        SELECT p FROM Payment p
        WHERE LOWER(p.customerName) = LOWER(:customerName)
          AND p.shift = :shift
          AND p.date = :date
    """)
    List<Payment> findAllMatching(@Param("shift") String shift,
                                  @Param("date") LocalDate date,
                                  @Param("customerName") String customerName);

    // ✅ Bulk mark as paid/unpaid
    @Transactional
    @Modifying
    @Query("""
        UPDATE Payment p
           SET p.paid = :paid
         WHERE p.date = :date
           AND p.shift = :shift
    """)
    int updateAllByShiftAndDate(@Param("date") LocalDate date,
                                @Param("shift") String shift,
                                @Param("paid") boolean paid);

    // ✅ Fetch all between two dates
    @Query("""
        SELECT p FROM Payment p
         WHERE p.date BETWEEN :start AND :end
         ORDER BY p.date ASC
    """)
    List<Payment> findByDateBetween(@Param("start") LocalDate start,
                                    @Param("end") LocalDate end);

    // ✅ Fetch shift data between two dates
    @Query("""
        SELECT p FROM Payment p
         WHERE p.shift = :shift
           AND p.date BETWEEN :start AND :end
         ORDER BY p.date ASC
    """)
    List<Payment> findByShiftAndDateRange(@Param("shift") String shift,
                                          @Param("start") LocalDate start,
                                          @Param("end") LocalDate end);
}
