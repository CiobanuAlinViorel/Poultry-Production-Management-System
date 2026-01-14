package com.example.slaughterhouse.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryNoticeDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("external_system_id")
    private String externalSystemId;

    @JsonProperty("source_system")
    private String sourceSystem;

    @JsonProperty("farm_origin")
    private String farmOrigin;

    @JsonProperty("lot_number_from_farm")
    private String lotNumberFromFarm;

    @JsonProperty("scheduled_delivery_date")
    private LocalDate scheduledDeliveryDate;

    @JsonProperty("estimated_quantity")
    private Integer estimatedQuantity;

    @JsonProperty("average_weight")
    private Float averageWeight;

    @JsonProperty("breed")
    private String breed;

    @JsonProperty("average_age_in_days")
    private Integer averageAgeInDays;

    @JsonProperty("transport_details")
    private String transportDetails;

    @JsonProperty("vehicle_plate")
    private String vehiclePlate;

    @JsonProperty("driver_info")
    private String driverInfo;

    @JsonProperty("received_date")
    private LocalDate receivedDate;

    @JsonProperty("received_by_id")
    private Long receivedById;

    @JsonProperty("received_by_name")
    private String receivedByName;

    @JsonProperty("reception_status")
    private String receptionStatus;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("chicken_reception_id")
    private Long chickenReceptionId;

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