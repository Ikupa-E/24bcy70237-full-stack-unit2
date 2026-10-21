package com.example.postscheduler.config;

import com.example.postscheduler.model.AppUser;
import com.example.postscheduler.model.Role;
import com.example.postscheduler.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Makes sure an admin account exists (credentials come from application.properties / environment).
 * When app.seed.enabled=true it also creates a demo USER account for testing.
 */
@Component
@Order(1)
public class SecuritySeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SecuritySeeder.class);

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final boolean seedDemoData;

    public SecuritySeeder(AppUserRepository repository, PasswordEncoder passwordEncoder,
                          @Value("${app.security.admin-username}") String adminUsername,
                          @Value("${app.security.admin-password}") String adminPassword,
                          @Value("${app.seed.enabled:false}") boolean seedDemoData) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.seedDemoData = seedDemoData;
    }

    @Override
    public void run(String... args) {
        ensureUser(adminUsername, adminPassword, Role.ADMIN);
        if (seedDemoData) {
            ensureUser("user", "User@123", Role.USER);
        }
    }

    private void ensureUser(String username, String rawPassword, Role role) {
        if (repository.existsByUsernameIgnoreCase(username)) {
            return;
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        repository.save(user);
        log.info("Created {} account '{}'", role, username);
    }
}
