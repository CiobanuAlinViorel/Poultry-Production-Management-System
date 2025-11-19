package com.example.broilerfarm.services;

import com.example.broilerfarm.domain.entities.*;
import com.example.broilerfarm.domain.enums.ConsumableType;
import com.example.broilerfarm.domain.enums.StockStatus;
import com.example.broilerfarm.domain.enums.UnitOfMeasure;
import com.example.broilerfarm.infrastructure.persistence.repositories.ConsumableRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.ConsumableStockRepository;
import com.example.shared.domain.enums.Role;
import com.example.shared.domain.enums.WarehouseType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@DisplayName("StockManagementService Tests")
class StockManagementServiceTest {

    @Autowired
    private StockManagementService stockManagementService;

    @MockBean
    private ConsumableStockRepository consumableStockRepository;

    @MockBean
    private ConsumableRepository consumableRepository;

    private Consumable consumable;
    private Warehouse warehouse;
    private ConsumableStock stock1;
    private ConsumableStock stock2;
    private BroilerFarm broilerFarm;
    private FarmEmployee farmEmployee;

    @BeforeEach
    void setUp() {
        LocalDateTime dateTime = LocalDateTime.now();

        // Setup BroilerFarm and FarmEmployee
        broilerFarm = new BroilerFarm(1L, dateTime, dateTime, "F1", "Location", "Address", 1000, "L1000");
        farmEmployee = new FarmEmployee(1L, dateTime, dateTime, "Cristi", "Cristian", "0762612341",
                "cristi@example.com", Role.WORKER, LocalDate.now(), broilerFarm);

        // Setup Consumable
        consumable = new Consumable(
                1L,
                 LocalDateTime.now(),
                LocalDateTime.now(),
                "Feed Starter",
                ConsumableType.FEED_STARTER,
                "category 1",
                UnitOfMeasure.KG,
                new BigDecimal("100"),
                new BigDecimal("25.50"),
                "Supplier 1",
                "Must be in cold place",
                10

        );

        // Setup Warehouse
        warehouse = new Warehouse(
                "Main Warehouse",
                "WH001",
                WarehouseType.MAIN_WAREHOUSE,
                new BigDecimal("1000"),
                BigDecimal.ZERO,
                broilerFarm,
                farmEmployee
        );
        warehouse.setId(1L);

        // Setup ConsumableStock instances
        stock1 = new ConsumableStock(
                1L,
                dateTime,
                dateTime,
                consumable,
                warehouse,
                "BATCH001",
                new BigDecimal("300"),
                BigDecimal.ZERO,
                LocalDate.now().minusDays(20),
                LocalDate.now(),
                LocalDate.now().plusDays(60),
                StockStatus.AVAILABLE
        );

        stock2 = new ConsumableStock(
                2L,
                dateTime,
                dateTime,
                consumable,
                warehouse,
                "BATCH002",
                new BigDecimal("200"),
                BigDecimal.ZERO,
                LocalDate.now().minusDays(10),
                LocalDate.now(),
                LocalDate.now().plusDays(90),
                StockStatus.AVAILABLE
        );
    }

    @Test
    @DisplayName("Should check stock availability successfully")
    void testCheckStockAvailability() {
        List<ConsumableStock> availableStocks = Arrays.asList(stock1, stock2);
        when(consumableStockRepository.findAvailableStockFIFO(1L, 1L))
                .thenReturn(availableStocks);
        when(consumableRepository.findById(1L))
                .thenReturn(Optional.of(consumable));

        StockManagementService.StockAvailability availability =
                stockManagementService.checkStockAvailability(1L, 1L);

        assertNotNull(availability);
        assertEquals("Feed Starter", availability.consumableName());
        assertEquals("FEED_STARTER", availability.consumableType());
        assertEquals(0, new BigDecimal("500").compareTo(availability.availableQuantity()));
        assertEquals(0, new BigDecimal("100").compareTo(availability.reorderPoint()));
        assertFalse(availability.needsReorder());
    }

    @Test
    @DisplayName("Should throw exception when consumable not found")
    void testCheckStockAvailabilityConsumableNotFound() {
        when(consumableStockRepository.findAvailableStockFIFO(1L, 1L))
                .thenReturn(Collections.emptyList());
        when(consumableRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> stockManagementService.checkStockAvailability(1L, 1L));
    }

    @Test
    @DisplayName("Should reserve stock successfully from single batch")
    void testReserveStockFromSingleBatch() {
        List<ConsumableStock> availableStocks = Arrays.asList(stock1, stock2);
        when(consumableStockRepository.findAvailableStockFIFO(1L, 1L))
                .thenReturn(availableStocks);

        boolean result = stockManagementService.reserveStockForConsumption(1L, 1L, new BigDecimal("250"));

        assertTrue(result);
        verify(consumableStockRepository, times(1)).save(stock1);
        assertEquals(0, new BigDecimal("250").compareTo(stock1.getReservedQuantity()));
    }

    @Test
    @DisplayName("Should reserve stock from multiple batches (FIFO)")
    void testReserveStockFromMultipleBatches() {
        List<ConsumableStock> availableStocks = Arrays.asList(stock1, stock2);
        when(consumableStockRepository.findAvailableStockFIFO(1L, 1L))
                .thenReturn(availableStocks);

        boolean result = stockManagementService.reserveStockForConsumption(1L, 1L, new BigDecimal("450"));

        assertTrue(result);
        verify(consumableStockRepository, times(2)).save(any(ConsumableStock.class));
        assertEquals(0, new BigDecimal("300").compareTo(stock1.getReservedQuantity()));
        assertEquals(0, new BigDecimal("150").compareTo(stock2.getReservedQuantity()));
    }

