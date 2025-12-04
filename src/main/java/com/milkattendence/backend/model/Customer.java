package com.milkattendence.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "customers")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    private String nickname;
    private String shift;
    private double pricePerLitre;

    // ADD THIS FIELD 👇
    private Long userId;

    public Customer() {}

    public Customer(String fullName, String nickname, String shift, double pricePerLitre, Long userId) {
        this.fullName = fullName;
        this.nickname = nickname;
        this.shift = shift;
        this.pricePerLitre = pricePerLitre;
        this.userId = userId;
    }

    // Getters and Setters
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }

    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getNickname() { return nickname; }

    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getShift() { return shift; }

    public void setShift(String shift) { this.shift = shift; }

    public double getPricePerLitre() { return pricePerLitre; }

    public void setPricePerLitre(double pricePerLitre) { this.pricePerLitre = pricePerLitre; }

    public Long getUserId() { return userId; }

    public void setUserId(Long userId) { this.userId = userId; }
}
