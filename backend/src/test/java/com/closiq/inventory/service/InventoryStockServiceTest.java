package com.closiq.inventory.service;

import com.closiq.config.ClosiqProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryStockServiceTest {

    @Mock
    private com.closiq.inventory.repository.InventoryItemRepository inventoryItemRepository;

    private InventoryStockService stockService;

    @BeforeEach
    void setUp() {
        ClosiqProperties properties = new ClosiqProperties();
        properties.getInventory().setLowStockThreshold(2);
        stockService = new InventoryStockService(inventoryItemRepository, properties);
    }

    @Test
    void isLowStock_trueWhenAtOrBelowThreshold() {
        assertThat(stockService.isLowStock(2)).isTrue();
        assertThat(stockService.isLowStock(1)).isTrue();
    }

    @Test
    void isLowStock_falseWhenAboveThresholdOrZero() {
        assertThat(stockService.isLowStock(3)).isFalse();
        assertThat(stockService.isLowStock(0)).isFalse();
    }
}
