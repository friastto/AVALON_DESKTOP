package org.avalon.desktop.product.domain.model;

import java.math.BigDecimal;

public record Product(
        Long id,
        String name,
        BigDecimal price,
        Integer stock,
        String barcode,
        ProductType productType) {}
