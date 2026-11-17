package com.cronos.auth.config;

import com.cronos.auth.entity.Role;
import com.cronos.auth.entity.User;
import com.cronos.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@cronos.com")) {
            User admin = new User(
                    "admin@cronos.com",
                    passwordEncoder.encode("Admin123!"),
                    "Admin",
                    "Cronos",
                    Role.ADMIN
            );
            userRepository.save(admin);
            log.info("Demo admin user created: admin@cronos.com");
        }

        if (!userRepository.existsByEmail("user@cronos.com")) {
            User user = new User(
                    "user@cronos.com",
                    passwordEncoder.encode("User123!"),
                    "Demo",
                    "User",
                    Role.USER
            );
            userRepository.save(user);
            log.info("Demo user created: user@cronos.com");
        }
    }
}
