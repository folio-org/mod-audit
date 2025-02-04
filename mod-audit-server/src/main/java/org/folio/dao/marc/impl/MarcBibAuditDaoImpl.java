package org.folio.dao.marc.impl;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.marc.MarcAuditEntity;
import org.folio.dao.marc.MarcBibAuditDao;
import org.folio.util.PostgresClientFactory;
import org.folio.util.marc.SourceRecordType;
import org.springframework.stereotype.Repository;

import static java.lang.String.format;
import static org.folio.util.DbUtils.formatDBTableName;

@Repository
public class MarcBibAuditDaoImpl implements MarcBibAuditDao {
  private static final Logger LOGGER = LogManager.getLogger();

  private static final String MARC_BIB_TABLE = "marc_bib_audit";
  private static final String MARC_AUTHORITY_TABLE = "marc_authority_audit";
  private static final String INSERT_SQL = "INSERT INTO %s (event_id, event_date, entity_id, origin, action, user_id, diff)" +
    " VALUES ($1, $2, $3, $4, $5, $6, $7)";

  private final PostgresClientFactory pgClientFactory;

  public MarcBibAuditDaoImpl(PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  @Override
  public Future<RowSet<Row>> save(MarcAuditEntity entity, SourceRecordType recordType, String tenantId) {
    LOGGER.debug("save:: Saving Marc domain event with record id: {}", entity.entityId());
    var tableName = tableName(recordType);
    var query = format(INSERT_SQL, formatDBTableName(tenantId, tableName));
    return makeSaveCall(query, entity, tenantId)
      .onSuccess(rows -> LOGGER.info("save:: Saved Marc Bib domain event with record id: '{}' in to table '{}'", entity.entityId(), tableName))
      .onFailure(e -> LOGGER.error("Failed to save record with id: '{}' in to table '{}'", entity.entityId(), tableName, e));
  }

  private Future<RowSet<Row>> makeSaveCall(String query, MarcAuditEntity entity, String tenantId) {
    LOGGER.debug("makeSaveCall:: Making save call with query : {} and tenant id : {}", query, tenantId);
    try {
      return pgClientFactory.createInstance(tenantId).execute(query, Tuple.of(
        entity.eventId(),
        entity.eventDate(),
        entity.entityId(),
        entity.origin(),
        entity.action(),
        entity.userId(),
        JsonObject.mapFrom(entity.diff())));
    } catch (Exception e) {
      return Future.failedFuture(e);
    }
  }

  private String tableName(SourceRecordType recordType) {
    return recordType.equals(SourceRecordType.MARC_BIB) ? MARC_BIB_TABLE : MARC_AUTHORITY_TABLE;
  }
}
