package com.milkattendence.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Foreign key to link customer to the user who owns it
    private Long userId;

    private String fullName;
    private String nickname;
    private String shift;
    private Double pricePerLitre;
    
    // ==========================================================
    // 🟢 FIX: Added 'active' status for soft-delete and filtering
    // ==========================================================
    private boolean active = true; 

    // --- Constructors ---
    public Customer() {
    }
    
    // You might have a more detailed constructor here...
    // public Customer(Long userId, String fullName, ...) { ... }

    // --- Getters and Setters ---
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public Double getPricePerLitre() {
        return pricePerLitre;
    }

    public void setPricePerLitre(Double pricePerLitre) {
        this.pricePerLitre = pricePerLitre;
    }

    // ==========================================================
    // 🟢 FIX: Getter and Setter for 'active'
    // ==========================================================
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}