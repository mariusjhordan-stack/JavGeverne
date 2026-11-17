package com.cronos.auth.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_addresses")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(length = 60)
    private String label;

    @Column(nullable = false, length = 150)
    private String recipient;

    @Column(nullable = false)
    private String line1;

    private String line2;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(length = 120)
    private String region;

    @Column(name = "postal_code", length = 30)
    private String postalCode;

    @Column(nullable = false, length = 120)
    private String country;

    @Column(length = 40)
    private String phone;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected Address() {}

    public Address(UUID userId) {
        this.userId = userId;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getLine1() { return line1; }
    public void setLine1(String line1) { this.line1 = line1; }
    public String getLine2() { return line2; }
    public void setLine2(String line2) { this.line2 = line2; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public boolean isDefaultAddress() { return defaultAddress; }
    public void setDefaultAddress(boolean defaultAddress) { this.defaultAddress = defaultAddress; }
}
