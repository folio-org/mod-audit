package org.folio.services.marc.impl;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.marc.MarcAuditDao;
import org.folio.dao.marc.MarcAuditEntity;
import org.folio.rest.jaxrs.model.MarcAuditCollection;
import org.folio.services.marc.MarcAuditService;
import org.folio.util.marc.ParsedRecordUtil;
import org.folio.util.marc.SourceRecordDomainEvent;
import org.folio.util.marc.SourceRecordType;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static org.folio.util.ErrorUtils.handleFailures;

@Service
public class MarcAuditServiceImpl implements MarcAuditService {

  private static final Logger LOGGER = LogManager.getLogger();

  private final MarcAuditDao marcAuditDao;

  public MarcAuditServiceImpl(MarcAuditDao marcAuditDao) {
    this.marcAuditDao = marcAuditDao;
  }

  @Override
  public Future<RowSet<Row>> saveMarcDomainEvent(SourceRecordDomainEvent event) {
    var tenantId = event.getEventMetadata().getTenantId();
    LOGGER.debug("saveOrderAuditEvent:: Trying to save SourceRecordDomainEvent tenantId: '{}', eventId: '{}', record type '{}';", tenantId, event.getEventId(), event.getRecordType());
    MarcAuditEntity entity;
    try {
      entity = ParsedRecordUtil.mapToEntity(event);
    } catch (Exception e) {
      LOGGER.warn("saveMarcBibDomainEvent:: Error during mapping SourceRecordDomainEvent to MarcAuditEntity for event '{}'", event.getEventId());
      return handleFailures(e, event.getEventId());
    }
    if (entity.diff() == null || entity.diff().isEmpty()) {
      LOGGER.debug("saveMarcBibDomainEvent:: No changes detected, skipping save record: '{}' and tenantId: '{}'", entity.entityId(), tenantId);
      return Future.succeededFuture();
    }
    return marcAuditDao.save(entity, event.getRecordType(), tenantId)
            .recover(throwable -> {
              LOGGER.error("handleFailures:: Could not save marc audit event for tenantId: {}", tenantId);
              return handleFailures(throwable, event.getEventId());
            });
  }

  @Override
  public Future<MarcAuditCollection> getMarcAuditRecords(String entityId, SourceRecordType recordType, String tenantId, int limit, int offset) {
    UUID entityUUID;
    try {
      entityUUID = UUID.fromString(entityId);
    } catch (IllegalArgumentException e) {
      LOGGER.error("getMarcAuditRecords:: Could not parse entityId  for tenantId: '{}', recordType: '{}', entityId: '{}'", tenantId, recordType, entityId, e);
      return Future.failedFuture(e);
    }
    return marcAuditDao.get(entityUUID, recordType, tenantId, limit, offset)
            .map(ParsedRecordUtil::mapToCollection)
            .recover(throwable -> {
              LOGGER.error("getMarcAuditRecords:: Could not retrieve marc audit records for tenantId: '{}', recordType: '{}', entityId: '{}'", tenantId, recordType, entityId, throwable);
              return handleFailures(throwable, entityId);
            });
  }
}
