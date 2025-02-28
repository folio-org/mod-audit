package org.folio.services.inventory.impl;

import static org.folio.util.ErrorUtils.handleFailures;

import io.vertx.core.Future;
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
import org.folio.exception.ValidationException;
import org.folio.rest.jaxrs.model.InventoryAuditCollection;
import org.folio.services.configuration.ConfigurationService;
import org.folio.services.configuration.Setting;
import org.folio.services.inventory.InventoryEventService;
import org.folio.util.inventory.InventoryEvent;
import org.folio.util.inventory.InventoryEventType;
import org.folio.util.inventory.InventoryResourceType;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class InventoryEventServiceImpl implements InventoryEventService {

  private static final Logger LOGGER = LogManager.getLogger();

  private static final String DAO_NOT_FOUND_MESSAGE = "Could not find dao for resource type: ";

  private final Function<InventoryEvent, InventoryAuditEntity> eventToEntityMapper;
  private final Function<List<InventoryAuditEntity>, InventoryAuditCollection> entitiesToCollectionMapper;
  private final ConfigurationService configurationService;
  private final Map<InventoryResourceType, InventoryEventDao> inventoryEventDaoMap;

  public InventoryEventServiceImpl(Function<InventoryEvent, InventoryAuditEntity> eventToEntityMapper,
                                   Function<List<InventoryAuditEntity>, InventoryAuditCollection> entitiesToCollectionMapper,
                                   ConfigurationService configurationService,
                                   List<InventoryEventDao> inventoryEventDaoList) {
    this.eventToEntityMapper = eventToEntityMapper;
    this.entitiesToCollectionMapper = entitiesToCollectionMapper;
    this.configurationService = configurationService;
    this.inventoryEventDaoMap = inventoryEventDaoList.stream()
      .collect(Collectors.toMap(InventoryEventDao::resourceType, Function.identity()));
  }

  @Override
  public Future<String> processEvent(InventoryEvent inventoryEvent, String tenantId) {
    LOGGER.debug("saveEvent:: Trying to save InventoryEvent with [tenantId: {}, eventId: {}, entityId: {}]",
      tenantId, inventoryEvent.getEventId(), inventoryEvent.getEntityId());

    return configurationService.getSetting(Setting.INVENTORY_RECORDS_ENABLED, tenantId)
      .compose(setting -> {
        if (!((boolean) setting.getValue())) {
          LOGGER.debug("saveEvent:: Inventory audit is disabled for tenant [tenantId: {}]", tenantId);
          return Future.succeededFuture(inventoryEvent.getEventId());
        }
        return process(inventoryEvent, tenantId);
      })
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
      return Future.failedFuture(new ValidationException(e.getMessage()));
    }

    return getDao(resourceType)
      .compose(inventoryEventDao ->
        inventoryEventDao.count(entityUUID, tenantId)
          .compose(fetchIfExist(resourceType, eventTs, tenantId, inventoryEventDao, entityUUID, eventTsTimestamp))
      );
  }

  private Future<String> process(InventoryEvent inventoryEvent, String tenantId) {
    return getDao(inventoryEvent.getResourceType())
      .compose(inventoryEventDao -> {
        if (Boolean.TRUE.equals(inventoryEvent.getIsConsortiumShadowCopy())) {
          return deleteAll(inventoryEventDao, inventoryEvent, tenantId);
        } else {
          return save(inventoryEventDao, inventoryEvent, tenantId);
        }
      });
  }

  private Future<String> save(InventoryEventDao inventoryEventDao, InventoryEvent inventoryEvent, String tenantId) {
    var eventId = inventoryEvent.getEventId();
    LOGGER.debug("save:: Trying to save InventoryEvent with [tenantId: {}, eventId: {}, entityId: {}]",
      tenantId, eventId, inventoryEvent.getEntityId());

    var entity = eventToEntityMapper.apply(inventoryEvent);
    if (InventoryEventType.UPDATE.name().equals(entity.action()) && entity.diff() == null) {
      LOGGER.debug(
        "save:: No diff calculated for InventoryEvent with [tenantId: {}, eventId: {}, entityId: {}]",
        tenantId, eventId, inventoryEvent.getEntityId());
      return Future.succeededFuture(eventId);
    }

    return inventoryEventDao.save(entity, tenantId).map(eventId);
  }

  private Future<String> deleteAll(InventoryEventDao inventoryEventDao, InventoryEvent inventoryEvent, String tenantId) {
    var entityId = UUID.fromString(inventoryEvent.getEntityId());
    LOGGER.debug("deleteAll:: Trying to delete all InventoryEvents with [tenantId: {}, entityId: {}]",
      tenantId, entityId);
    return inventoryEventDao.deleteAll(entityId, tenantId)
      .map(inventoryEvent.getEventId());
  }

  @Override
  public Future<Void> expireRecords(String tenantId, Timestamp expireOlderThan) {
    return Future.all(inventoryEventDaoMap.values().stream()
        .map(dao -> dao.deleteOlderThanDate(expireOlderThan, tenantId))
        .toList())
      .mapEmpty();
  }

  private Function<Integer, Future<InventoryAuditCollection>> fetchIfExist(InventoryResourceType resourceType,
                                                                           String eventTs, String tenantId,
                                                                           InventoryEventDao inventoryEventDao,
                                                                           UUID entityId, Timestamp eventTsTimestamp) {
    return count -> {
      if (count == 0) {
        LOGGER.debug(
          "fetchIfExist:: No inventory events found for [tenantId: {}, resourceType: {}, entityId: {}, eventTs: {}]",
          tenantId, resourceType, entityId, eventTs);
        return Future.succeededFuture(new InventoryAuditCollection().withTotalRecords(0));
      }
      return configurationService.getSetting(Setting.INVENTORY_RECORDS_PAGE_SIZE, tenantId)
        .map(setting -> (Integer) setting.getValue())
        .compose(limit -> inventoryEventDao.get(entityId, eventTsTimestamp, limit, tenantId))
        .map(entitiesToCollectionMapper)
        .map(inventoryAuditCollection -> inventoryAuditCollection.withTotalRecords(count));
    };
  }

  private Future<InventoryEventDao> getDao(InventoryResourceType resourceType) {
    var dao = inventoryEventDaoMap.get(resourceType);
    if (dao == null) {
      var message = DAO_NOT_FOUND_MESSAGE + resourceType;
      LOGGER.error(message);
      return Future.failedFuture(new IllegalArgumentException(message));
    }
    return Future.succeededFuture(dao);
  }
}
