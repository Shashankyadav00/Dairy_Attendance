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

    /**
     * ✅ Fetch all customers (used in Admin, Overview, PDF download, etc.)
     * Returns a plain array so frontend can safely call customers.map(...)
     */
    @GetMapping
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    /**
     * ✅ Fetch customers by shift (Morning / Night)
     * Also returns a plain list instead of a wrapped map
     */
    @GetMapping("/{shift}")
    public List<Customer> getCustomersByShift(@PathVariable String shift) {
        return customerRepository.findByShift(shift);
    }

    /**
     * ✅ Add a new customer
     */
    @PostMapping
    public Customer addCustomer(@RequestBody Customer customer) {
        if (customer.getFullName() == null || customer.getFullName().isBlank()) {
            throw new RuntimeException("Full name is required");
        }
        return customerRepository.save(customer);
    }

    /**
     * ✅ Update an existing customer
     */
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

    /**
     * ✅ Delete a customer
     */
    @DeleteMapping("/{id}")
    public void deleteCustomer(@PathVariable Long id) {
        if (!customerRepository.existsById(id)) {
            throw new RuntimeException("Customer not found with ID: " + id);
        }
        customerRepository.deleteById(id);
    }
}
