package org.folio.dao.inventory;

import java.sql.Timestamp;
import java.util.UUID;

public record InventoryAuditEntity (UUID eventId, Timestamp eventDate, UUID entityId, String origin,
                                    String action, UUID userId, Object diff) {}
