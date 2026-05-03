package org.avalon.desktop.auth.domain.repository;

import org.avalon.desktop.auth.domain.model.User;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUsername(String username);
    void save(User user);
    void updateUserCredentials(User user); // New method
}
