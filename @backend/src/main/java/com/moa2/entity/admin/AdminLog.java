package com.moa2.entity.admin;

import com.moa2.entity.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "admin_logs")
public class AdminLog {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 작업을 수행한 관리자 (단방향)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType; // USER, SHOW, RESERVATION

    @Column(nullable = false)
    private Long targetId; // 대상의 ID (다형성 대신 ID만 저장하여 결합도 낮춤)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminAction action; // FORCE_CANCEL, FORCE_DELETE ...

    private String reason; // 사유
    private String clientIp; // IP 주소

    private LocalDateTime createdAt;

    @Builder
    public AdminLog(User admin, TargetType targetType, Long targetId, 
                    AdminAction action, String reason, String clientIp) {
        this.admin = admin;
        this.targetType = targetType;
        this.targetId = targetId;
        this.action = action;
        this.reason = reason;
        this.clientIp = clientIp;
        this.createdAt = LocalDateTime.now();
    }
}

