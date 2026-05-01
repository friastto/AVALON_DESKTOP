package org.avalon.desktop.config;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import org.avalon.desktop.auth.domain.repository.UserRepository;
import org.avalon.desktop.auth.infrastructure.persistence.sqlite.SqliteUserRepository;
import org.avalon.desktop.core.devices.DeviceManager;
import org.avalon.desktop.core.devices.printer.PrinterService;
import org.avalon.desktop.core.devices.scale.ScaleService;
import org.avalon.desktop.core.update.DownloadService;
import org.avalon.desktop.core.update.UpdateService;
import org.avalon.desktop.core.update.UpdaterLauncher;
import org.avalon.desktop.product.application.service.InventoryService;
import org.avalon.desktop.product.domain.repository.ProductRepository;
import org.avalon.desktop.product.infrastructure.persistence.sqlite.SqliteProductRepository;
import org.avalon.desktop.sales.application.service.SalesReportService;
import org.avalon.desktop.sales.domain.repository.SaleRepository;
import org.avalon.desktop.sales.infrastructure.persistence.sqlite.SqliteSaleRepository;
import org.avalon.desktop.ui.navigation.ViewLoader;

public class AppModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(DatabaseManager.class).in(Scopes.SINGLETON);
        bind(UserRepository.class).to(SqliteUserRepository.class).in(Scopes.SINGLETON);
        bind(ProductRepository.class).to(SqliteProductRepository.class).in(Scopes.SINGLETON);
        bind(SaleRepository.class).to(SqliteSaleRepository.class).in(Scopes.SINGLETON);
        bind(ViewLoader.class).in(Scopes.SINGLETON);
        
        // Servicios de Dispositivos
        bind(PrinterService.class).in(Scopes.SINGLETON);
        bind(ScaleService.class).in(Scopes.SINGLETON);
        bind(DeviceManager.class).in(Scopes.SINGLETON);
        
        // Servicios de Actualización
        bind(DownloadService.class).in(Scopes.SINGLETON);
        bind(UpdaterLauncher.class).in(Scopes.SINGLETON);
        bind(UpdateService.class).in(Scopes.SINGLETON);
        
        bind(SalesReportService.class).in(Scopes.SINGLETON);
        bind(InventoryService.class).in(Scopes.SINGLETON);
    }
}
