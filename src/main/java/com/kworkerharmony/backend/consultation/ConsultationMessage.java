package com.kworkerharmony.backend.consultation;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "consultation_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsultationMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private ConsultationSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsultationMessageRole role;

    @Column(nullable = false, length = 4000)
    private String content;

    public ConsultationMessage(ConsultationSession session, ConsultationMessageRole role, String content) {
        this.session = session;
        this.role = role;
        this.content = content;
    }
}
