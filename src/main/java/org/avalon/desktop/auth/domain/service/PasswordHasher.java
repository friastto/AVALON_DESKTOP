package org.avalon.desktop.auth.domain.service;

public interface PasswordHasher {
    String hashPassword(String password);
    boolean checkPassword(String plainPassword, String hashedPassword);
}
