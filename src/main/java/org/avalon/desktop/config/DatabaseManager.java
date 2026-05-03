package org.avalon.desktop.config;

import com.google.inject.Inject;
import org.avalon.desktop.auth.domain.service.PasswordHasher;
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
    private final PasswordHasher passwordHasher; // Inyectar PasswordHasher

    @Inject // Constructor para inyección de dependencias
    public DatabaseManager(PasswordHasher passwordHasher) {
        this.passwordHasher = passwordHasher;
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
            // Users
            // Modificado: Añadir la columna is_first_login con un valor por defecto TRUE
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, password TEXT NOT NULL, role TEXT NOT NULL, is_first_login BOOLEAN DEFAULT TRUE NOT NULL)");
            
            // Modificado: Añadir la columna is_first_login a bases de datos existentes
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN is_first_login BOOLEAN DEFAULT TRUE NOT NULL");
            } catch (SQLException e) {
                // Ignorar si la columna ya existe (SQLITE_ERROR: duplicate column name)
                // O si la tabla no existe (aunque CREATE TABLE IF NOT EXISTS debería evitar esto)
                if (!e.getMessage().contains("duplicate column name")) {
                    logger.warn("Could not add is_first_login column to users table (might already exist or other issue): {}", e.getMessage());
                }
            }

            // Modificado: Insertar el usuario admin con la contraseña hasheada y is_first_login = TRUE
            String hashedPassword = passwordHasher.hashPassword("admin");
            // Usamos PreparedStatement para evitar inyección SQL y manejar mejor los valores
            String insertAdminSql = "INSERT OR IGNORE INTO users (username, password, role, is_first_login) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertAdminSql)) {
                pstmt.setString(1, "admin");
                pstmt.setString(2, hashedPassword);
                pstmt.setString(3, "ADMIN");
                pstmt.setBoolean(4, true);
                pstmt.executeUpdate();
            }


            // Products
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
            try { stmt.execute("ALTER TABLE products ADD COLUMN product_type TEXT NOT NULL DEFAULT 'UNITARIO'"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE products ADD COLUMN barcode TEXT UNIQUE"); } catch (SQLException ignored) {}

            // Sales
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sales (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    date TEXT NOT NULL,
                    total DECIMAL(10,2) NOT NULL,
                    status TEXT NOT NULL DEFAULT 'COMPLETED'
                )
            """);
            try { stmt.execute("ALTER TABLE sales ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETED'"); } catch (SQLException ignored) {}

            // Sale Items
            stmt.execute("CREATE TABLE IF NOT EXISTS sale_items (id INTEGER PRIMARY KEY AUTOINCREMENT, sale_id INTEGER NOT NULL, product_id INTEGER NOT NULL, quantity DECIMAL(10,3) NOT NULL, price DECIMAL(10,2) NOT NULL, FOREIGN KEY (sale_id) REFERENCES sales(id))");

            // Returns
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS returns (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sale_id INTEGER NOT NULL,
                    return_date TEXT NOT NULL,
                    user_id INTEGER NOT NULL,
                    total_returned DECIMAL(10,2) NOT NULL,
                    FOREIGN KEY (sale_id) REFERENCES sales(id)
                )
            """);

            // Return Details
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS return_details (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    return_id INTEGER NOT NULL,
                    product_id INTEGER NOT NULL,
                    product_name TEXT NOT NULL,
                    quantity DECIMAL(10,3) NOT NULL,
                    price DECIMAL(10,2) NOT NULL,
                    return_type TEXT NOT NULL,
                    reason TEXT,
                    FOREIGN KEY (return_id) REFERENCES returns(id)
                )
            """);

            // Inventory Movements
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS inventory_movements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    product_id INTEGER NOT NULL,
                    product_name TEXT NOT NULL,
                    quantity_change DECIMAL(10,3) NOT NULL,
                    type TEXT NOT NULL,
                    reference TEXT,
                    timestamp TEXT NOT NULL,
                    FOREIGN KEY (product_id) REFERENCES products(id)
                )
            """);

            // Dummy data con un producto pesable
            stmt.execute("INSERT OR IGNORE INTO products (id, name, price, stock, barcode, product_type) VALUES (1, 'Café Premium', 15.50, 100, '7701234567890', 'UNITARIO')");
            stmt.execute("INSERT OR IGNORE INTO products (id, name, price, stock, barcode, product_type) VALUES (2, 'Pan Artesanal', 2.00, 50, '7709876543210', 'UNITARIO')");
            stmt.execute("INSERT OR IGNORE INTO products (id, name, price, stock, barcode, product_type) VALUES (3, 'Manzana Roja', 4.50, 500, '1001', 'PESABLE')");

            logger.info("Database initialized successfully");
        } catch (SQLException e) {
            logger.error("Error initializing database", e);
        }
    }
}
