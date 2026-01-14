package com.example.slaughterhouse.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Transport entity
 * Represents transport of packages to clients
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransportDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("transport_number")
    private String transportNumber;

    @JsonProperty("client_delivery_notice_id")
    private Long clientDeliveryNoticeId;

    @JsonProperty("package_ids")
    private List<Long> packageIds;

    @JsonProperty("transport_date")
    private LocalDate transportDate;

    @JsonProperty("vehicle_plate")
    private String vehiclePlate;

    @JsonProperty("driver_name")
    private String driverName;

    @JsonProperty("driver_contact")
    private String driverContact;

    @JsonProperty("departure_time")
    private LocalDateTime departureTime;

    @JsonProperty("estimated_arrival_time")
    private LocalDateTime estimatedArrivalTime;

    @JsonProperty("actual_arrival_time")
    private LocalDateTime actualArrivalTime;

    @JsonProperty("destination")
    private String destination;

    @JsonProperty("status")
    private String status;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("created_by_id")
    private Long createdById;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("updated_by_id")
    private Long updatedById;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("version")
    private Integer version;
}