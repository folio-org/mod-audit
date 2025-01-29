package org.folio.services.inventory.impl;

import static org.folio.util.ErrorUtils.handleFailures;
import static org.folio.util.inventory.InventoryUtils.extractUserId;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.inventory.InventoryAuditEntity;
import org.folio.dao.inventory.InventoryEventDao;
import org.folio.services.inventory.InventoryEventService;
import org.folio.util.inventory.InventoryEvent;
import org.folio.util.inventory.InventoryResourceType;
import org.springframework.stereotype.Service;

@Service
public class InventoryEventServiceImpl implements InventoryEventService {
  private static final Logger LOGGER = LogManager.getLogger();

  private final Map<InventoryResourceType, InventoryEventDao> inventoryEventDaos;

  public InventoryEventServiceImpl(List<InventoryEventDao> inventoryEventDao) {
    this.inventoryEventDaos = inventoryEventDao.stream()
      .collect(Collectors.toMap(InventoryEventDao::resourceType, Function.identity()));
  }

  @Override
  public Future<RowSet<Row>> saveEvent(InventoryEvent inventoryEvent, String tenantId) {
    LOGGER.debug("saveEvent:: Trying to save inventoryEvent with [tenantId: {}, eventId: {}, entityId: {}]",
      tenantId, inventoryEvent.getEventId(), inventoryEvent.getEntityId());
    var inventoryEventDao = inventoryEventDaos.get(inventoryEvent.getResourceType());
    if (inventoryEventDao == null) {
      LOGGER.warn(
        "saveEvent:: Could not find dao for [tenantId: {}, eventId: {}, entityId: {}, resourceType: {}]]",
        tenantId, inventoryEvent.getEventId(), inventoryEvent.getEntityId(), inventoryEvent.getResourceType());
      return Future.failedFuture(
        new IllegalArgumentException("Could not find dao for resource type: " + inventoryEvent.getResourceType()));
    }

    var entity = mapToEntity(inventoryEvent);
    return inventoryEventDao.save(entity, tenantId)
      .recover(throwable -> {
        LOGGER.error("saveEvent:: Could not save InventoryEvent for [tenantId: {}, eventId: {}, entityId: {}]",
          tenantId, inventoryEvent.getEventId(), inventoryEvent.getEntityId());
        return handleFailures(throwable, inventoryEvent.getEventId());
      });
  }

  private InventoryAuditEntity mapToEntity(InventoryEvent event) {
    var userId = extractUserId(event);
    return new InventoryAuditEntity(
      UUID.fromString(event.getEventId()),
      new Timestamp(event.getEventTs()),
      UUID.fromString(event.getEntityId()),
      event.getType().name(),
      UUID.fromString(userId),
      event.getNewValue() //todo: replace with appropriate diff
    );
  }
}
