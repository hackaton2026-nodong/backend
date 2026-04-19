package com.kworkerharmony.backend.enterprise;

import com.kworkerharmony.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "enterprises")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enterprise extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String businessNumber;

    @Column(nullable = false, length = 255)
    private String industry;

    @Column(nullable = false, length = 100)
    private String country;

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnterpriseStatus status;

    public Enterprise(String name, String businessNumber, String industry, String country, EnterpriseStatus status) {
        this.name = name;
        this.businessNumber = businessNumber;
        this.industry = industry;
        this.country = country;
        this.status = status;
    }
}
