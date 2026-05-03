package org.avalon.desktop.sales.infrastructure.persistence.sqlite;

import com.google.inject.Inject;
import org.avalon.desktop.config.DatabaseManager;
import org.avalon.desktop.product.domain.model.Product;
import org.avalon.desktop.product.domain.model.ProductType;
import org.avalon.desktop.sales.domain.model.Sale;
import org.avalon.desktop.sales.domain.model.SaleItem;
import org.avalon.desktop.sales.domain.model.SaleStatus;
import org.avalon.desktop.sales.domain.repository.SaleRepository;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class SqliteSaleRepository implements SaleRepository {
    private final DatabaseManager dbManager;

    @Inject
    public SqliteSaleRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // --- Métodos no transaccionales (obtienen y cierran su propia conexión) ---
    @Override
    public void save(Sale sale) {
        try (Connection conn = dbManager.getConnection()) {
            save(sale, conn);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public Optional<Sale> findById(Long id) {
        try (Connection conn = dbManager.getConnection()) {
            return findById(id, conn);
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    @Override
    public void updateStatus(Long saleId, SaleStatus status) {
        try (Connection conn = dbManager.getConnection()) {
            updateStatus(saleId, status, conn);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    // --- Métodos transaccionales (usan la conexión provista y NO la cierran) ---
    @Override
    public void save(Sale sale, Connection conn) {
        String saleSql = "INSERT INTO sales (date, total, status) VALUES (?, ?, ?)";
        String itemSql = "INSERT INTO sale_items (sale_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
        try {
            try (PreparedStatement pstmt = conn.prepareStatement(saleSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, sale.date().toString());
                pstmt.setBigDecimal(2, sale.total());
                pstmt.setString(3, sale.status().name());
                pstmt.executeUpdate();
                
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    long saleId = rs.getLong(1);
                    try (PreparedStatement itemPstmt = conn.prepareStatement(itemSql)) {
                        for (SaleItem item : sale.items()) {
                            itemPstmt.setLong(1, saleId);
                            itemPstmt.setLong(2, item.product().id());
                            itemPstmt.setDouble(3, item.quantity());
                            itemPstmt.setBigDecimal(4, item.price());
                            itemPstmt.addBatch();
                        }
                        itemPstmt.executeBatch();
                    }
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public Optional<Sale> findById(Long id, Connection conn) {
        String saleSql = "SELECT id, date, total, status FROM sales WHERE id = ?";
        // Asegúrate de que esta consulta obtenga todos los campos necesarios para construir Product
        String itemSql = "SELECT si.quantity, si.price, p.id, p.name, p.price as product_price, p.stock, p.barcode, p.product_type FROM sale_items si JOIN products p ON si.product_id = p.id WHERE si.sale_id = ?";
        try (PreparedStatement salePstmt = conn.prepareStatement(saleSql)) {
            salePstmt.setLong(1, id);
            ResultSet saleRs = salePstmt.executeQuery();
            
            if (saleRs.next()) {
                List<SaleItem> items = new ArrayList<>();
                try (PreparedStatement itemPstmt = conn.prepareStatement(itemSql)) {
                    itemPstmt.setLong(1, id);
                    ResultSet itemRs = itemPstmt.executeQuery();
                    while (itemRs.next()) {
                        Product product = new Product(
                            itemRs.getLong("id"),
                            itemRs.getString("name"),
                            itemRs.getBigDecimal("product_price"),
                            itemRs.getInt("stock"),
                            itemRs.getString("barcode"),
                            ProductType.valueOf(itemRs.getString("product_type"))
                        );
                        items.add(new SaleItem(
                            product,
                            itemRs.getDouble("quantity"),
                            itemRs.getBigDecimal("price")
                        ));
                    }
                }
                return Optional.of(new Sale(
                    saleRs.getLong("id"),
                    LocalDateTime.parse(saleRs.getString("date")),
                    saleRs.getBigDecimal("total"),
                    items,
                    SaleStatus.valueOf(saleRs.getString("status"))
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    @Override
    public void updateStatus(Long saleId, SaleStatus status, Connection conn) {
        String sql = "UPDATE sales SET status = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status.name());
            pstmt.setLong(2, saleId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public List<Sale> findAll() { return new ArrayList<>(); } // Implementar si es necesario

    @Override
    public BigDecimal getTotalSalesBetween(LocalDateTime start, LocalDateTime end) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT SUM(total) FROM sales WHERE date BETWEEN ? AND ?")) {
            pstmt.setString(1, start.toString());
            pstmt.setString(2, end.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) { BigDecimal res = rs.getBigDecimal(1); return res != null ? res : BigDecimal.ZERO; }
        } catch (SQLException e) { e.printStackTrace(); }
        return BigDecimal.ZERO;
    }

    @Override
    public long getSalesCountBetween(LocalDateTime start, LocalDateTime end) {
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM sales WHERE date BETWEEN ? AND ?")) {
            pstmt.setString(1, start.toString());
            pstmt.setString(2, end.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    @Override
    public Map<String, BigDecimal> getSalesByPeriod(LocalDateTime start, LocalDateTime end, String periodType) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT strftime('%H', date) as hour, SUM(total) FROM sales WHERE date BETWEEN ? AND ? GROUP BY hour ORDER BY hour")) {
            pstmt.setString(1, start.toString());
            pstmt.setString(2, end.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) result.put(rs.getString(1) + ":00", rs.getBigDecimal(2));
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }

    @Override
    public Map<String, Double> getTopProducts(LocalDateTime start, LocalDateTime end, int limit) {
        Map<String, Double> result = new LinkedHashMap<>();
        String sql = "SELECT p.name, SUM(si.quantity) as total_qty FROM sale_items si JOIN products p ON si.product_id = p.id JOIN sales s ON si.sale_id = s.id WHERE s.date BETWEEN ? AND ? GROUP BY p.id ORDER BY total_qty DESC LIMIT ?";
        try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, start.toString());
            pstmt.setString(2, end.toString());
            pstmt.setInt(3, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) result.put(rs.getString(1), rs.getDouble(2));
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }
}
