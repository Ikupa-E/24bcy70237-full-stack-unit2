package com.example.postscheduler.service;

import com.example.postscheduler.dto.AuthResponse;
import com.example.postscheduler.dto.LoginRequest;
import com.example.postscheduler.dto.RefreshRequest;
import com.example.postscheduler.dto.RegisterRequest;
import com.example.postscheduler.dto.UserResponse;
import com.example.postscheduler.exception.DuplicateResourceException;
import com.example.postscheduler.exception.InvalidTokenException;
import com.example.postscheduler.exception.ResourceNotFoundException;
import com.example.postscheduler.model.AppUser;
import com.example.postscheduler.model.Role;
import com.example.postscheduler.repository.AppUserRepository;
import com.example.postscheduler.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(AppUserRepository repository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    /** Public registration always creates a normal USER. Only an admin can promote someone. */
    public AuthResponse register(RegisterRequest request) {
        if (repository.existsByUsernameIgnoreCase(request.username())) {
            throw new DuplicateResourceException("Username '" + request.username() + "' is already taken");
        }
        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        AppUser saved = repository.save(user);
        log.info("New user registered: id={}, username={}", saved.getId(), saved.getUsername());
        return buildResponse(saved, refreshTokenService.issue(saved));
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            log.warn("Failed login attempt for username '{}'", sanitize(request.username()));
            throw ex;
        }
        AppUser user = repository.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        log.info("User logged in: username={}, role={}", user.getUsername(), user.getRole());
        return buildResponse(user, refreshTokenService.issue(user));
    }

    /**
     * Exchanges a valid refresh token for a NEW access token and a NEW refresh token (rotation).
     * The access token is built from the user's CURRENT role in the database, so role changes
     * take effect at the next refresh.
     * noRollbackFor: see RefreshTokenService.rotate (reuse detection must not be rolled back).
     */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public AuthResponse refresh(RefreshRequest request) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(request.refreshToken());
        return buildResponse(rotation.user(), rotation.newRefreshToken());
    }

    public void logout(RefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(String username) {
        return repository.findByUsernameIgnoreCase(username)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private AuthResponse buildResponse(AppUser user, String refreshToken) {
        String accessToken = jwtService.generateToken(user.getUsername(), user.getRole());
        return new AuthResponse(accessToken, refreshToken, "Bearer", jwtService.getExpirationSeconds(),
                refreshTokenService.getExpirationSeconds(), user.getUsername(), user.getRole());
    }

    /** Stops a user-supplied name from forging extra lines in the log. */
    private String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]", "_");
    }
}
