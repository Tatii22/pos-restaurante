package com.pos.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "audit_events", indexes = {
        @Index(name = "idx_audit_entity", columnList = "entityType,entityId"),
        @Index(name = "idx_audit_occurred", columnList = "occurredAt"),
        @Index(name = "idx_audit_type", columnList = "eventType")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String eventType;

    @Column(nullable = false, length = 80)
    private String entityType;

    @Column(nullable = false, length = 80)
    private String entityId;

    @ManyToOne(fetch = FetchType.LAZY)
    private Usuario actor;

    @Column(length = 80)
    private String actorUsername;

    @Column(length = 40)
    private String actorRole;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    private TurnoCaja turno;

    @Column(length = 255)
    private String reason;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AuditChange> changes = new ArrayList<>();

    public void addChange(String fieldName, String oldValue, String newValue) {
        AuditChange change = AuditChange.builder()
                .event(this)
                .fieldName(fieldName)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();
        changes.add(change);
    }
}
