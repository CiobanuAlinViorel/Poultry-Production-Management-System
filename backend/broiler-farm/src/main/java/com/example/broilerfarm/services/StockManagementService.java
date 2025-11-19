package com.example.broilerfarm.services;

import com.example.broilerfarm.domain.entities.Consumable;
import com.example.broilerfarm.domain.entities.ConsumableStock;
import com.example.broilerfarm.domain.entities.Warehouse;
import com.example.broilerfarm.infrastructure.persistence.repositories.ConsumableRepository;
import com.example.broilerfarm.infrastructure.persistence.repositories.ConsumableStockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Domain Service pentru gestionarea stocurilor
 * UC-09: Create the Reception of Consumables
 */
@Service
@RequiredArgsConstructor
public class StockManagementService {

    private final ConsumableStockRepository consumableStockRepository;
    private final ConsumableRepository consumableRepository;

    /**
     * Verifică disponibilitatea stocului pentru un consumable
     */
    public StockAvailability checkStockAvailability(Long consumableId, Long warehouseId) {
        List<ConsumableStock> availableStocks = consumableStockRepository
                .findAvailableStockFIFO(consumableId, warehouseId);

        BigDecimal totalAvailable = availableStocks.stream()
                .map(ConsumableStock::getAvailableQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Consumable consumable = consumableRepository.findById(consumableId)
                .orElseThrow(() -> new IllegalArgumentException("Consumable not found"));

        LocalDate nearestExpiration = availableStocks.stream()
                .map(ConsumableStock::getExpirationDate)
                .filter(date -> date != null && date.isAfter(LocalDate.now()))
                .min(LocalDate::compareTo)
                .orElse(null);

        return new StockAvailability(
                consumable.getName(),
                consumable.getType().name(),
                totalAvailable,
                consumable.getReorderPoint(),
                nearestExpiration
        );
    }

    /**
     * Rezervă stoc pentru consumție
     */
    public boolean reserveStockForConsumption(Long consumableId, Long warehouseId, BigDecimal quantity) {
        List<ConsumableStock> availableStocks = consumableStockRepository
                .findAvailableStockFIFO(consumableId, warehouseId);

        BigDecimal remainingToReserve = quantity;

        for (ConsumableStock stock : availableStocks) {
            BigDecimal availableInThisBatch = stock.getAvailableQuantity();

            if (availableInThisBatch.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal reserveFromThisBatch = availableInThisBatch.min(remainingToReserve);
                stock.reserveStock(reserveFromThisBatch);
                consumableStockRepository.save(stock);

                remainingToReserve = remainingToReserve.subtract(reserveFromThisBatch);

                if (remainingToReserve.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
            }
        }

        return remainingToReserve.compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * Consumă stoc rezervat
     */
    public void consumeReservedStock(Long consumableId, Long warehouseId, BigDecimal quantity) {
        List<ConsumableStock> reservedStocks = consumableStockRepository
                .findByConsumableIdAndWarehouseId(consumableId, warehouseId);

        BigDecimal remainingToConsume = quantity;

        for (ConsumableStock stock : reservedStocks) {
            if (stock.getReservedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal consumeFromThisBatch = stock.getReservedQuantity().min(remainingToConsume);
                stock.consumeStock(consumeFromThisBatch);
                consumableStockRepository.save(stock);

                remainingToConsume = remainingToConsume.subtract(consumeFromThisBatch);

                if (remainingToConsume.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
            }
        }

        if (remainingToConsume.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException("Could not consume all reserved stock");
        }
    }

    /**
     * Găsește stocuri sub punctul de re-comandă
     */
    public List<ConsumableStock> findLowStockItems(Long farmId) {
        return consumableStockRepository.findStockBelowReorderPoint(farmId);
    }

    /**
     * Găsește stocuri expirate sau care expiră în curând
     */
    public List<ConsumableStock> findExpiringStock(int daysThreshold) {
        LocalDate thresholdDate = LocalDate.now().plusDays(daysThreshold);
        return consumableStockRepository.findStockExpiringSoon(LocalDate.now(), thresholdDate);
    }

    /**
     * Actualizează stoc după recepție
     */
    public void updateStockAfterReception(Consumable consumable, Warehouse warehouse,
                                          String batchNumber, BigDecimal quantityReceived,
                                          LocalDate manufacturingDate, LocalDate expirationDate) {

        ConsumableStock newStock = ConsumableStock.builder()
                .consumable(consumable)
                .warehouse(warehouse)
                .batchNumber(batchNumber)
                .quantityOnHand(quantityReceived)
                .manufacturingDate(manufacturingDate)
                .expirationDate(expirationDate)
                .lastRestockDate(LocalDate.now())
                .build();

        consumableStockRepository.save(newStock);
    }

    // DTO pentru disponibilitatea stocului
    public record StockAvailability(
            String consumableName,
            String consumableType,
            BigDecimal availableQuantity,
            BigDecimal reorderPoint,
            LocalDate nearestExpirationDate
    ) {
        public boolean needsReorder() {
            return availableQuantity.compareTo(reorderPoint) < 0;
        }
    }
}