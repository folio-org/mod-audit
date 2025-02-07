package org.folio.services.marc.impl;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.marc.MarcAuditDao;
import org.folio.dao.marc.MarcAuditEntity;
import org.folio.services.marc.MarcAuditService;
import org.folio.util.marc.ParsedRecordUtil;
import org.folio.util.marc.SourceRecordDomainEvent;
import org.springframework.stereotype.Service;

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
    LOGGER.debug("saveOrderAuditEvent:: Trying to save SourceRecordDomainEvent tenantId: '{}', eventId: '{};", tenantId, event.getEventId());
    MarcAuditEntity entity;
    try {
      entity = ParsedRecordUtil.mapToEntity(event);
    } catch (Exception e) {
      LOGGER.warn("saveMarcBibDomainEvent:: Error during mapping SourceRecordDomainEvent to MarcAuditEntity for event '{}'", event.getEventId());
      return handleFailures(e, event.getEventId());
    }
    if (entity.diff() == null || entity.diff().isEmpty()) {
      LOGGER.debug("saveMarcBibDomainEvent:: No changes detected, skipping save record '{}' and tenantId='{}'", entity.recordId(), tenantId);
      return Future.succeededFuture();
    }
    return marcAuditDao.save(entity, tenantId)
      .recover(throwable -> {
        LOGGER.error("handleFailures:: Could not save order audit event for tenantId: {}", tenantId);
        return handleFailures(throwable, event.getEventId());
      });
  }
}
