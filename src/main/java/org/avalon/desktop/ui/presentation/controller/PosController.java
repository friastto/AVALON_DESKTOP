package org.avalon.desktop.ui.presentation.controller;

import com.google.inject.Inject;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import org.avalon.desktop.core.devices.DeviceManager;
import org.avalon.desktop.product.domain.model.Product;
import org.avalon.desktop.product.domain.model.ProductType;
import org.avalon.desktop.product.domain.repository.ProductRepository;
import org.avalon.desktop.sales.domain.model.Sale;
import org.avalon.desktop.sales.domain.model.SaleItem;
import org.avalon.desktop.sales.domain.repository.SaleRepository;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;

public class PosController implements Initializable {

    @FXML private TextField scannerField, quantityField, searchField;
    @FXML private Label totalLabel, lblLiveWeight;

    @FXML private TableView<SaleItem> cartTable;
    @FXML private TableColumn<SaleItem, String> colCartProduct;
    @FXML private TableColumn<SaleItem, BigDecimal> colCartPrice;
    @FXML private TableColumn<SaleItem, Double> colCartQty;
    @FXML private TableColumn<SaleItem, BigDecimal> colCartTotal;

    @FXML private TableView<Product> productTableView;
    @FXML private TableColumn<Product, String> colProdName;
    @FXML private TableColumn<Product, BigDecimal> colProdPrice;
    @FXML private TableColumn<Product, Integer> colProdStock;

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final DeviceManager deviceManager;
    private final ObservableList<SaleItem> cartItems = FXCollections.observableArrayList();
    private final ObservableList<Product> availableProducts = FXCollections.observableArrayList();

    @Inject
    public PosController(ProductRepository productRepository, SaleRepository saleRepository, DeviceManager deviceManager) {
        this.productRepository = productRepository;
        this.saleRepository = saleRepository;
        this.deviceManager = deviceManager;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTables();
        refreshProductList();
        
        // Bind peso en vivo
        lblLiveWeight.textProperty().bind(deviceManager.getScaleService().weightStrProperty());

        Platform.runLater(() -> scannerField.requestFocus());

        searchField.textProperty().addListener((obs, old, newVal) -> 
            availableProducts.setAll(productRepository.findByName(newVal)));

        productTableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Product p = productTableView.getSelectionModel().getSelectedItem();
                if (p != null) processAddToCart(p);
            }
        });
    }

    private void setupTables() {
        colCartProduct.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().product().name()));
        colCartPrice.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().price()));
        colCartQty.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().quantity()));
        colCartTotal.setCellValueFactory(d -> {
            BigDecimal total = d.getValue().price().multiply(BigDecimal.valueOf(d.getValue().quantity()));
            return new SimpleObjectProperty<>(total);
        });
        cartTable.setItems(cartItems);

        colProdName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().name()));
        colProdPrice.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().price()));
        colProdStock.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().stock()));
        productTableView.setItems(availableProducts);
    }

    @FXML
    private void handleScan() {
        String code = scannerField.getText().trim();
        if (code.isEmpty()) return;

        Optional<Product> productOpt = productRepository.findByBarcode(code);
        if (productOpt.isPresent()) {
            processAddToCart(productOpt.get());
            scannerField.clear();
        } else {
            new Alert(Alert.AlertType.ERROR, "Producto no encontrado").show();
        }
    }

    private void processAddToCart(Product product) {
        double qtyToAdd;

        if (product.productType() == ProductType.PESABLE) {
            qtyToAdd = deviceManager.getScaleService().getCurrentWeight();
            if (qtyToAdd <= 0) {
                new Alert(Alert.AlertType.WARNING, "Coloque el producto en la báscula (Peso 0)").show();
                return;
            }
        } else {
            qtyToAdd = getSelectedQuantity();
        }

        addToCart(product, qtyToAdd);
    }

    private void addToCart(Product product, double qty) {
        // Lógica de agregado (si ya existe, sumar si es unitario)
        boolean exists = false;
        if (product.productType() == ProductType.UNITARIO) {
            for (int i = 0; i < cartItems.size(); i++) {
                if (cartItems.get(i).product().id().equals(product.id())) {
                    double newQty = cartItems.get(i).quantity() + qty;
                    cartItems.set(i, new SaleItem(product, newQty, product.price()));
                    exists = true;
                    break;
                }
            }
        }

        if (!exists) {
            cartItems.add(new SaleItem(product, qty, product.price()));
        }

        updateTotal();
    }

    private double getSelectedQuantity() {
        try { return Double.parseDouble(quantityField.getText()); } catch (Exception e) { return 1.0; }
    }

    private void updateTotal() {
        BigDecimal total = cartItems.stream()
            .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        totalLabel.setText("$" + total.toString());
    }

    private void refreshProductList() { availableProducts.setAll(productRepository.findAll()); }

    @FXML
    private void handleCheckout() {
        if (cartItems.isEmpty()) return;
        BigDecimal total = cartItems.stream()
            .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Sale sale = new Sale(null, LocalDateTime.now(), total, new ArrayList<>(cartItems));
        saleRepository.save(sale);
        
        for (SaleItem item : cartItems) {
            productRepository.updateStock(item.product().id(), (int) -Math.ceil(item.quantity()));
        }

        cartItems.clear();
        updateTotal();
        refreshProductList();
        new Alert(Alert.AlertType.INFORMATION, "Venta Completada").show();
    }
}
