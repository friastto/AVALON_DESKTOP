package org.avalon.desktop.sales.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record Return(
    Long id,
    Long saleId,
    LocalDateTime returnDate,
    Long userId, // ID del usuario que registra la devolución
    BigDecimal totalReturned,
    List<ReturnDetail> details
) {
    public BigDecimal calculateTotalReturned() {
        return details.stream()
                .map(detail -> detail.price().multiply(BigDecimal.valueOf(detail.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isFullReturn(BigDecimal originalSaleTotal) {
        return calculateTotalReturned().compareTo(originalSaleTotal) >= 0;
    }
}
