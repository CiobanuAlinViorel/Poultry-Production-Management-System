package com.example.slaughterhouse.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonRootName("chicken_reception")
public class ChickenReceptionDto {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("slaughter_lot_id")
    private Long slaughterLotId;
    
    @JsonProperty("delivery_notice_id")
    private Long deliveryNoticeId;
    
    @JsonProperty("reception_date")
    private LocalDate receptionDate;
    
    @JsonProperty("reception_time")
    private LocalDateTime receptionTime;
    
    @JsonProperty("received_by_id")
    private Long receivedById;
    
    @JsonProperty("quantity_received")
    private Integer quantityReceived;
    
    @JsonProperty("chicks_alive")
    private Integer chicksAlive;
    
    @JsonProperty("chicks_doa")
    private Integer chicksDOA;
    
    @JsonProperty("transport_conditions")
    private String transportConditions;
    
    @JsonProperty("animal_welfare_check")
    private Boolean animalWelfareCheck;
    
    @JsonProperty("animal_welfare_notes")
    private String animalWelfareNotes;
    
    @JsonProperty("notes")
    private String notes;
    
    @JsonProperty("is_active")
    private Boolean isActive;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
