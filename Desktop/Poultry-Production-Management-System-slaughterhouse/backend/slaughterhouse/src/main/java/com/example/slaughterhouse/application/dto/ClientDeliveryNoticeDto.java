package com.example.slaughterhouse.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for ClientDeliveryNotice entity
 * Represents delivery notice sent to clients
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientDeliveryNoticeDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("notice_number")
    private String noticeNumber;

    @JsonProperty("client_name")
    private String clientName;

    @JsonProperty("client_address")
    private String clientAddress;

    @JsonProperty("client_contact")
    private String clientContact;

    @JsonProperty("delivery_date")
    private LocalDate deliveryDate;

    @JsonProperty("total_packages")
    private Integer totalPackages;

    @JsonProperty("total_weight_value")
    private Double totalWeightValue;

    @JsonProperty("total_weight_unit")
    private String totalWeightUnit;

    @JsonProperty("transport_id")
    private Long transportId;

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