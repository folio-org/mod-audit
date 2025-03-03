package org.folio.dao.marc;

import lombok.NonNull;
import org.folio.domain.diff.ChangeRecordDto;

import java.time.LocalDateTime;

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
  ChangeRecordDto diff) {
}
