package com.milkattendence.backend.controller;

import com.milkattendence.backend.model.Customer;
import com.milkattendence.backend.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

    // ==========================================================
    // ✅ GET Customers: Only retrieves ACTIVE customers
    // The required userId identifies the owner. Shift is an optional filter.
    // ==========================================================
    @GetMapping
    public List<Customer> getCustomers(
            @RequestParam Long userId,
            @RequestParam(required = false) String shift
    ) {
        // If no shift is provided or if it's blank, fetch all active customers for the user.
        if (shift == null || shift.isBlank()) {
            // Calls the new method in CustomerRepository: findByUserIdAndActive(Long userId, boolean active)
            return customerRepository.findByUserIdAndActive(userId, true);
        }
        
        // If a shift is provided, fetch active customers filtered by shift.
        // Calls the new method in CustomerRepository: findByShiftAndUserIdAndActive(String shift, Long userId, boolean active)
        return customerRepository.findByShiftAndUserIdAndActive(shift, userId, true);
    }

    // ==========================================================
    // ✅ ADD Customer: Sets 'active' to true by default.
    // ==========================================================
    @PostMapping
    public ResponseEntity<Customer> addCustomer(@RequestBody Customer c) {

        if (c.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is required for a new customer.");
        }
        if (c.getFullName() == null || c.getFullName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name is required.");
        }

        // Set active to true on creation (Crucial to make it appear in lists)
        c.setActive(true); 
        Customer savedCustomer = customerRepository.save(c);
        
        // Return 201 Created status
        return new ResponseEntity<>(savedCustomer, HttpStatus.CREATED);
    }

    // ==========================================================
    // ✅ UPDATE Customer
    // Includes updating the active status if passed in the request body.
    // ==========================================================
    @PutMapping("/{id}")
    public Customer updateCustomer(@PathVariable Long id, @RequestBody Customer updated) {

        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Customer not found with ID: " + id));

        c.setFullName(updated.getFullName());
        c.setNickname(updated.getNickname());
        c.setShift(updated.getShift());
        c.setPricePerLitre(updated.getPricePerLitre());
        // Allow updating active status
        c.setActive(updated.isActive()); 

        return customerRepository.save(c);
    }

    // ==========================================================
    // ✅ DELETE Customer (Soft Delete)
    // Sets 'active' to false instead of physical deletion.
    // ==========================================================
    @DeleteMapping("/{id}")
    public void deleteCustomer(@PathVariable Long id) {
        
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Customer not found with ID: " + id));

        // Perform a soft delete: set active to false
        c.setActive(false);
        customerRepository.save(c);
        
        // NOTE: If you need to physically delete, uncomment the line below and remove the two above:
        // customerRepository.deleteById(id);
    }
}