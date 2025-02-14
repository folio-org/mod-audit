package org.folio.dao.marc;

import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Represents a record in the MarcAudit table.
 */
public record MarcAuditEntity(
  @NonNull String eventId,
  @NonNull LocalDateTime eventDate,
  @NonNull String entityId,
  @NonNull String origin,
  @NonNull String action,
  @NonNull String userId,
  Map<String, Object> diff) {
}
