package org.avalon.desktop.auth.infrastructure.persistence.sqlite;

import com.google.inject.Inject;
import org.avalon.desktop.auth.domain.model.User;
import org.avalon.desktop.auth.domain.repository.UserRepository;
import org.avalon.desktop.config.DatabaseManager;

import java.sql.*;
import java.util.Optional;

public class SqliteUserRepository implements UserRepository {
    private final DatabaseManager dbManager;

    @Inject
    public SqliteUserRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT id, username, password, role, is_first_login FROM users WHERE username = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(new User(
                    rs.getLong("id"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("role"),
                    rs.getBoolean("is_first_login") // Read is_first_login
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public void save(User user) {
        String sql = "INSERT INTO users (username, password, role, is_first_login) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.username());
            pstmt.setString(2, user.password());
            pstmt.setString(3, user.role());
            pstmt.setBoolean(4, user.isFirstLogin()); // Save is_first_login
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateUserCredentials(User user) {
        String sql = "UPDATE users SET username = ?, password = ?, is_first_login = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.username());
            pstmt.setString(2, user.password());
            pstmt.setBoolean(3, user.isFirstLogin());
            pstmt.setLong(4, user.id());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
