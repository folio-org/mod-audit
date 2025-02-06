package org.folio.dao.marc;

import lombok.NonNull;
import org.folio.util.marc.SourceRecordType;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a record in the MarcAudit table.
 */
public record MarcAuditEntity(
  @NonNull String eventId,
  @NonNull LocalDateTime eventDate,
  @NonNull String recordId,
  @NonNull String origin,
  @NonNull String action,
  @NonNull String userId,
  @NonNull SourceRecordType recordType,
  Map<String, Object> diff) {
}
