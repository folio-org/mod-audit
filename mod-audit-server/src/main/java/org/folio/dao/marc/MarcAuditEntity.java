package org.folio.dao.marc;

import java.time.LocalDateTime;
import java.util.Map;

public record MarcAuditEntity(String eventId, LocalDateTime eventDate, String entityId, String origin,
                              String action, String userId, Map<String, Object> diff) {
}
