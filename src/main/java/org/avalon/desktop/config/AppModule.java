package org.avalon.desktop.config;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Scopes;
import org.avalon.desktop.auth.domain.repository.UserRepository;
import org.avalon.desktop.auth.domain.service.PasswordHasher; // Import PasswordHasher
import org.avalon.desktop.auth.infrastructure.persistence.sqlite.SqliteUserRepository;
import org.avalon.desktop.auth.infrastructure.security.DefaultPasswordHasher; // Import DefaultPasswordHasher
import org.avalon.desktop.core.devices.DeviceManager;
import org.avalon.desktop.core.devices.printer.PrinterService;
import org.avalon.desktop.core.devices.scale.ScaleService;
import org.avalon.desktop.core.update.UpdateService;
import org.avalon.desktop.product.application.service.InventoryService;
import org.avalon.desktop.product.domain.repository.InventoryMovementRepository;
import org.avalon.desktop.product.domain.repository.ProductRepository;
import org.avalon.desktop.product.infrastructure.persistence.sqlite.SqliteInventoryMovementRepository;
import org.avalon.desktop.product.infrastructure.persistence.sqlite.SqliteProductRepository;
import org.avalon.desktop.sales.application.service.SalesReportService;
import org.avalon.desktop.sales.application.usecase.RegisterReturnUseCase;
import org.avalon.desktop.sales.domain.repository.ReturnRepository;
import org.avalon.desktop.sales.domain.repository.SaleRepository;
import org.avalon.desktop.sales.infrastructure.persistence.sqlite.SqliteReturnRepository;
import org.avalon.desktop.sales.infrastructure.persistence.sqlite.SqliteSaleRepository;
import org.avalon.desktop.ui.navigation.ViewLoader;
import org.avalon.desktop.ui.SceneManager; // Import SceneManager

import java.sql.Connection;
import java.sql.SQLException;

public class AppModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(DatabaseManager.class).in(Scopes.SINGLETON);
        
        // Proveedor de conexión para transacciones (ahora como clase estática anidada)
        bind(Connection.class).toProvider(ConnectionProvider.class).in(Scopes.SINGLETON);

        // Repositorios
        bind(UserRepository.class).to(SqliteUserRepository.class).in(Scopes.SINGLETON);
        bind(ProductRepository.class).to(SqliteProductRepository.class).in(Scopes.SINGLETON);
        bind(SaleRepository.class).to(SqliteSaleRepository.class).in(Scopes.SINGLETON);
        bind(ReturnRepository.class).to(SqliteReturnRepository.class).in(Scopes.SINGLETON);
        bind(InventoryMovementRepository.class).to(SqliteInventoryMovementRepository.class).in(Scopes.SINGLETON);

        // Servicios de Aplicación
        bind(ViewLoader.class).in(Scopes.SINGLETON);
        bind(SalesReportService.class).in(Scopes.SINGLETON);
        bind(InventoryService.class).in(Scopes.SINGLETON);
        bind(UpdateService.class).in(Scopes.SINGLETON);
        bind(SceneManager.class).in(Scopes.SINGLETON); // Bind SceneManager

        // Seguridad
        bind(PasswordHasher.class).to(DefaultPasswordHasher.class).in(Scopes.SINGLETON); // Bind PasswordHasher

        // Servicios de Dispositivos
        bind(PrinterService.class).in(Scopes.SINGLETON);
        bind(ScaleService.class).in(Scopes.SINGLETON);
        bind(DeviceManager.class).in(Scopes.SINGLETON);

        // Casos de Uso
        bind(RegisterReturnUseCase.class).in(Scopes.SINGLETON);
    }

    // Clase estática anidada para proveer conexiones a la base de datos
    private static class ConnectionProvider implements Provider<Connection> {
        @Inject DatabaseManager dbManager; // Guice inyectará DatabaseManager aquí

        @Override
        public Connection get() {
            try {
                return dbManager.getConnection();
            } catch (SQLException e) {
                throw new RuntimeException("Error al obtener conexión a la base de datos", e);
            }
        }
    }
}
