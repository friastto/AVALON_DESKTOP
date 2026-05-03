package org.avalon.desktop.product.infrastructure.persistence.sqlite;

import com.google.inject.Inject;
import org.avalon.desktop.config.DatabaseManager;
import org.avalon.desktop.product.domain.model.Product;
import org.avalon.desktop.product.domain.model.ProductType;
import org.avalon.desktop.product.domain.repository.ProductRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqliteProductRepository implements ProductRepository {
    private final DatabaseManager dbManager;

    @Inject
    public SqliteProductRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    private Product mapResultSet(ResultSet rs) throws SQLException {
        return new Product(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getBigDecimal("price"),
            rs.getInt("stock"),
            rs.getString("barcode"),
            ProductType.valueOf(rs.getString("product_type"))
        );
    }

    // --- Métodos no transaccionales ---
    @Override
    public List<Product> findAll() {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products";
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                products.add(mapResultSet(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return products;
    }

    @Override
    public List<Product> findByName(String name) {
        List<Product> products = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE name LIKE ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + name + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                products.add(mapResultSet(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return products;
    }

    @Override
    public Optional<Product> findByBarcode(String barcode) {
        String sql = "SELECT * FROM products WHERE barcode = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, barcode);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return Optional.of(mapResultSet(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    @Override
    public Optional<Product> findById(Long id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return Optional.of(mapResultSet(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    @Override
    public void save(Product product) {
        String sql = "INSERT INTO products (name, price, stock, barcode, product_type) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, product.name());
            pstmt.setBigDecimal(2, product.price());
            pstmt.setInt(3, product.stock());
            pstmt.setString(4, product.barcode());
            pstmt.setString(5, product.productType().name());
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void update(Product product) {
        String sql = "UPDATE products SET name = ?, price = ?, stock = ?, barcode = ?, product_type = ? WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, product.name());
            pstmt.setBigDecimal(2, product.price());
            pstmt.setInt(3, product.stock());
            pstmt.setString(4, product.barcode());
            pstmt.setString(5, product.productType().name());
            pstmt.setLong(6, product.id());
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public void updateStock(Long productId, Double quantity) {
        try (Connection conn = dbManager.getConnection()) {
            updateStock(productId, quantity, conn);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- Métodos transaccionales ---
    @Override
    public void updateStock(Long productId, Double quantity, Connection conn) {
        String sql = "UPDATE products SET stock = stock + ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, quantity.intValue()); // Redondeamos a entero para el stock
            pstmt.setLong(2, productId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
