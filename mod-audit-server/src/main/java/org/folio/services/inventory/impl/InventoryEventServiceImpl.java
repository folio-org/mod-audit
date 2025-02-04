package org.folio.services.inventory.impl;

import static org.folio.util.ErrorUtils.handleFailures;

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
import org.folio.rest.jaxrs.model.InventoryAuditCollection;
import org.folio.services.configuration.ConfigurationService;
import org.folio.services.configuration.Setting;
import org.folio.services.inventory.InventoryEventService;
import org.folio.util.inventory.InventoryEvent;
import org.folio.util.inventory.InventoryResourceType;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class InventoryEventServiceImpl implements InventoryEventService {
  private static final String DAO_NOT_FOUND_MESSAGE = "Could not find dao for resource type: ";
  private static final Logger LOGGER = LogManager.getLogger();

  private final Function<InventoryEvent, InventoryAuditEntity> eventToEntityMapper;
  private final Function<List<InventoryAuditEntity>, InventoryAuditCollection> entitiesToCollectionMapper;
  private final ConfigurationService configurationService;
  private final Map<InventoryResourceType, InventoryEventDao> inventoryEventDaos;

  public InventoryEventServiceImpl(Function<InventoryEvent, InventoryAuditEntity> eventToEntityMapper,
                                   Function<List<InventoryAuditEntity>, InventoryAuditCollection> entitiesToCollectionMapper,
                                   ConfigurationService configurationService,
                                   List<InventoryEventDao> inventoryEventDao) {
    this.eventToEntityMapper = eventToEntityMapper;
    this.entitiesToCollectionMapper = entitiesToCollectionMapper;
    this.configurationService = configurationService;
    this.inventoryEventDaos = inventoryEventDao.stream()
      .collect(Collectors.toMap(InventoryEventDao::resourceType, Function.identity()));
  }

  @Override
  public Future<RowSet<Row>> saveEvent(InventoryEvent inventoryEvent, String tenantId) {
    LOGGER.debug("saveEvent:: Trying to save inventoryEvent with [tenantId: {}, eventId: {}, entityId: {}]",
      tenantId, inventoryEvent.getEventId(), inventoryEvent.getEntityId());

    return getDao(inventoryEvent.getResourceType())
      .compose(inventoryEventDao -> {
        var entity = eventToEntityMapper.apply(inventoryEvent);
        return inventoryEventDao.save(entity, tenantId);})
      .recover(throwable -> {
        LOGGER.error("saveEvent:: Could not save InventoryEvent for [tenantId: {}, eventId: {}, entityId: {}]",
          tenantId, inventoryEvent.getEventId(), inventoryEvent.getEntityId());
        return handleFailures(throwable, inventoryEvent.getEventId());
      });
  }

  @Override
  public Future<InventoryAuditCollection> getEvents(InventoryResourceType resourceType, String entityId,
                                                    String eventTs, String tenantId) {
    LOGGER.debug(
      "getEvents:: Trying to retrieve inventory events with [tenantId: {}, resourceType: {},entityId: {}, eventTs: {}]",
      tenantId, resourceType, entityId, eventTs);
    UUID entityUUID;
    Timestamp eventTsTimestamp;
    try {
      entityUUID = UUID.fromString(entityId);
      eventTsTimestamp = eventTs == null ? null : new Timestamp(Long.parseLong(eventTs));
    } catch (IllegalArgumentException e) {
      LOGGER.error(
        "getEvents:: Could not parse entityId or eventTs [tenantId: {}, resourceType: {}, entityId: {}, eventTs: {}]",
        tenantId, resourceType, entityId, eventTs, e);
      return Future.failedFuture(e);
    }

    return getDao(resourceType)
      .compose(inventoryEventDao ->
        configurationService.getSetting(Setting.INVENTORY_RECORDS_PAGE_SIZE, tenantId)
          .compose(setting ->
            inventoryEventDao.get(entityUUID, eventTsTimestamp, (int) setting.getValue(), tenantId)))
      .map(entitiesToCollectionMapper);
  }

  private Future<InventoryEventDao> getDao(InventoryResourceType resourceType) {
    var dao = inventoryEventDaos.get(resourceType);
    if (dao == null) {
      var message = DAO_NOT_FOUND_MESSAGE + resourceType;
      LOGGER.error(message);
      return Future.failedFuture(new IllegalArgumentException(message));
    }
    return Future.succeededFuture(dao);
  }
}
