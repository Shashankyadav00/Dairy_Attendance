package com.milkattendence.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "milk_entries")
public class MilkEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;
    private String shift;
    private double litres;
    private double rate;
    private double amount;

    @Column(nullable = false)
    private LocalDate date = LocalDate.now();

    // ⭐ NEW FIELD — Links entry to a specific user
    @Column(nullable = false)
    private Long userId;

    public MilkEntry() {}

    public MilkEntry(String customerName, String shift, double litres, double rate, double amount, LocalDate date, Long userId) {
        this.customerName = customerName;
        this.shift = shift;
        this.litres = litres;
        this.rate = rate;
        this.amount = amount;
        this.date = (date != null ? date : LocalDate.now());
        this.userId = userId;
    }

    // Getters and setters
    public Long getId() { return id; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }

    public double getLitres() { return litres; }
    public void setLitres(double litres) { this.litres = litres; }

    public double getRate() { return rate; }
    public void setRate(double rate) { this.rate = rate; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    @Override
    public String toString() {
        return "MilkEntry{" +
                "id=" + id +
                ", customerName='" + customerName + '\'' +
                ", shift='" + shift + '\'' +
                ", litres=" + litres +
                ", rate=" + rate +
                ", amount=" + amount +
                ", date=" + date +
                ", userId=" + userId +
                '}';
    }
}
