package com.milkattendence.backend.controller;

import com.milkattendence.backend.model.Customer;
import com.milkattendence.backend.repository.CustomerRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "*")
public class CustomerController {

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    // ============================================================
    // ✅ Fetch customers ONLY for the logged-in user
    // Example request: /api/customers?userId=3
    // ============================================================
    @GetMapping
    public List<Customer> getAllCustomers(@RequestParam Long userId) {
        return customerRepository.findByUserId(userId);
    }

    // ============================================================
    // ✅ Fetch customers by shift for the logged-in user
    // Example: /api/customers/morning?userId=3
    // ============================================================
    @GetMapping("/{shift}")
    public List<Customer> getCustomersByShift(
            @PathVariable String shift,
            @RequestParam Long userId
    ) {
        return customerRepository.findByShiftAndUserId(shift, userId);
    }

    // ============================================================
    // ✅ Add customer (userId MUST be included in request body)
    // ============================================================
    @PostMapping
    public Customer addCustomer(@RequestBody Customer customer) {
        if (customer.getFullName() == null || customer.getFullName().isBlank()) {
            throw new RuntimeException("Full name is required");
        }
        if (customer.getUserId() == null) {
            throw new RuntimeException("User ID is required");
        }

        return customerRepository.save(customer);
    }

    // ============================================================
    // ✅ Update an existing customer (userId should NOT change)
    // ============================================================
    @PutMapping("/{id}")
    public Customer updateCustomer(@PathVariable Long id, @RequestBody Customer updatedCustomer) {
        Optional<Customer> existing = customerRepository.findById(id);

        if (existing.isPresent()) {
            Customer c = existing.get();
            c.setFullName(updatedCustomer.getFullName());
            c.setNickname(updatedCustomer.getNickname());
            c.setPricePerLitre(updatedCustomer.getPricePerLitre());
            c.setShift(updatedCustomer.getShift());

            return customerRepository.save(c);
        } else {
            throw new RuntimeException("Customer not found with ID: " + id);
        }
    }

    // ============================================================
    // ✅ Delete a customer
    // ============================================================
    @DeleteMapping("/{id}")
    public void deleteCustomer(@PathVariable Long id) {
        if (!customerRepository.existsById(id)) {
            throw new RuntimeException("Customer not found with ID: " + id);
        }
        customerRepository.deleteById(id);
    }
}
