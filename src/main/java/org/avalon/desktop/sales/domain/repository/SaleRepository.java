package org.avalon.desktop.sales.domain.repository;

import org.avalon.desktop.sales.domain.model.Sale;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface SaleRepository {
    void save(Sale sale);
    List<Sale> findAll();
    BigDecimal getTotalSalesBetween(LocalDateTime start, LocalDateTime end);
    long getSalesCountBetween(LocalDateTime start, LocalDateTime end);
    Map<String, BigDecimal> getSalesByPeriod(LocalDateTime start, LocalDateTime end, String periodType);
    Map<String, Double> getTopProducts(LocalDateTime start, LocalDateTime end, int limit); // <--- CAMBIO A DOUBLE
}
