package org.avalon.desktop.product.domain.model;

import java.time.LocalDateTime;

public record InventoryMovement(
    Long id,
    Long productId,
    String productName,
    Double quantityChange, // Positivo para entrada, negativo para salida
    InventoryMovementType type,
    String reference, // Ej: ID de venta, ID de devolución
    LocalDateTime timestamp
) {}
