package com.kworkerharmony.backend.user;

import com.kworkerharmony.backend.enterprise.Enterprise;
import com.kworkerharmony.backend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "visa_expires_at")
    private LocalDate visaExpiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserType userType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "country_code", nullable = false, length = 10)
    private String countryCode;

    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enterprise_id")
    private Enterprise enterprise;

    public User(
            String email,
            String passwordHash,
            String name,
            Role role,
            UserType userType,
            UserStatus status,
            String countryCode,
            String languageCode,
            Enterprise enterprise
    ) {
        this(
                email,
                passwordHash,
                name,
                null,
                null,
                null,
                role,
                userType,
                status,
                countryCode,
                languageCode,
                enterprise
        );
    }

    public User(
            String email,
            String passwordHash,
            String name,
            LocalDate birthDate,
            String phoneNumber,
            LocalDate visaExpiresAt,
            Role role,
            UserType userType,
            UserStatus status,
            String countryCode,
            String languageCode,
            Enterprise enterprise
    ) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.birthDate = birthDate;
        this.phoneNumber = phoneNumber;
        this.visaExpiresAt = visaExpiresAt;
        this.role = role;
        this.userType = userType;
        this.status = status;
        this.countryCode = countryCode;
        this.languageCode = languageCode;
        this.enterprise = enterprise;
    }

    public void assignEnterprise(Enterprise enterprise) {
        this.enterprise = enterprise;
    }

    public void changeRole(Role role) {
        this.role = role;
    }

    public void changeUserType(UserType userType) {
        this.userType = userType;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }
}
