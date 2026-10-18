package com.ppms.admin.domain.workforce;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @Entity
@Table(name = "certification", schema = "admin")
public class Certification extends BaseEntity {
    public enum Type { VET_LICENCE, WELFARE_CERT_1099_2009, FORKLIFT, HACCP }
    @Column(name = "employee_id", nullable = false) private UUID employeeId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Type type;
    private String number;
    @Column(name = "issued_at") private LocalDate issuedAt;
    @Column(name = "expires_at") private LocalDate expiresAt;
    @Column(name = "document_ref") private String documentRef;
}
