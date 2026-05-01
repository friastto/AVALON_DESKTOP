package org.avalon.desktop.sales.infrastructure.persistence.sqlite;

import com.google.inject.Inject;
import org.avalon.desktop.config.DatabaseManager;
import org.avalon.desktop.sales.domain.model.Sale;
import org.avalon.desktop.sales.domain.model.SaleItem;
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

    @Override
    public void save(Sale sale) {
        String saleSql = "INSERT INTO sales (date, total) VALUES (?, ?)";
        String itemSql = "INSERT INTO sale_items (sale_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(saleSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, sale.date().toString());
                pstmt.setBigDecimal(2, sale.total());
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
                conn.commit();
            } catch (SQLException e) { conn.rollback(); throw e; }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public List<Sale> findAll() { return new ArrayList<>(); }

    @Override
    public BigDecimal getTotalSalesBetween(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT SUM(total) FROM sales WHERE date BETWEEN ? AND ?";
        try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, start.toString());
            pstmt.setString(2, end.toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) { BigDecimal res = rs.getBigDecimal(1); return res != null ? res : BigDecimal.ZERO; }
        } catch (SQLException e) { e.printStackTrace(); }
        return BigDecimal.ZERO;
    }

    @Override
    public long getSalesCountBetween(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT COUNT(*) FROM sales WHERE date BETWEEN ? AND ?";
        try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        String sql = "SELECT strftime('%H', date) as hour, SUM(total) FROM sales WHERE date BETWEEN ? AND ? GROUP BY hour ORDER BY hour";
        try (Connection conn = dbManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
            while (rs.next()) result.put(rs.getString(1), rs.getDouble(2)); // <--- CAMBIO A DOUBLE
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }
}
