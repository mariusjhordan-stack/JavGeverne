package com.cronos.auth.service;

import com.cronos.auth.dto.*;
import com.cronos.auth.dto.AccountRequests.AddressRequest;
import com.cronos.auth.dto.AccountRequests.AddressResponse;
import com.cronos.auth.entity.Address;
import com.cronos.auth.entity.RefreshToken;
import com.cronos.auth.entity.Role;
import com.cronos.auth.entity.User;
import com.cronos.auth.repository.AddressRepository;
import com.cronos.auth.repository.RefreshTokenRepository;
import com.cronos.auth.repository.UserRepository;
import com.cronos.auth.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private static final int MAX_ADDRESSES = 10;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokens;
    private final AddressRepository addresses;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokens, AddressRepository addresses,
                       PasswordEncoder passwordEncoder, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.refreshTokens = refreshTokens;
        this.addresses = addresses;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        User user = userRepository.save(new User(email, passwordEncoder.encode(request.password()),
                request.firstName().trim(), request.lastName().trim(), Role.USER));
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException e) {
            throw new BadCredentialsException("Invalid email or password");
        }
        User user = userRepository.findByEmail(email).orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        refreshTokens.deleteExpired(LocalDateTime.now());
        return issueTokens(user);
    }

    /** Single-use rotation. Presenting a token that was already exchanged revokes the whole session family. */
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        UUID tokenId;
        try {
            tokenId = UUID.fromString(jwtService.extractTokenId(refreshToken));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        RefreshToken stored = refreshTokens.findById(tokenId).orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (stored.isRevoked()) {
            refreshTokens.revokeAllForUser(stored.getUserId());
            throw new IllegalArgumentException("Refresh token already used");
        }
        if (refreshTokens.revoke(tokenId) == 0 || stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invalid refresh token");
        }
        User user = userRepository.findById(stored.getUserId()).filter(User::isEnabled)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        try {
            if (refreshToken != null && jwtService.isTokenValid(refreshToken) && jwtService.isRefreshToken(refreshToken)) {
                refreshTokens.revoke(UUID.fromString(jwtService.extractTokenId(refreshToken)));
            }
        } catch (RuntimeException ignored) {
            // Logging out with a broken token is still a successful logout.
        }
    }

    private AuthResponse issueTokens(User user) {
        UUID tokenId = UUID.randomUUID();
        refreshTokens.save(new RefreshToken(tokenId, user.getId(),
                LocalDateTime.now().plusNanos(jwtService.getRefreshExpirationMillis() * 1_000_000)));
        String access = jwtService.generateAccessToken(user.getEmail(), user.getRole().name(), user.getId().toString());
        String refresh = jwtService.generateRefreshToken(user.getEmail(), tokenId.toString());
        return new AuthResponse(access, refresh, user.getEmail(), user.getRole().name(), user.getFirstName(), user.getLastName());
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        return toProfile(user(email));
    }

    @Transactional
    public UserProfileResponse updateProfile(String email, AccountRequests.UpdateProfile request) {
        User user = user(email);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setPhone(request.phone() == null || request.phone().isBlank() ? null : request.phone().trim());
        return toProfile(user);
    }

    @Transactional
    public void changePassword(String email, AccountRequests.ChangePassword request) {
        User user = user(email);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokens.revokeAllForUser(user.getId());
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> listAddresses(String email) {
        return addresses.findByUserIdOrderByDefaultAddressDescCreatedAtAsc(user(email).getId()).stream().map(AuthService::toAddress).toList();
    }

    @Transactional
    public AddressResponse saveAddress(String email, UUID addressId, AddressRequest request) {
        UUID userId = user(email).getId();
        Address address;
        if (addressId == null) {
            if (addresses.countByUserId(userId) >= MAX_ADDRESSES) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "You can save up to " + MAX_ADDRESSES + " addresses");
            }
            address = new Address(userId);
        } else {
            address = addresses.findByIdAndUserId(addressId, userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
        }
        address.setLabel(request.label());
        address.setRecipient(request.recipient().trim());
        address.setLine1(request.line1().trim());
        address.setLine2(request.line2());
        address.setCity(request.city().trim());
        address.setRegion(request.region());
        address.setPostalCode(request.postalCode());
        address.setCountry(request.country().trim());
        address.setPhone(request.phone());
        boolean makeDefault = Boolean.TRUE.equals(request.defaultAddress()) || addresses.countByUserId(userId) == 0;
        if (makeDefault) {
            addresses.findByUserIdOrderByDefaultAddressDescCreatedAtAsc(userId).forEach(a -> a.setDefaultAddress(false));
        }
        address.setDefaultAddress(makeDefault || (addressId != null && address.isDefaultAddress() && request.defaultAddress() == null));
        return toAddress(addresses.save(address));
    }

    @Transactional
    public void deleteAddress(String email, UUID addressId) {
        Address address = addresses.findByIdAndUserId(addressId, user(email).getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Address not found"));
        addresses.delete(address);
    }

    public UserProfileResponse validateToken(String token) {
        if (!jwtService.isTokenValid(token) || !jwtService.isAccessToken(token)) {
            throw new IllegalArgumentException("Invalid token");
        }
        return getProfile(jwtService.extractEmail(token));
    }

    private User user(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private static UserProfileResponse toProfile(User user) {
        return new UserProfileResponse(user.getId().toString(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getPhone(), user.getRole().name(), user.getCreatedAt());
    }

    private static AddressResponse toAddress(Address a) {
        return new AddressResponse(a.getId().toString(), a.getLabel(), a.getRecipient(), a.getLine1(), a.getLine2(), a.getCity(),
                a.getRegion(), a.getPostalCode(), a.getCountry(), a.getPhone(), a.isDefaultAddress());
    }
}
