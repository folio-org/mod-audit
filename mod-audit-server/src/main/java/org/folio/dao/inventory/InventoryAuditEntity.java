package org.folio.dao.inventory;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

public record InventoryAuditEntity (UUID eventId, Timestamp eventDate, UUID entityId,
                                    String action, UUID userId, Map<String, Object> diff) {}
