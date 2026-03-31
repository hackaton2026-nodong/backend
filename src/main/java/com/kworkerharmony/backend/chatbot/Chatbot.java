package com.kworkerharmony.backend.chatbot;

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
@Table(name = "chatbots")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Chatbot extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String diagnose;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uid", nullable = false)
    private User user;

    public Chatbot(String diagnose, User user) {
        this.diagnose = diagnose;
        this.user = user;
    }
}
