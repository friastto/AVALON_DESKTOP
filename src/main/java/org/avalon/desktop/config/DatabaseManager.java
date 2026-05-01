package org.avalon.desktop.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_NAME = "avalon.db";
    private final String url;

    public DatabaseManager() {
        String userHome = System.getProperty("user.home");
        Path appDataPath = Paths.get(userHome, ".avalon-desktop");
        try {
            Files.createDirectories(appDataPath);
        } catch (Exception e) {
            logger.error("Could not create app data directory", e);
        }
        this.url = "jdbc:sqlite:" + appDataPath.resolve(DB_NAME).toString();
        initializeDatabase();
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, password TEXT NOT NULL, role TEXT NOT NULL)");
            stmt.execute("INSERT OR IGNORE INTO users (username, password, role) VALUES ('admin', 'admin', 'ADMIN')");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS products (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    price DECIMAL(10,2) NOT NULL,
                    stock INTEGER NOT NULL,
                    barcode TEXT UNIQUE,
                    product_type TEXT NOT NULL DEFAULT 'UNITARIO'
                )
            """);

            // Asegurar columna product_type
            try { stmt.execute("ALTER TABLE products ADD COLUMN product_type TEXT NOT NULL DEFAULT 'UNITARIO'"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE products ADD COLUMN barcode TEXT UNIQUE"); } catch (SQLException ignored) {}

            // Dummy data con un producto pesable
            stmt.execute("INSERT OR IGNORE INTO products (id, name, price, stock, barcode, product_type) VALUES (1, 'Café Premium', 15.50, 100, '7701234567890', 'UNITARIO')");
            stmt.execute("INSERT OR IGNORE INTO products (id, name, price, stock, barcode, product_type) VALUES (2, 'Pan Artesanal', 2.00, 50, '7709876543210', 'UNITARIO')");
            stmt.execute("INSERT OR IGNORE INTO products (id, name, price, stock, barcode, product_type) VALUES (3, 'Manzana Roja', 4.50, 500, '1001', 'PESABLE')");

            stmt.execute("CREATE TABLE IF NOT EXISTS sales (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT NOT NULL, total DECIMAL(10,2) NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS sale_items (id INTEGER PRIMARY KEY AUTOINCREMENT, sale_id INTEGER NOT NULL, product_id INTEGER NOT NULL, quantity DECIMAL(10,3) NOT NULL, price DECIMAL(10,2) NOT NULL, FOREIGN KEY (sale_id) REFERENCES sales(id))");

            logger.info("Database initialized successfully");
        } catch (SQLException e) {
            logger.error("Error initializing database", e);
        }
    }
}