    @Test
    @DisplayName("Should return false when insufficient stock to reserve")
    void testReserveStockInsufficientStock() {
        List<ConsumableStock> availableStocks = Arrays.asList(stock1, stock2);
        when(consumableStockRepository.findAvailableStockFIFO(1L, 1L))
                .thenReturn(availableStocks);

        boolean result = stockManagementService.reserveStockForConsumption(1L, 1L, new BigDecimal("600"));

        assertFalse(result);
    }

    @Test
    @DisplayName("Should throw exception when consuming more than reserved")
    void testConsumeReservedStockInsufficientReservation() {
        stock1.reserveStock(new BigDecimal("100"));

        List<ConsumableStock> reservedStocks = Collections.singletonList(stock1);
        when(consumableStockRepository.findByConsumableIdAndWarehouseId(1L, 1L))
                .thenReturn(reservedStocks);

        assertThrows(IllegalStateException.class,
                () -> stockManagementService.consumeReservedStock(1L, 1L, new BigDecimal("200")));
    }

    @Test
    @DisplayName("Should find low stock items")
    void testFindLowStockItems() {
        List<ConsumableStock> lowStockItems = Arrays.asList(stock1);
        when(consumableStockRepository.findStockBelowReorderPoint(1L))
                .thenReturn(lowStockItems);

        List<ConsumableStock> result = stockManagementService.findLowStockItems(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(consumableStockRepository).findStockBelowReorderPoint(1L);
    }

    @Test
    @DisplayName("Should find expiring stock")
    void testFindExpiringStock() {
        stock1.setExpirationDate(LocalDate.now().plusDays(5));
        List<ConsumableStock> expiringStock = Arrays.asList(stock1);

        when(consumableStockRepository.findStockExpiringSoon(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(expiringStock);

        List<ConsumableStock> result = stockManagementService.findExpiringStock(7);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(consumableStockRepository).findStockExpiringSoon(any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("Should update stock after reception")
    void testUpdateStockAfterReception() {
        BigDecimal quantityReceived = new BigDecimal("500");
        LocalDate manufacturingDate = LocalDate.now().minusDays(5);
        LocalDate expirationDate = LocalDate.now().plusDays(180);
        String batchNumber = "BATCH003";

        stockManagementService.updateStockAfterReception(
                consumable, warehouse, batchNumber, quantityReceived,
                manufacturingDate, expirationDate
        );

        verify(consumableStockRepository).save(argThat(stock ->
                stock.getConsumable().equals(consumable) &&
                        stock.getWarehouse().equals(warehouse) &&
                        stock.getBatchNumber().equals(batchNumber) &&
                        stock.getQuantityOnHand().compareTo(quantityReceived) == 0 &&
                        stock.getManufacturingDate().equals(manufacturingDate) &&
                        stock.getExpirationDate().equals(expirationDate)
        ));
    }

    @Test
    @DisplayName("Should handle empty stock list when reserving")
    void testReserveStockWithEmptyList() {
        when(consumableStockRepository.findAvailableStockFIFO(1L, 1L))
                .thenReturn(Collections.emptyList());

        boolean result = stockManagementService.reserveStockForConsumption(1L, 1L, new BigDecimal("100"));

        assertFalse(result);
        verify(consumableStockRepository, never()).save(any());
    }

    @Test
    @DisplayName("StockAvailability DTO should calculate needsReorder correctly")
    void testStockAvailabilityNeedsReorder() {
        StockManagementService.StockAvailability availability1 =
                new StockManagementService.StockAvailability(
                        "Feed", "FEED_STARTER", new BigDecimal("50"),
                        new BigDecimal("100"), LocalDate.now()
                );
        assertTrue(availability1.needsReorder());

        StockManagementService.StockAvailability availability2 =
                new StockManagementService.StockAvailability(
                        "Feed", "FEED_STARTER", new BigDecimal("150"),
                        new BigDecimal("100"), LocalDate.now()
                );
        assertFalse(availability2.needsReorder());
    }

    @Test
    @DisplayName("Should handle stock with no available quantity")
    void testReserveStockWithZeroAvailability() {
        stock1.setQuantityOnHand(new BigDecimal("100"));
        stock1.setReservedQuantity(new BigDecimal("100"));

        List<ConsumableStock> availableStocks = Collections.singletonList(stock1);
        when(consumableStockRepository.findAvailableStockFIFO(1L, 1L))
                .thenReturn(availableStocks);

        boolean result = stockManagementService.reserveStockForConsumption(1L, 1L, new BigDecimal("50"));

        assertFalse(result);
        verify(consumableStockRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should find nearest expiration date in stock availability")
    void testStockAvailabilityNearestExpiration() {
        stock1.setExpirationDate(LocalDate.now().plusDays(30));
        stock2.setExpirationDate(LocalDate.now().plusDays(60));

        List<ConsumableStock> availableStocks = Arrays.asList(stock1, stock2);
        when(consumableStockRepository.findAvailableStockFIFO(1L, 1L))
                .thenReturn(availableStocks);
        when(consumableRepository.findById(1L))
                .thenReturn(Optional.of(consumable));

        StockManagementService.StockAvailability availability =
                stockManagementService.checkStockAvailability(1L, 1L);

        assertEquals(LocalDate.now().plusDays(30), availability.nearestExpirationDate());
    }
}