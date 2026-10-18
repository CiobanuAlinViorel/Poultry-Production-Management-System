package com.ppms.slaughterhouse.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public class CoreDtos {

    public record SlaughterhouseRequest(@NotBlank String code, String name, String address, String county, Integer dailyCapacityBirds) {
    }

    public record LineRequest(@NotNull UUID slaughterhouseId, @NotBlank String code, String name, Integer speedBph) {
    }

    public record SlaughterSessionRequest(@NotNull UUID slaughterhouseId, UUID lineId, @NotBlank String sessionCode, Instant startedAt, Instant expectedEndAt, Integer birdsPlanned) {
    }

    public record CustomerRequest(@NotBlank String code, String name, String vatNo, String address) {
    }

    public record SalesOrderRequest(@NotBlank String orderNo, @NotNull UUID customerId, Instant orderDate, Instant requestedDate) {
    }

    public record DispatchOrderRequest(@NotBlank String orderNo, UUID salesOrderId, @NotNull UUID customerId, Instant dispatchDate, Integer quantity) {
    }
}
