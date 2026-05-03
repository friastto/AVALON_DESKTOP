package org.avalon.desktop.auth.domain.model;

public record User(Long id, String username, String password, String role, boolean isFirstLogin) {

    public User withIsFirstLogin(boolean isFirstLogin) {
        return new User(this.id, this.username, this.password, this.role, isFirstLogin);
    }

    public User withUsernameAndPassword(String username, String password) {
        return new User(this.id, username, password, this.role, this.isFirstLogin);
    }
}
