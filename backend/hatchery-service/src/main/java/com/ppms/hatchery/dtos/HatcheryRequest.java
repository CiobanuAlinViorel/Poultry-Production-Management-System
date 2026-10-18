package com.ppms.hatchery.dtos;

import jakarta.validation.constraints.NotBlank;

public record HatcheryRequest(
        @NotBlank String code,
        String name,
        String address,
        String county,
        Integer weeklySetCapacity
) {
}
