package com.ppms.farm.domain;

import com.ppms.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "farm", schema = "fa")
@Getter @Setter @NoArgsConstructor
public class Farm extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50) private String code;
    @Column(nullable = false) private String name;
    @Column(length = 500) private String address;
    @Column(length = 100) private String county;
    @Column(nullable = false) private boolean active = true;
}
