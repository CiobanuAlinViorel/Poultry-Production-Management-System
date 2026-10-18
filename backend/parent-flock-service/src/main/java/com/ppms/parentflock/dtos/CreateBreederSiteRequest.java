package com.ppms.parentflock.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateBreederSiteRequest(
        @NotBlank String code,
        @NotBlank String name,
        String address,
        String county,
        @NotNull @PositiveOrZero Integer capacityHouses
) {
}
