package org.folio.services.marc.impl;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.marc.MarcAuditDao;
import org.folio.services.marc.MarcAuditService;
import org.folio.util.marc.ParsedRecordUtil;
import org.folio.util.marc.SourceRecordDomainEvent;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.folio.util.ErrorUtils.handleFailures;

@Service
public class MarcAuditServiceImpl implements MarcAuditService {

  private static final Logger LOGGER = LogManager.getLogger();

  private final MarcAuditDao marcAuditDao;

  public MarcAuditServiceImpl(MarcAuditDao marcAuditDao) {
    this.marcAuditDao = marcAuditDao;
  }

  @Override
  public Future<RowSet<Row>> saveMarcDomainEvent(SourceRecordDomainEvent sourceRecordDomainEvent) {
    var tenantId = sourceRecordDomainEvent.getEventMetadata().getTenantId();
    LOGGER.debug("saveOrderAuditEvent:: Saving marc bib event for tenantId={}", tenantId);
    var entity = ParsedRecordUtil.mapToEntity(sourceRecordDomainEvent);
    if (isEmptyDifference(entity.diff())) {
      LOGGER.debug("saveMarcBibDomainEvent:: No changes detected, skipping save for tenantId={}", tenantId);
      return Future.succeededFuture();
    }
    return marcAuditDao.save(entity, tenantId)
      .recover(throwable -> {
        LOGGER.error("handleFailures:: Could not save order audit event for tenantId: {}", tenantId);
        return handleFailures(throwable, sourceRecordDomainEvent.getEventId());
      });
  }

  private boolean isEmptyDifference(Map<String, Object> difference) {
    return difference != null &&
      difference.getOrDefault("added", Collections.emptyList()) instanceof List<?> added && added.isEmpty() &&
      difference.getOrDefault("removed", Collections.emptyList()) instanceof List<?> removed && removed.isEmpty() &&
      difference.getOrDefault("modified", Collections.emptyList()) instanceof List<?> modified && modified.isEmpty();
  }
}
