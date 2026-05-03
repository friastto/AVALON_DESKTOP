package org.avalon.desktop.product.infrastructure.persistence.sqlite;

import com.google.inject.Inject;
import org.avalon.desktop.config.DatabaseManager;
import org.avalon.desktop.product.domain.model.InventoryMovement;
import org.avalon.desktop.product.domain.repository.InventoryMovementRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SqliteInventoryMovementRepository implements InventoryMovementRepository {
    private final DatabaseManager dbManager;

    @Inject
    public SqliteInventoryMovementRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // --- Métodos no transaccionales ---
    @Override
    public void save(InventoryMovement movement) {
        try (Connection conn = dbManager.getConnection()) {
            save(movement, conn);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- Métodos transaccionales ---
    @Override
    public void save(InventoryMovement movement, Connection conn) {
        String sql = "INSERT INTO inventory_movements (product_id, product_name, quantity_change, type, reference, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, movement.productId());
            pstmt.setString(2, movement.productName());
            pstmt.setDouble(3, movement.quantityChange());
            pstmt.setString(4, movement.type().name());
            pstmt.setString(5, movement.reference());
            pstmt.setString(6, movement.timestamp().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
