package org.avalon.desktop.sales.domain.model;

import org.avalon.desktop.product.domain.model.Product;
import java.math.BigDecimal;

public record SaleItem(Product product, Double quantity, BigDecimal price) {}
