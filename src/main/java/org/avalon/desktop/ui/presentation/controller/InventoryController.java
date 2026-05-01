package org.avalon.desktop.ui.presentation.controller;

import com.google.inject.Inject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.avalon.desktop.product.application.service.InventoryService;
import org.avalon.desktop.product.domain.model.Product;
import org.avalon.desktop.product.domain.model.ProductType;
import org.kordamp.ikonli.javafx.FontIcon;

import java.math.BigDecimal;
import java.net.URL;
import java.util.ResourceBundle;

public class InventoryController implements Initializable {

    @FXML private Label lblTotal, lblLow, lblOut, lblValue;
    @FXML private TextField searchField;
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Long> colId;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, BigDecimal> colPrice;
    @FXML private TableColumn<Product, Integer> colStock;
    @FXML private TableColumn<Product, String> colStatus;
    @FXML private TableColumn<Product, Void> colActions;

    // Modal
    @FXML private VBox modalOverlay;
    @FXML private Label modalTitle;
    @FXML private TextField fieldName, fieldBarcode, fieldPrice, fieldStock;
    @FXML private ComboBox<ProductType> comboType;

    private final InventoryService inventoryService;
    private final ObservableList<Product> masterData = FXCollections.observableArrayList();
    private Product selectedProduct = null;

    @Inject
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        setupForm();
        refreshData();

        searchField.textProperty().addListener((obs, old, newVal) -> {
            productTable.setItems(masterData.filtered(p -> 
                p.name().toLowerCase().contains(newVal.toLowerCase())));
        });
    }

    private void setupForm() {
        comboType.setItems(FXCollections.observableArrayList(ProductType.values()));
        comboType.setValue(ProductType.UNITARIO);
    }

    private void setupTable() {
        colId.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().id()));
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name() + (d.getValue().productType() == ProductType.PESABLE ? " (KG)" : "")));
        colPrice.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().price()));
        colStock.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().stock()));
        
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("status-critical", "status-low", "status-normal");
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Product p = getTableRow().getItem();
                    if (p.stock() <= 0) {
                        setText("🔴 CRÍTICO");
                        getStyleClass().add("status-critical");
                    } else if (p.stock() <= 5) {
                        setText("🟡 BAJO");
                        getStyleClass().add("status-low");
                    } else {
                        setText("🟢 NORMAL");
                        getStyleClass().add("status-normal");
                    }
                }
            }
        });

        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();
            private final HBox container = new HBox(10, editBtn, deleteBtn);
            {
                editBtn.setGraphic(new FontIcon("mdi2p-pencil"));
                deleteBtn.setGraphic(new FontIcon("mdi2t-trash-can"));
                editBtn.getStyleClass().add("action-button");
                deleteBtn.getStyleClass().add("action-button");
                editBtn.setOnAction(e -> showEditProduct(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> {
                    inventoryService.deleteProduct(getTableView().getItems().get(getIndex()).id());
                    refreshData();
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
        productTable.setItems(masterData);
    }

    @FXML public void refreshData() {
        masterData.setAll(inventoryService.getAllProducts());
        InventoryService.InventoryStats stats = inventoryService.getStats();
        lblTotal.setText(String.valueOf(stats.totalProducts()));
        lblLow.setText(String.valueOf(stats.lowStock()));
        lblOut.setText(String.valueOf(stats.outOfStock()));
        lblValue.setText("$" + stats.totalValue().toString());
    }

    @FXML private void showAddProduct() {
        selectedProduct = null;
        modalTitle.setText("NUEVO PRODUCTO");
        fieldName.clear(); fieldBarcode.clear(); fieldPrice.clear(); fieldStock.clear();
        comboType.setValue(ProductType.UNITARIO);
        modalOverlay.setVisible(true);
    }

    private void showEditProduct(Product p) {
        selectedProduct = p;
        modalTitle.setText("EDITAR PRODUCTO");
        fieldName.setText(p.name());
        fieldBarcode.setText(p.barcode());
        fieldPrice.setText(p.price().toString());
        fieldStock.setText(p.stock().toString());
        comboType.setValue(p.productType());
        modalOverlay.setVisible(true);
    }

    @FXML private void hideModal() { modalOverlay.setVisible(false); }

    @FXML
    private void saveProduct() {
        try {
            Product p = new Product(
                selectedProduct != null ? selectedProduct.id() : null,
                fieldName.getText(),
                new BigDecimal(fieldPrice.getText()),
                Integer.parseInt(fieldStock.getText()),
                fieldBarcode.getText(),
                comboType.getValue()
            );

            if (selectedProduct == null) inventoryService.addProduct(p);
            else inventoryService.updateProduct(p);
            
            hideModal();
            refreshData();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Datos inválidos").show();
        }
    }
}
