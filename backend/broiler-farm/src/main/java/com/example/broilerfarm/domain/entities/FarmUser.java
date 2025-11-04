package com.example.broilerfarm.domain.entities;

import com.example.shared.domain.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "farm_user")
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmUser extends User {

    @OneToOne
    @JoinColumn(name = "id_employee")
    private FarmEmployee employee;
}

