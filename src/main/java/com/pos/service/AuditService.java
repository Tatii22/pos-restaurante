package com.pos.service;

import com.pos.entity.AuditEvent;
import com.pos.entity.TurnoCaja;
import com.pos.entity.Usuario;
import com.pos.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    @Transactional
    public void record(
            String eventType,
            String entityType,
            Object entityId,
            Usuario actor,
            TurnoCaja turno,
            String reason,
            Change... changes
    ) {
        AuditEvent event = AuditEvent.builder()
                .eventType(eventType)
                .entityType(entityType)
                .entityId(entityId == null ? "-" : String.valueOf(entityId))
                .actor(actor)
                .actorUsername(actor != null ? actor.getUsername() : null)
                .actorRole(actor != null && actor.getRol() != null ? actor.getRol().getNombre() : null)
                .occurredAt(java.time.LocalDateTime.now())
                .turno(turno)
                .reason(reason)
                .build();

        if (changes != null) {
            for (Change change : changes) {
                if (change != null) {
                    event.addChange(change.fieldName(), toText(change.oldValue()), toText(change.newValue()));
                }
            }
        }

        auditEventRepository.save(event);
    }

    public Change change(String fieldName, Object oldValue, Object newValue) {
        return new Change(fieldName, oldValue, newValue);
    }

    private String toText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value);
        return text.length() > 1000 ? text.substring(0, 1000) : text;
    }

    public record Change(String fieldName, Object oldValue, Object newValue) {
    }
}
