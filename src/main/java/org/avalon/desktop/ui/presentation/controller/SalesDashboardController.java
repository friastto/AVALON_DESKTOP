package org.avalon.desktop.ui.presentation.controller;

import com.google.inject.Inject;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.util.Duration;
import org.avalon.desktop.sales.application.service.SalesReportService;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.ResourceBundle;

public class SalesDashboardController implements Initializable {

    @FXML private Label lblTotalSales, lblSalesCount, lblAverageSale;
    @FXML private LineChart<String, Number> salesChart;
    @FXML private TableView<Map.Entry<String, Double>> topProductsTable;
    @FXML private TableColumn<Map.Entry<String, Double>, String> colProdName;
    @FXML private TableColumn<Map.Entry<String, Double>, Double> colProdQty;
    
    @FXML private ToggleButton btnToday, btnWeek, btnMonth;
    private ToggleGroup filterGroup;

    private final SalesReportService salesReportService;
    private Timeline autoRefresh;

    @Inject
    public SalesDashboardController(SalesReportService salesReportService) {
        this.salesReportService = salesReportService;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupTable();
        setupFilters();
        refreshData();
        startAutoRefresh();
    }

    private void setupTable() {
        colProdName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getKey()));
        colProdQty.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getValue()).asObject());
    }

    private void setupFilters() {
        filterGroup = new ToggleGroup();
        btnToday.setToggleGroup(filterGroup);
        btnWeek.setToggleGroup(filterGroup);
        btnMonth.setToggleGroup(filterGroup);
        
        filterGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) refreshData();
            else if (oldVal != null) oldVal.setSelected(true);
        });
    }

    private void refreshData() {
        LocalDateTime start = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime end = LocalDateTime.now().with(LocalTime.MAX);

        if (btnWeek.isSelected()) start = LocalDate.now().minusWeeks(1).atStartOfDay();
        else if (btnMonth.isSelected()) start = LocalDate.now().minusMonths(1).atStartOfDay();

        SalesReportService.SalesReportData data = salesReportService.getReport(start, end);
        updateUI(data);
    }

    private void updateUI(SalesReportService.SalesReportData data) {
        lblTotalSales.setText("$" + data.totalSales().toString());
        lblSalesCount.setText(String.valueOf(data.salesCount()));
        lblAverageSale.setText("$" + data.averageSale().toString());

        salesChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        data.salesOverTime().forEach((k, v) -> series.getData().add(new XYChart.Data<>(k, v)));
        salesChart.getData().add(series);

        topProductsTable.setItems(FXCollections.observableArrayList(data.topProducts().entrySet()));
    }

    private void startAutoRefresh() {
        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(10), e -> refreshData()));
        autoRefresh.setCycleCount(Timeline.INDEFINITE);
        autoRefresh.play();
    }
}
