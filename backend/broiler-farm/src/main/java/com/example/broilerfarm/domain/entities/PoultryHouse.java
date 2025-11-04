package com.example.broilerfarm.domain.entities;

import com.example.broilerfarm.domain.enums.PoultryHouseStatus;
import com.example.broilerfarm.domain.enums.PoultryHouseType;
import com.example.shared.domain.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "poultry_house")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class PoultryHouse extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="farm_id", nullable=false)
    private BroilerFarm farm;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "current_lot")
    private Integer currentLot;

    @Column(name = "area")
    private Double area;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private PoultryHouseType type;

    @Column(name = "equipmentType")
    private String equipmentType;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private PoultryHouseStatus status;

    @Column(name = "current_occupancy")
    private Integer currentOccupancy;

}


//houseNumber: String
//- farm: BroilerFarm (FK)
//- capacity: Integer (max birds)
//- currentLot: ChicksLot (FK)
//- area: Decimal (square meters)
//- type: Enum (OPEN, CLOSED, SEMI_CLOSED)
//- equipmentType: String
//- status: Enum (ACTIVE, MAINTENANCE, EMPTY, OCCUPIED)
//- currentOccupancy: Integer