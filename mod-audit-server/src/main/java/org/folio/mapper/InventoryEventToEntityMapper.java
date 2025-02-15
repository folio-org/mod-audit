package org.folio.mapper;

import static org.folio.util.inventory.InventoryUtils.extractUserId;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.folio.dao.inventory.InventoryAuditEntity;
import org.folio.domain.diff.ChangeRecordDto;
import org.folio.services.diff.DiffCalculator;
import org.folio.util.inventory.InventoryEvent;
import org.folio.util.inventory.InventoryEventType;
import org.folio.util.inventory.InventoryResourceType;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventToEntityMapper implements Function<InventoryEvent, InventoryAuditEntity> {

  private final Map<InventoryResourceType, DiffCalculator<?>> diffServices;

  public InventoryEventToEntityMapper(List<DiffCalculator<?>> diffServices) {
    this.diffServices = diffServices.stream()
      .collect(Collectors.toMap(DiffCalculator::getResourceType, Function.identity()));
  }

  @Override
  public InventoryAuditEntity apply(InventoryEvent event) {
    var userId = extractUserId(event);
    var diff = InventoryEventType.UPDATE.equals(event.getType())
               ? getDiff(event)
               : null;
    return new InventoryAuditEntity(
      UUID.fromString(event.getEventId()),
      new Timestamp(event.getEventTs()),
      UUID.fromString(event.getEntityId()),
      event.getType().name(),
      UUID.fromString(userId),
      diff
    );
  }

  private ChangeRecordDto getDiff(InventoryEvent event) {
    var diff = diffServices.get(event.getResourceType()).calculateDiff(event.getOldValue(), event.getNewValue());
    if (diff == null || (diff.getFieldChanges().isEmpty() && diff.getCollectionChanges().isEmpty())) {
      return null;
    }
    return diff;
  }
}
