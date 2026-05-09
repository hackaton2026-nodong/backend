package com.kworkerharmony.backend.alert.entity;

import com.kworkerharmony.backend.alert.domain.AlertType;
import com.kworkerharmony.backend.global.entity.BaseEntity;
import com.kworkerharmony.backend.user.User;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "alerts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Alert extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertType type;

    @Column(nullable = false)
    private boolean isRead;

    public Alert(User user, String title, String message, AlertType type, boolean isRead) {
        this.user = user;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = isRead;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
