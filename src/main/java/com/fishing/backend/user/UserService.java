package com.fishing.backend.user;

import com.fishing.backend.user.dto.UserProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<UserProfile> findProfile(UUID id) {
        return userRepository.findById(id)
                .map(user -> new UserProfile(user.getId(), user.getEmail(), user.getDisplayName()));
    }
}
