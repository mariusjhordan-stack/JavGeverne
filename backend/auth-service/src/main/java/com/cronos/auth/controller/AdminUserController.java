package com.cronos.auth.controller;

import com.cronos.auth.dto.AdminUserResponse;
import com.cronos.auth.entity.User;
import com.cronos.auth.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public Page<AdminUserResponse> list(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        @RequestParam(required = false) String q) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("createdAt").descending());
        Page<User> users = q == null || q.isBlank()
                ? userRepository.findAll(pageable)
                : userRepository.search(q.trim().toLowerCase(), pageable);
        return users.map(AdminUserController::toResponse);
    }

    @GetMapping("/{id}")
    public AdminUserResponse get(@PathVariable UUID id) {
        return userRepository.findById(id).map(AdminUserController::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private static AdminUserResponse toResponse(User u) {
        return new AdminUserResponse(u.getId().toString(), u.getEmail(), u.getFirstName(), u.getLastName(),
                u.getRole().name(), u.isEnabled(), u.getCreatedAt());
    }
}
