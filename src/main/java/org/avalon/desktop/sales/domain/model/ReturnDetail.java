package org.avalon.desktop.sales.domain.model;

import org.avalon.desktop.product.domain.model.Product;

import java.math.BigDecimal;

public record ReturnDetail(
    Long productId,
    String productName,
    Double quantity,
    BigDecimal price,
    ReturnType returnType,
    String reason
) {}
