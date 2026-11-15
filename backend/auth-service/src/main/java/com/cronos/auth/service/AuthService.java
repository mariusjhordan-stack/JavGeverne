package com.cronos.auth.service;

import com.cronos.auth.dto.*;
import com.cronos.auth.entity.Role;
import com.cronos.auth.entity.User;
import com.cronos.auth.repository.UserRepository;
import com.cronos.auth.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName(),
                Role.USER
        );
        user = userRepository.save(user);

        String token = jwtService.generateAccessToken(user.getEmail(), user.getRole().name(), user.getId().toString());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        return new AuthResponse(token, refreshToken, user.getEmail(), user.getRole().name(),
                user.getFirstName(), user.getLastName());
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        String token = jwtService.generateAccessToken(user.getEmail(), user.getRole().name(), user.getId().toString());
        String refreshToken = jwtService.generateRefreshToken(user.getEmail());

        return new AuthResponse(token, refreshToken, user.getEmail(), user.getRole().name(),
                user.getFirstName(), user.getLastName());
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        String email = jwtService.extractEmail(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String newAccessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole().name(), user.getId().toString());
        String newRefreshToken = jwtService.generateRefreshToken(user.getEmail());

        return new AuthResponse(newAccessToken, newRefreshToken, user.getEmail(), user.getRole().name(),
                user.getFirstName(), user.getLastName());
    }

    public UserProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return new UserProfileResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getCreatedAt()
        );
    }

    public UserProfileResponse validateToken(String token) {
        if (!jwtService.isTokenValid(token) || !jwtService.isAccessToken(token)) {
            throw new IllegalArgumentException("Invalid token");
        }

        String email = jwtService.extractEmail(token);
        return getProfile(email);
    }
}
