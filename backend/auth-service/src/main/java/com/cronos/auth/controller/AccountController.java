package com.cronos.auth.controller;

import com.cronos.auth.dto.AccountRequests;
import com.cronos.auth.dto.AccountRequests.AddressResponse;
import com.cronos.auth.dto.UserProfileResponse;
import com.cronos.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AccountController {

    private final AuthService authService;

    public AccountController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) Map<String, String> body) {
        authService.logout(body == null ? null : body.get("refreshToken"));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/profile")
    public UserProfileResponse updateProfile(Authentication authentication, @Valid @RequestBody AccountRequests.UpdateProfile request) {
        return authService.updateProfile(authentication.getName(), request);
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(Authentication authentication, @Valid @RequestBody AccountRequests.ChangePassword request) {
        authService.changePassword(authentication.getName(), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/addresses")
    public List<AddressResponse> addresses(Authentication authentication) {
        return authService.listAddresses(authentication.getName());
    }

    @PostMapping("/addresses")
    @ResponseStatus(HttpStatus.CREATED)
    public AddressResponse addAddress(Authentication authentication, @Valid @RequestBody AccountRequests.AddressRequest request) {
        return authService.saveAddress(authentication.getName(), null, request);
    }

    @PutMapping("/addresses/{id}")
    public AddressResponse updateAddress(Authentication authentication, @PathVariable UUID id,
                                         @Valid @RequestBody AccountRequests.AddressRequest request) {
        return authService.saveAddress(authentication.getName(), id, request);
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(Authentication authentication, @PathVariable UUID id) {
        authService.deleteAddress(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
