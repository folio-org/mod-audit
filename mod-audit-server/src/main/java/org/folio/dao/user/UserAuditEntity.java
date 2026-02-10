package org.folio.dao.user;

import java.sql.Timestamp;
import java.util.UUID;
import org.folio.domain.diff.ChangeRecordDto;

public record UserAuditEntity(UUID eventId, Timestamp eventDate, UUID userId,
                               String action, UUID performedBy, ChangeRecordDto diff) { }
