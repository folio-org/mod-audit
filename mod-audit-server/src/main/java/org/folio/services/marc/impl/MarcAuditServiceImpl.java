package org.folio.services.marc.impl;

import static org.folio.util.ErrorUtils.handleFailures;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.marc.MarcAuditDao;
import org.folio.dao.marc.MarcAuditEntity;
import org.folio.services.configuration.ConfigurationService;
import org.folio.services.configuration.Setting;
import org.folio.services.marc.MarcAuditService;
import org.folio.util.marc.ParsedRecordUtil;
import org.folio.util.marc.SourceRecordDomainEvent;
import org.folio.util.marc.SourceRecordType;
import org.springframework.stereotype.Service;

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
    var recordType = getSourceRecordType(event);
    return configurationService.getSetting(SETTINGS_MAP.get(recordType), tenantId)
      .compose(setting -> {
        if (!((boolean) setting.getValue())) {
          LOGGER.debug("saveMarcDomainEvent:: Audit is disabled for tenantId: '{}', recordType: '{}", tenantId,
            recordType);
          return Future.succeededFuture();
        }
        return save(event, tenantId);
      });
  }

  private SourceRecordType getSourceRecordType(SourceRecordDomainEvent event) {
    var marcRecord = event.getEventPayload().getNewRecord() == null ? event.getEventPayload().getOld()
                                                                    : event.getEventPayload().getNewRecord();
    return marcRecord.getRecordType();
  }

  private Future<RowSet<Row>> save(SourceRecordDomainEvent event, String tenantId) {
    MarcAuditEntity entity;
    try {
      entity = ParsedRecordUtil.mapToEntity(event);
    } catch (Exception e) {
      LOGGER.warn(
        "save:: Error during mapping SourceRecordDomainEvent to MarcAuditEntity for event '{}'",
        event.getEventId());
      return handleFailures(e, event.getEventId());
    }
    if (entity.diff() == null || entity.diff().isEmpty()) {
      LOGGER.debug("save:: No changes detected, skipping save record '{}' and tenantId='{}'",
        entity.recordId(), tenantId);
      return Future.succeededFuture();
    }
    return marcAuditDao.save(entity, tenantId)
      .recover(throwable -> {
        LOGGER.error("save:: Could not save order audit event for tenantId: {}", tenantId);
        return handleFailures(throwable, event.getEventId());
      });
  }
}
