package org.avalon.desktop.product.domain.repository;

import org.avalon.desktop.product.domain.model.InventoryMovement;

import java.sql.Connection;

public interface InventoryMovementRepository {
    void save(InventoryMovement movement);

    // Métodos transaccionales (aceptan Connection)
    void save(InventoryMovement movement, Connection conn);
}
