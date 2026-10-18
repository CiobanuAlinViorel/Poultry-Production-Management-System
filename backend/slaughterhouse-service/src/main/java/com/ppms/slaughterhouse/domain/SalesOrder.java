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
@Table(name = "sales_order", schema = "slaughterhouse")
public class SalesOrder extends BaseEntity {

    @Column(name = "order_no", nullable = false, unique = true)
    private String orderNo;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Column(name = "order_date")
    private Instant orderDate;

    @Column(name = "requested_date")
    private Instant requestedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Status status = Status.DRAFT;

    public enum Status { DRAFT, CONFIRMED, IN_PRODUCTION, DISPATCHED, CLOSED, CANCELLED }
}
