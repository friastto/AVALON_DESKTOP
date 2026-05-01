package org.avalon.desktop.sales.application.service;

import com.google.inject.Inject;
import org.avalon.desktop.sales.domain.repository.SaleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

public class SalesReportService {
    private final SaleRepository saleRepository;

    @Inject
    public SalesReportService(SaleRepository saleRepository) {
        this.saleRepository = saleRepository;
    }

    public SalesReportData getReport(LocalDateTime start, LocalDateTime end) {
        BigDecimal total = saleRepository.getTotalSalesBetween(start, end);
        long count = saleRepository.getSalesCountBetween(start, end);
        BigDecimal average = count > 0 ? total.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        
        return new SalesReportData(
            total,
            count,
            average,
            saleRepository.getSalesByPeriod(start, end, "HOUR"),
            saleRepository.getTopProducts(start, end, 5)
        );
    }

    public record SalesReportData(
        BigDecimal totalSales,
        long salesCount,
        BigDecimal averageSale,
        Map<String, BigDecimal> salesOverTime,
        Map<String, Double> topProducts // <--- CAMBIO A DOUBLE
    ) {}
}
