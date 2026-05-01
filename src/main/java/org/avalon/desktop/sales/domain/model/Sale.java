package org.avalon.desktop.sales.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record Sale(Long id, LocalDateTime date, BigDecimal total, List<SaleItem> items) {}
