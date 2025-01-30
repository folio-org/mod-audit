package org.folio.mapper;

import static org.folio.util.inventory.InventoryUtils.extractUserId;

import java.sql.Timestamp;
import java.util.UUID;
import java.util.function.Function;
import org.folio.dao.inventory.InventoryAuditEntity;
import org.folio.util.inventory.InventoryEvent;
import org.folio.util.inventory.InventoryEventType;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventToEntityMapper implements Function<InventoryEvent, InventoryAuditEntity> {

  @Override
  public InventoryAuditEntity apply(InventoryEvent event) {
    var userId = extractUserId(event);
    var diff = InventoryEventType.DELETE.equals(event.getType()) ? null : event.getNewValue();
    return new InventoryAuditEntity(
      UUID.fromString(event.getEventId()),
      new Timestamp(event.getEventTs()),
      UUID.fromString(event.getEntityId()),
      event.getType().name(),
      UUID.fromString(userId),
      diff //todo: replace with appropriate diff
    );
  }
}
