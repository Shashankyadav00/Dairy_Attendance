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
     * Fetch customers belonging to a specific user.
     */
    List<Customer> findByUserId(Long userId);

    /**
     * Fetch customers by shift AND user.
     * Required to prevent other users from seeing each other's data.
     */
    List<Customer> findByShiftAndUserId(String shift, Long userId);
}
