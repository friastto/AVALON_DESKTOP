package org.avalon.desktop.auth.infrastructure.security;

import org.avalon.desktop.auth.domain.service.PasswordHasher;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class DefaultPasswordHasher implements PasswordHasher {

    // IMPORTANT: In a production environment, use a strong, adaptive hashing algorithm like BCrypt or Argon2.
    // This simple SHA-256 implementation is for demonstration purposes only.

    @Override
    public String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    @Override
    public boolean checkPassword(String plainPassword, String hashedPassword) {
        return hashPassword(plainPassword).equals(hashedPassword);
    }
}
