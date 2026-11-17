package com.cronos.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AccountRequests {

    private AccountRequests() {}

    public record UpdateProfile(
            @NotBlank(message = "First name is required") @Size(max = 100) String firstName,
            @NotBlank(message = "Last name is required") @Size(max = 100) String lastName,
            @Size(max = 40) String phone) {}

    public record ChangePassword(
            @NotBlank(message = "Current password is required") String currentPassword,
            @NotBlank @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters") String newPassword) {}

    public record AddressRequest(
            @Size(max = 60) String label,
            @NotBlank(message = "Recipient is required") @Size(max = 150) String recipient,
            @NotBlank(message = "Address is required") @Size(max = 255) String line1,
            @Size(max = 255) String line2,
            @NotBlank(message = "City is required") @Size(max = 120) String city,
            @Size(max = 120) String region,
            @Size(max = 30) String postalCode,
            @NotBlank(message = "Country is required") @Size(max = 120) String country,
            @Size(max = 40) String phone,
            Boolean defaultAddress) {}

    public record AddressResponse(String id, String label, String recipient, String line1, String line2, String city,
                                  String region, String postalCode, String country, String phone, boolean defaultAddress) {}
}
