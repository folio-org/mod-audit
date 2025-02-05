package org.folio.dao.marc;

import org.folio.util.marc.SourceRecordType;

import java.time.LocalDateTime;
import java.util.Map;

public record MarcAuditEntity(String eventId, LocalDateTime eventDate, String recordId, String origin,
                              String action, String userId, SourceRecordType recordType, Map<String, Object> diff) {
}
