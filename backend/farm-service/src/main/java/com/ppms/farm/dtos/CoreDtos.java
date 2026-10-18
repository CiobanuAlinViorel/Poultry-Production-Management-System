package com.ppms.farm.dtos;

import com.ppms.farm.domain.Lot;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public final class CoreDtos {
    private CoreDtos() {}

    public record FarmRequest(@NotBlank String code, @NotBlank String name, String address, String county, Boolean active) {}
    public record FarmResponse(UUID id, String code, String name, String address, String county, boolean active, Instant createdAt, Instant updatedAt) {}

    public record HouseRequest(@NotBlank String code, @NotBlank String name, @Positive int capacity, @NotNull @Positive BigDecimal areaM2, Boolean active) {}
    public record HouseResponse(UUID id, UUID farmId, String code, String name, int capacity, BigDecimal areaM2, boolean active) {}

    public record SiloRequest(@NotBlank String code, @NotBlank String name, @NotNull @Positive BigDecimal capacityKg) {}
    public record SiloResponse(UUID id, UUID farmId, String code, String name, BigDecimal capacityKg) {}

    public record LotRequest(@NotNull UUID houseId, @NotBlank String breedCode, UUID chickBatchId, @NotNull @PastOrPresent LocalDate arrivalDate,
                             @PositiveOrZero int femaleCount, @PositiveOrZero int maleCount, LocalDate expectedCatchDate) {}
    public record LotResponse(UUID id, UUID farmId, UUID houseId, String breedCode, UUID chickBatchId, LocalDate arrivalDate,
                              int femaleCount, int maleCount, Lot.Status status, LocalDate expectedCatchDate) {}

    public record DailySheetRequest(@NotNull @PastOrPresent LocalDate date, @PositiveOrZero int mortality,
                                    @PositiveOrZero BigDecimal feedKg, @PositiveOrZero BigDecimal waterLiters,
                                    @Positive BigDecimal avgWeightG, BigDecimal temperatureMin, BigDecimal temperatureMax,
                                    @DecimalMin("0") @DecimalMax("100") BigDecimal humidityPct, String comments) {}
    public record DailySheetResponse(UUID id, UUID lotId, LocalDate date, int mortality, BigDecimal feedKg, BigDecimal waterLiters,
                                     BigDecimal avgWeightG, BigDecimal temperatureMin, BigDecimal temperatureMax, BigDecimal humidityPct, String comments) {}

    public record FarmDeliveryRequest(@NotNull @PastOrPresent LocalDate dispatchDate, @Positive int quantity,
                                      @NotNull @Positive BigDecimal avgWeightKg, String fciRef) {}
    public record FarmDeliveryResponse(UUID id, UUID lotId, LocalDate dispatchDate, int quantity, BigDecimal avgWeightKg, String fciRef) {}
}
