package org.avalon.desktop.product.domain.repository;

import org.avalon.desktop.product.domain.model.Product;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    List<Product> findAll();
    List<Product> findByName(String name);
    Optional<Product> findById(Long id);
    Optional<Product> findByBarcode(String barcode);
    void save(Product product);
    void update(Product product);
    void delete(Long id);
    void updateStock(Long productId, Double quantity);

    // Métodos transaccionales (aceptan Connection)
    void updateStock(Long productId, Double quantity, Connection conn);
}
