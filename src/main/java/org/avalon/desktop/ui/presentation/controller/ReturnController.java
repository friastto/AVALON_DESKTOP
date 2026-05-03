package org.avalon.desktop.ui.presentation.controller;

import com.google.inject.Inject;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;
import org.avalon.desktop.product.domain.model.Product;
import org.avalon.desktop.sales.application.usecase.RegisterReturnUseCase;
import org.avalon.desktop.sales.domain.model.Sale;
import org.avalon.desktop.sales.domain.model.SaleItem;
import org.avalon.desktop.sales.domain.model.ReturnType;
import org.avalon.desktop.sales.domain.repository.SaleRepository;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class ReturnController implements Initializable {

    @FXML private TextField searchSaleField;
    @FXML private Label lblSaleInfo;
    @FXML private TableView<ReturnableSaleItem> saleItemsTable;
    @FXML private TableColumn<ReturnableSaleItem, Long> colProductId;
    @FXML private TableColumn<ReturnableSaleItem, String> colProductName;
    @FXML private TableColumn<ReturnableSaleItem, Double> colProductQty;
    @FXML private TableColumn<ReturnableSaleItem, BigDecimal> colProductPrice;
    @FXML private TableColumn<ReturnableSaleItem, Double> colReturnQty;
    @FXML private TableColumn<ReturnableSaleItem, ReturnType> colReturnType;
    @FXML private TableColumn<ReturnableSaleItem, String> colReturnReason;

    private final SaleRepository saleRepository;
    private final RegisterReturnUseCase registerReturnUseCase;
    private Sale currentSale;
    private ObservableList<ReturnableSaleItem> returnableItems = FXCollections.observableArrayList();

    @Inject
    public ReturnController(SaleRepository saleRepository, RegisterReturnUseCase registerReturnUseCase) {
        this.saleRepository = saleRepository;
        this.registerReturnUseCase = registerReturnUseCase;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
    }

    private void setupTable() {
        colProductId.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().saleItem().product().id()));
        colProductName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().saleItem().product().name()));
        colProductQty.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().saleItem().quantity()));
        colProductPrice.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().saleItem().price()));
        
        colReturnQty.setCellValueFactory(d -> d.getValue().returnQuantityProperty().asObject());
        colReturnQty.setCellFactory(TextFieldTableCell.forTableColumn(new javafx.util.converter.DoubleStringConverter()));
        colReturnQty.setOnEditCommit(event -> {
            ReturnableSaleItem item = event.getRowValue();
            if (event.getNewValue() != null && event.getNewValue() >= 0 && event.getNewValue() <= item.saleItem().quantity()) {
                item.setReturnQuantity(event.getNewValue());
            } else {
                new Alert(Alert.AlertType.WARNING, "Cantidad a devolver inválida o excede la cantidad vendida.").show();
                saleItemsTable.refresh(); // Refrescar para mostrar el valor original
            }
        });

        colReturnType.setCellValueFactory(d -> d.getValue().returnTypeProperty());
        colReturnType.setCellFactory(ComboBoxTableCell.forTableColumn(ReturnType.values()));
        colReturnType.setOnEditCommit(event -> event.getRowValue().setReturnType(event.getNewValue()));

        colReturnReason.setCellValueFactory(d -> d.getValue().returnReasonProperty());
        colReturnReason.setCellFactory(TextFieldTableCell.forTableColumn());
        colReturnReason.setOnEditCommit(event -> event.getRowValue().setReturnReason(event.getNewValue()));

        saleItemsTable.setEditable(true);
        saleItemsTable.setItems(returnableItems);
    }

    @FXML
    private void searchSale() {
        try {
            Long saleId = Long.parseLong(searchSaleField.getText());
            Optional<Sale> saleOpt = saleRepository.findById(saleId);
            if (saleOpt.isPresent()) {
                currentSale = saleOpt.get();
                lblSaleInfo.setText(String.format("Venta #%d - Total: $%.2f - Estado: %s", 
                                                  currentSale.id(), currentSale.total(), currentSale.status()));
                returnableItems.clear();
                currentSale.items().forEach(item -> returnableItems.add(new ReturnableSaleItem(item)));
            } else {
                lblSaleInfo.setText("Venta no encontrada.");
                returnableItems.clear();
            }
        } catch (NumberFormatException e) {
            lblSaleInfo.setText("ID de venta inválido.");
            returnableItems.clear();
        }
    }

    @FXML
    private void registerReturn() {
        if (currentSale == null) {
            new Alert(Alert.AlertType.WARNING, "Primero debe buscar y seleccionar una venta.").show();
            return;
        }

        List<RegisterReturnUseCase.ReturnItemRequest> itemsToReturn = new ArrayList<>();
        for (ReturnableSaleItem item : returnableItems) {
            if (item.getReturnQuantity() > 0) {
                itemsToReturn.add(new RegisterReturnUseCase.ReturnItemRequest(
                    item.saleItem().product().id(),
                    item.getReturnQuantity(),
                    item.getReturnType(),
                    item.getReturnReason()
                ));
            }
        }

        if (itemsToReturn.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "No se ha especificado ninguna cantidad a devolver.").show();
            return;
        }

        try {
            // TODO: Obtener el ID del usuario actual (simulado como 1L por ahora)
            RegisterReturnUseCase.ReturnResult result = registerReturnUseCase.execute(
                new RegisterReturnUseCase.ReturnRequest(currentSale.id(), itemsToReturn, 1L)
            );
            new Alert(Alert.AlertType.INFORMATION, "Devolución registrada: " + result.message() + "\nNuevo estado de venta: " + result.newSaleStatus()).show();
            searchSale(); // Refrescar la venta para ver el nuevo estado
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Error al registrar devolución: " + e.getMessage()).show();
        }
    }

    // Clase auxiliar para la TableView de devoluciones
    public static class ReturnableSaleItem {
        private final SaleItem saleItem;
        private final SimpleDoubleProperty returnQuantity;
        private final SimpleObjectProperty<ReturnType> returnType;
        private final SimpleStringProperty returnReason;

        public ReturnableSaleItem(SaleItem saleItem) {
            this.saleItem = saleItem;
            this.returnQuantity = new SimpleDoubleProperty(0.0);
            this.returnType = new SimpleObjectProperty<>(ReturnType.REINGRESO_STOCK);
            this.returnReason = new SimpleStringProperty("");
        }

        public SaleItem saleItem() { return saleItem; }
        public double getReturnQuantity() { return returnQuantity.get(); }
        public SimpleDoubleProperty returnQuantityProperty() { return returnQuantity; }
        public void setReturnQuantity(double returnQuantity) { this.returnQuantity.set(returnQuantity); }
        public ReturnType getReturnType() { return returnType.get(); }
        public SimpleObjectProperty<ReturnType> returnTypeProperty() { return returnType; }
        public void setReturnType(ReturnType returnType) { this.returnType.set(returnType); }
        public String getReturnReason() { return returnReason.get(); }
        public SimpleStringProperty returnReasonProperty() { return returnReason; }
        public void setReturnReason(String returnReason) { this.returnReason.set(returnReason); }
    }
}
