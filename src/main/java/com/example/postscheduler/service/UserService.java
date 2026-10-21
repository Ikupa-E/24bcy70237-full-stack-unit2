package com.example.postscheduler.service;

import com.example.postscheduler.dto.UserResponse;
import com.example.postscheduler.exception.InvalidOperationException;
import com.example.postscheduler.exception.ResourceNotFoundException;
import com.example.postscheduler.model.AppUser;
import com.example.postscheduler.model.Role;
import com.example.postscheduler.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final AppUserRepository repository;

    public UserService(AppUserRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers() {
        return repository.findAll(Sort.by("id")).stream().map(UserResponse::from).toList();
    }

    public UserResponse changeRole(Long id, Role newRole, String currentUsername) {
        AppUser user = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
        if (user.getUsername().equalsIgnoreCase(currentUsername)) {
            throw new InvalidOperationException("You cannot change your own role");   // avoids locking out the last admin
        }
        user.setRole(newRole);
        AppUser saved = repository.save(user);
        log.info("Role of user '{}' changed to {} by '{}'", saved.getUsername(), newRole, currentUsername);
        return UserResponse.from(saved);
    }
}
