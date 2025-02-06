package org.folio.dao.marc.impl;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.marc.MarcAuditEntity;
import org.folio.dao.marc.MarcAuditDao;
import org.folio.util.PostgresClientFactory;
import org.folio.util.marc.SourceRecordType;
import org.springframework.stereotype.Repository;

import static java.lang.String.format;
import static org.folio.util.DbUtils.formatDBTableName;

@Repository
public class MarcAuditDaoImpl implements MarcAuditDao {
  private static final Logger LOGGER = LogManager.getLogger();

  private static final String MARC_BIB_TABLE = "marc_bib_audit";
  private static final String MARC_AUTHORITY_TABLE = "marc_authority_audit";
  private static final String INSERT_SQL = "INSERT INTO %s (event_id, event_date, entity_id, origin, action, user_id, diff)" +
    " VALUES ($1, $2, $3, $4, $5, $6, $7)";

  private final PostgresClientFactory pgClientFactory;

  public MarcAuditDaoImpl(PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  @Override
  public Future<RowSet<Row>> save(MarcAuditEntity entity, String tenantId) {
    LOGGER.debug("save:: Saving Marc domain event with id: '{}' and record id: '{}'", entity.recordId(), entity.recordId());
    var tableName = tableName(entity.recordType());
    var query = format(INSERT_SQL, formatDBTableName(tenantId, tableName));
    return makeSaveCall(query, entity, tenantId)
      .onSuccess(rows -> LOGGER.info("save:: Saved Marc Bib event with id: '{}' and recordId: '{}' in to table '{}'", entity.eventId(), entity.recordId(), tableName))
      .onFailure(e -> LOGGER.error("save:: Failed to save Marc Bib event with id: '{}' and recordId: '{}' in to table '{}'", entity.recordId(), entity.recordId(), tableName, e));
  }

  private Future<RowSet<Row>> makeSaveCall(String query, MarcAuditEntity entity, String tenantId) {
    LOGGER.debug("makeSaveCall:: Making save call with query : {} and tenant id : {}", query, tenantId);
    try {
      return pgClientFactory.createInstance(tenantId).execute(query, Tuple.of(
        entity.eventId(),
        entity.eventDate(),
        entity.recordId(),
        entity.origin(),
        entity.action(),
        entity.userId(),
        JsonObject.mapFrom(entity.diff())));
    } catch (Exception e) {
      return Future.failedFuture(e);
    }
  }

  private String tableName(SourceRecordType recordType) {
    return SourceRecordType.MARC_BIB.equals(recordType) ? MARC_BIB_TABLE : MARC_AUTHORITY_TABLE;
  }
}
