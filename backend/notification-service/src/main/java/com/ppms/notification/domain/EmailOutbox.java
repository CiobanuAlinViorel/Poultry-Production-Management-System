package com.ppms.notification.domain;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "email_outbox", schema = "notification")
public class EmailOutbox extends BaseEntity {

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "subject")
    private String subject;

    @Column(name = "body_html")
    private String bodyHtml;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "attempts")
    private int attempts = 0;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "error")
    private String error;

    public enum Status { PENDING, SENT, FAILED }
}
