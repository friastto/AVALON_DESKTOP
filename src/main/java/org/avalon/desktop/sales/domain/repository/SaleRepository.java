package org.avalon.desktop.sales.domain.repository;

import org.avalon.desktop.sales.domain.model.Sale;
import org.avalon.desktop.sales.domain.model.SaleStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface SaleRepository {
    void save(Sale sale);
    Optional<Sale> findById(Long id);
    void updateStatus(Long saleId, SaleStatus status);

    // Métodos transaccionales (aceptan Connection)
    void save(Sale sale, Connection conn);
    Optional<Sale> findById(Long id, Connection conn);
    void updateStatus(Long saleId, SaleStatus status, Connection conn);

    List<Sale> findAll();
    BigDecimal getTotalSalesBetween(LocalDateTime start, LocalDateTime end);
    long getSalesCountBetween(LocalDateTime start, LocalDateTime end);
    Map<String, BigDecimal> getSalesByPeriod(LocalDateTime start, LocalDateTime end, String periodType);
    Map<String, Double> getTopProducts(LocalDateTime start, LocalDateTime end, int limit);
}
