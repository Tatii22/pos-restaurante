package com.pos.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "audit_changes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private AuditEvent event;

    @Column(nullable = false, length = 120)
    private String fieldName;

    @Column(length = 1000)
    private String oldValue;

    @Column(length = 1000)
    private String newValue;
}
