package org.folio.dao.inventory;

import java.sql.Timestamp;
import java.util.UUID;
import org.folio.domain.diff.ChangeRecordDto;

public record InventoryAuditEntity(UUID eventId, Timestamp eventDate, UUID entityId,
                                   String action, UUID userId, ChangeRecordDto diff) { }
