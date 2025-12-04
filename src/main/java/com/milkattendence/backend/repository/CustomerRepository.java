package com.milkattendence.backend.repository;

import com.milkattendence.backend.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Fetch all customers belonging to a particular shift.
     */
    List<Customer> findByShift(String shift);

    /**
     * Fetch a customer by full name OR nickname.
     */
    Optional<Customer> findByFullNameOrNickname(String fullName, String nickname);

    /**
     * NEW: Fetch customers belonging to a specific user.
     * This ensures each user sees ONLY their own customers.
     */
    List<Customer> findByUserId(Long userId);
}
