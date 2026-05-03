package org.avalon.desktop.sales.application.usecase;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.avalon.desktop.product.domain.model.InventoryMovement;
import org.avalon.desktop.product.domain.model.InventoryMovementType;
import org.avalon.desktop.product.domain.model.Product;
import org.avalon.desktop.product.domain.repository.InventoryMovementRepository;
import org.avalon.desktop.product.domain.repository.ProductRepository;
import org.avalon.desktop.sales.domain.model.*;
import org.avalon.desktop.sales.domain.repository.ReturnRepository;
import org.avalon.desktop.sales.domain.repository.SaleRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class RegisterReturnUseCase {

    private final SaleRepository saleRepository;
    private final ReturnRepository returnRepository;
    private final ProductRepository productRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final Provider<Connection> connectionProvider; // Para transacciones

    @Inject
    public RegisterReturnUseCase(SaleRepository saleRepository,
                                 ReturnRepository returnRepository,
                                 ProductRepository productRepository,
                                 InventoryMovementRepository inventoryMovementRepository,
                                 Provider<Connection> connectionProvider) {
        this.saleRepository = saleRepository;
        this.returnRepository = returnRepository;
        this.productRepository = productRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.connectionProvider = connectionProvider;
    }

    public ReturnResult execute(ReturnRequest request) throws Exception {
        Connection conn = connectionProvider.get();
        conn.setAutoCommit(false); // Iniciar transacción

        try {
            // 1. Obtener venta original
            Sale originalSale = saleRepository.findById(request.saleId())
                    .orElseThrow(() -> new IllegalArgumentException("Venta no encontrada: " + request.saleId()));

            // 2. Validar cantidades y construir detalles de devolución
            List<ReturnDetail> returnDetails = request.itemsToReturn().stream()
                    .map(itemRequest -> {
                        SaleItem originalSaleItem = originalSale.items().stream()
                                .filter(si -> si.product().id().equals(itemRequest.productId()))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("Producto " + itemRequest.productId() + " no encontrado en la venta."));

                        // Validar que no se devuelva más de lo vendido
                        if (itemRequest.quantity() > originalSaleItem.quantity()) {
                            throw new IllegalArgumentException("No se puede devolver más cantidad de " + originalSaleItem.product().name() + " (" + itemRequest.quantity() + ") de la que se vendió (" + originalSaleItem.quantity() + ").");
                        }
                        
                        // TODO: Aquí se podría añadir lógica para verificar devoluciones previas
                        // y asegurar que la suma de devoluciones no exceda la cantidad vendida.

                        return new ReturnDetail(
                                itemRequest.productId(),
                                originalSaleItem.product().name(),
                                itemRequest.quantity(),
                                originalSaleItem.price(),
                                itemRequest.returnType(),
                                itemRequest.reason()
                        );
                    })
                    .collect(Collectors.toList());

            // Calcular el total devuelto antes de crear el record Return
            BigDecimal calculatedTotalReturned = returnDetails.stream()
                    .map(detail -> detail.price().multiply(BigDecimal.valueOf(detail.quantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 3. Crear entidad Devolucion con el total ya calculado
            Return newReturn = new Return(
                    null, // ID se generará en la BD
                    request.saleId(),
                    LocalDateTime.now(),
                    request.userId(),
                    calculatedTotalReturned, // Usar el total calculado aquí
                    returnDetails
            );

            // 4. Persistir la devolución
            returnRepository.save(newReturn);

            // 5. Actualizar inventario y registrar movimientos
            for (ReturnDetail detail : newReturn.details()) {
                if (detail.returnType() == ReturnType.REINGRESO_STOCK) {
                    // Para productos pesables, la cantidad puede ser decimal
                    productRepository.updateStock(detail.productId(), detail.quantity());
                    inventoryMovementRepository.save(new InventoryMovement(
                            null,
                            detail.productId(),
                            detail.productName(),
                            detail.quantity(),
                            InventoryMovementType.RETURN,
                            "Devolución Venta #" + request.saleId(),
                            LocalDateTime.now()
                    ));
                }
            }

            // 6. Actualizar estado de la venta original
            SaleStatus newSaleStatus = originalSale.status();
            // Lógica simplificada: si la devolución cubre todos los ítems originales
            // (esto es una simplificación, una lógica real compararía cantidades devueltas vs vendidas)
            if (newReturn.totalReturned().compareTo(originalSale.total()) >= 0) { // Comparar total devuelto con total de venta
                newSaleStatus = SaleStatus.RETURNED;
            } else {
                newSaleStatus = SaleStatus.PARTIALLY_RETURNED;
            }
            saleRepository.updateStatus(originalSale.id(), newSaleStatus);

            conn.commit(); // Confirmar transacción
            return new ReturnResult(newReturn.id(), newReturn.totalReturned(), newSaleStatus, "Devolución registrada con éxito.");

        } catch (Exception e) {
            conn.rollback(); // Revertir transacción en caso de error
            throw e;
        } finally {
            conn.setAutoCommit(true); // Restaurar auto-commit
            conn.close();
        }
    }

    // DTOs para el caso de uso
    public record ReturnRequest(
            Long saleId,
            List<ReturnItemRequest> itemsToReturn,
            Long userId
    ) {}

    public record ReturnItemRequest(
            Long productId,
            Double quantity,
            ReturnType returnType,
            String reason
    ) {}

    public record ReturnResult(
            Long returnId,
            BigDecimal totalReturned,
            SaleStatus newSaleStatus,
            String message
    ) {}
}
