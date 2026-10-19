package com.ppms.slaughterhouse.domain;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "dispatch_order", schema = "sl")
public class DispatchOrder extends BaseEntity {

    @Column(name = "order_no", nullable = false, unique = true)
    private String orderNo;

    @Column(name = "sales_order_id")
    private UUID salesOrderId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "dispatch_date")
    private Instant dispatchDate;

    @Column(name = "quantity")
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.PLANNED;

    public enum Status { PLANNED, PICKED, DISPATCHED, DELIVERED, CANCELLED }
}
