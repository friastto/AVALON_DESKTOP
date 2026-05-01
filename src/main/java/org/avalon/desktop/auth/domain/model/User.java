package org.avalon.desktop.auth.domain.model;

public record User(Long id, String username, String password, String role) {}
