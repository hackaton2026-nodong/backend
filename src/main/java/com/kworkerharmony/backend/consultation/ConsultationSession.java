package com.kworkerharmony.backend.consultation;

import com.kworkerharmony.backend.global.entity.BaseEntity;
import com.kworkerharmony.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "consultation_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsultationSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "case_id", length = 36)
    private String caseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public ConsultationSession(String title, String caseId, User user) {
        this.title = title;
        this.caseId = caseId;
        this.user = user;
    }
}
