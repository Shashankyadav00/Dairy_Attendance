package com.milkattendence.backend.repository;

import com.milkattendence.backend.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * ✅ Fetch all customers belonging to a particular shift.
     * Example: Morning / Night
     */
    List<Customer> findByShift(String shift);

    /**
     * ✅ Fetch a customer by full name OR nickname (case-insensitive).
     * Used in overview & PDF report to find rate per litre.
     *
     * Spring Data JPA auto-generates:
     * SELECT * FROM customers WHERE LOWER(full_name) = LOWER(?) OR LOWER(nickname) = LOWER(?) LIMIT 1;
     */
    Optional<Customer> findByFullNameOrNickname(String fullName, String nickname);
}
