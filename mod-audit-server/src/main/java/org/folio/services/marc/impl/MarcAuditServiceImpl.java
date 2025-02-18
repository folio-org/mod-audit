package org.folio.services.marc.impl;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.marc.MarcAuditDao;
import org.folio.dao.marc.MarcAuditEntity;
import org.folio.exception.ValidationException;
import org.folio.rest.jaxrs.model.MarcAuditCollection;
import org.folio.services.configuration.ConfigurationService;
import org.folio.services.configuration.Setting;
import org.folio.services.marc.MarcAuditService;
import org.folio.util.marc.MarcUtil;
import org.folio.util.marc.SourceRecordDomainEvent;
import org.folio.util.marc.SourceRecordType;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

import static org.folio.util.ErrorUtils.handleFailures;

@Service
public class MarcAuditServiceImpl implements MarcAuditService {

  private static final Logger LOGGER = LogManager.getLogger();
  private static final Map<SourceRecordType, Setting> SETTINGS_MAP = Map.of(
    SourceRecordType.MARC_BIB, Setting.INVENTORY_RECORDS_ENABLED,
    SourceRecordType.MARC_AUTHORITY, Setting.AUTHORITY_RECORDS_ENABLED
  );

  private final MarcAuditDao marcAuditDao;
  private final ConfigurationService configurationService;

  public MarcAuditServiceImpl(MarcAuditDao marcAuditDao, ConfigurationService configurationService) {
    this.marcAuditDao = marcAuditDao;
    this.configurationService = configurationService;
  }

  @Override
  public Future<RowSet<Row>> saveMarcDomainEvent(SourceRecordDomainEvent event) {
    var tenantId = event.getEventMetadata().getTenantId();
    LOGGER.debug("saveMarcDomainEvent:: Trying to save SourceRecordDomainEvent tenantId: '{}', eventId: '{};", tenantId,
      event.getEventId());
    return configurationService.getSetting(SETTINGS_MAP.get(event.getRecordType()), tenantId)
      .compose(setting -> {
        if (!((boolean) setting.getValue())) {
          LOGGER.debug("saveMarcDomainEvent:: Audit is disabled for tenantId: '{}', recordType: '{}", tenantId,
            event.getRecordType());
          return Future.succeededFuture();
        }
        return save(event, tenantId);
      });
  }

  private Future<RowSet<Row>> save(SourceRecordDomainEvent event, String tenantId) {
    MarcAuditEntity entity;
    try {
      entity = MarcUtil.mapToEntity(event);
    } catch (Exception e) {
      LOGGER.warn(
        "save:: Error during mapping SourceRecordDomainEvent to MarcAuditEntity for event '{}'",
        event.getEventId());
      return handleFailures(e, event.getEventId());
    }
    if (entity.diff() == null || entity.diff().isEmpty()) {
      LOGGER.debug("save:: No changes detected, skipping save record '{}' and tenantId='{}'",
        entity.entityId(), tenantId);
      return Future.succeededFuture();
    }
    return marcAuditDao.save(entity, event.getRecordType(), tenantId)
      .recover(throwable -> {
        LOGGER.error("save:: Could not save order audit event for tenantId: {}", tenantId);
        return handleFailures(throwable, event.getEventId());
      });
  }

  @Override
  public Future<MarcAuditCollection> getMarcAuditRecords(String entityId, SourceRecordType recordType, String tenantId, String dateTime) {
    LOGGER.debug("getMarcAuditRecords:: Trying to get records by tenantId: '{}', entityId: '{}', record type '{}';", tenantId, entityId, recordType);
    UUID entityUUID;
    LocalDateTime eventDateTime;
    try {
      entityUUID = UUID.fromString(entityId);
      eventDateTime = dateTime == null ? null : LocalDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(dateTime)), ZoneId.systemDefault());
    } catch (IllegalArgumentException e) {
      LOGGER.error("getMarcAuditRecords:: Could not parse entityId or eventDateTime for tenantId: '{}', recordType: '{}', entityId: '{}'", tenantId, recordType, entityId, e);
      return Future.failedFuture(new ValidationException(e.getMessage()));
    }
    return getMarcRecordPageSize(tenantId, recordType)
      .compose(limit -> marcAuditDao.get(entityUUID, recordType, tenantId, eventDateTime, limit))
      .map(MarcUtil::mapToCollection)
      .recover(throwable -> {
        LOGGER.error("getMarcAuditRecords:: Could not retrieve marc audit records for tenantId: '{}', recordType: '{}', entityId: '{}'", tenantId, recordType, entityId, throwable);
        return handleFailures(throwable, entityId);
      });
  }

  private Future<Integer> getMarcRecordPageSize(String tenantId, SourceRecordType type) {
    var setting = SourceRecordType.MARC_BIB.equals(type) ? Setting.INVENTORY_RECORDS_PAGE_SIZE : Setting.AUTHORITY_RECORDS_PAGE_SIZE;
    return configurationService.getSetting(setting, tenantId)
      .map(result -> (int) result.getValue());
  }
}
