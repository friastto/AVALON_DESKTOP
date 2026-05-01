package org.avalon.desktop.product.application.service;

import com.google.inject.Inject;
import org.avalon.desktop.product.domain.model.Product;
import org.avalon.desktop.product.domain.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;

public class InventoryService {
    private final ProductRepository productRepository;

    @Inject
    public InventoryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> searchProducts(String query) {
        return productRepository.findByName(query);
    }

    public void addProduct(Product product) {
        productRepository.save(product);
    }

    public void updateProduct(Product product) {
        productRepository.update(product);
    }

    public void deleteProduct(Long id) {
        productRepository.delete(id);
    }

    public void adjustStock(Long id, int amount) {
        productRepository.updateStock(id, amount);
    }

    public InventoryStats getStats() {
        List<Product> products = productRepository.findAll();
        long total = products.size();
        long lowStock = products.stream().filter(p -> p.stock() > 0 && p.stock() <= 5).count();
        long outOfStock = products.stream().filter(p -> p.stock() <= 0).count();
        BigDecimal totalValue = products.stream()
                .map(p -> p.price().multiply(BigDecimal.valueOf(p.stock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new InventoryStats(total, lowStock, outOfStock, totalValue);
    }

    public record InventoryStats(long totalProducts, long lowStock, long outOfStock, BigDecimal totalValue) {}
}
