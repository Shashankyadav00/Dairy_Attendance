package com.milkattendence.backend.repository;

import com.milkattendence.backend.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // NOTE: This assumes your Customer model has a field: 'private boolean active;'

    // ==========================================================
    // 🟢 FIX 1: Fetch all ACTIVE customers belonging to a specific user.
    // This supports the main customer selection list endpoint (/api/customers?userId=...)
    // ==========================================================
    List<Customer> findByUserIdAndActive(Long userId, boolean active);

    // ==========================================================
    // 🟢 FIX 2: Fetch ACTIVE customers by shift AND user.
    // This supports filtered views and the Overview Controller.
    // ==========================================================
    List<Customer> findByShiftAndUserIdAndActive(String shift, Long userId, boolean active);


    // --- Original methods preserved (for clarity or legacy) ---

    /**
     * Fetch all customers belonging to a specific user (legacy, but useful if active status is not always needed).
     */
    List<Customer> findByUserId(Long userId);

    /**
     * Fetch customers by shift AND user (legacy version without active filter).
     */
    List<Customer> findByShiftAndUserId(String shift, Long userId);

    /**
     * Fetch all customers belonging to a particular shift (legacy).
     */
    List<Customer> findByShift(String shift);

    /**
     * Fetch a customer by full name OR nickname.
     */
    Optional<Customer> findByFullNameOrNickname(String fullName, String nickname);
}