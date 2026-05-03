package org.avalon.desktop.sales.infrastructure.persistence.sqlite;

import com.google.inject.Inject;
import org.avalon.desktop.config.DatabaseManager;
import org.avalon.desktop.sales.domain.model.Return;
import org.avalon.desktop.sales.domain.model.ReturnDetail;
import org.avalon.desktop.sales.domain.repository.ReturnRepository;

import java.sql.*;
import java.util.Optional;

public class SqliteReturnRepository implements ReturnRepository {
    private final DatabaseManager dbManager;

    @Inject
    public SqliteReturnRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    // --- Métodos no transaccionales ---
    @Override
    public void save(Return returnObj) {
        try (Connection conn = dbManager.getConnection()) {
            save(returnObj, conn);
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public Optional<Return> findById(Long id) {
        try (Connection conn = dbManager.getConnection()) {
            return findById(id, conn);
        } catch (SQLException e) { e.printStackTrace(); }
        return Optional.empty();
    }

    // --- Métodos transaccionales ---
    @Override
    public void save(Return returnObj, Connection conn) {
        String returnSql = "INSERT INTO returns (sale_id, return_date, user_id, total_returned) VALUES (?, ?, ?, ?)";
        String detailSql = "INSERT INTO return_details (return_id, product_id, product_name, quantity, price, return_type, reason) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement returnPstmt = conn.prepareStatement(returnSql, Statement.RETURN_GENERATED_KEYS)) {
            returnPstmt.setLong(1, returnObj.saleId());
            returnPstmt.setString(2, returnObj.returnDate().toString());
            returnPstmt.setLong(3, returnObj.userId());
            returnPstmt.setBigDecimal(4, returnObj.totalReturned());
            returnPstmt.executeUpdate();
            
            ResultSet rs = returnPstmt.getGeneratedKeys();
            if (rs.next()) {
                long returnId = rs.getLong(1);
                try (PreparedStatement detailPstmt = conn.prepareStatement(detailSql)) {
                    for (ReturnDetail detail : returnObj.details()) {
                        detailPstmt.setLong(1, returnId);
                        detailPstmt.setLong(2, detail.productId());
                        detailPstmt.setString(3, detail.productName());
                        detailPstmt.setDouble(4, detail.quantity());
                        detailPstmt.setBigDecimal(5, detail.price());
                        detailPstmt.setString(6, detail.returnType().name());
                        detailPstmt.setString(7, detail.reason());
                        detailPstmt.addBatch();
                    }
                    detailPstmt.executeBatch();
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    @Override
    public Optional<Return> findById(Long id, Connection conn) {
        // Implementar si es necesario para futuras funcionalidades
        return Optional.empty();
    }
}
