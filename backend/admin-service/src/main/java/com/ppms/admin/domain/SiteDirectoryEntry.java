package com.ppms.admin.domain;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "site_directory_entry", schema = "admin")
public class SiteDirectoryEntry extends BaseEntity {

    @Column(name = "site_id", nullable = false, unique = true)
    private java.util.UUID siteId;

    @Column(name = "site_type", nullable = false)
    private String siteType;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "county")
    private String county;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "source_service")
    private String sourceService;

    @Column(name = "last_event_at")
    private Instant lastEventAt;
}
