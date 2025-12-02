package com.milkattendence.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(
    name = "payments",
    uniqueConstraints = @UniqueConstraint(columnNames = {"customer_name", "shift", "date"})
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name")
    private String customerName;

    private String shift;

    private boolean paid;

    private LocalDate date;

    public Payment() {}

    public Payment(String customerName, String shift, boolean paid, LocalDate date) {
        this.customerName = customerName;
        this.shift = shift;
        this.paid = paid;
        this.date = date;
    }

    // Getters and setters
    public Long getId() { return id; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }

    public boolean isPaid() { return paid; }
    public void setPaid(boolean paid) { this.paid = paid; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}
