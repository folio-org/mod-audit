package org.folio.dao.marc.impl;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.marc.MarcAuditDao;
import org.folio.dao.marc.MarcAuditEntity;
import org.folio.domain.diff.ChangeRecordDto;
import org.folio.util.PostgresClientFactory;
import org.folio.util.marc.SourceRecordType;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.folio.util.AuditEventDBConstants.ACTION_FIELD;
import static org.folio.util.AuditEventDBConstants.DIFF_FIELD;
import static org.folio.util.AuditEventDBConstants.ENTITY_ID_FIELD;
import static org.folio.util.AuditEventDBConstants.EVENT_DATE_FIELD;
import static org.folio.util.AuditEventDBConstants.EVENT_ID_FIELD;
import static org.folio.util.AuditEventDBConstants.ORIGIN_FIELD;
import static org.folio.util.AuditEventDBConstants.USER_ID_FIELD;
import static org.folio.util.DbUtils.formatDBTableName;

@Repository
public class MarcAuditDaoImpl implements MarcAuditDao {
  private static final Logger LOGGER = LogManager.getLogger();

  private static final String MARC_BIB_TABLE = "marc_bib_audit";
  private static final String MARC_AUTHORITY_TABLE = "marc_authority_audit";
  private static final String INSERT_SQL = """
    INSERT INTO %s (event_id, event_date, entity_id, origin, action, user_id, diff)
    VALUES ($1, $2, $3, $4, $5, $6, $7)
    """;

  private static final String SELECT_SQL = """
    SELECT * FROM %s
      WHERE entity_id = $1 %s
      ORDER BY event_date DESC
      LIMIT $2
    """;

  private static final String COUNT_SQL = """
    SELECT COUNT(*) FROM %s
      WHERE entity_id = $1
    """;

  private static final String SEEK_BY_DATE_CLAUSE = "AND event_date < $3";

  private static final String DELETE_OLDER_THAN_DATE_SQL = """
    DELETE FROM %s
      WHERE event_date < $1
    """;

  private final PostgresClientFactory pgClientFactory;

  public MarcAuditDaoImpl(PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  @Override
  public Future<RowSet<Row>> save(MarcAuditEntity entity, SourceRecordType recordType, String tenantId) {
    LOGGER.debug("save:: Saving Marc domain event with id: '{}' and record id: '{}'", entity.entityId(), entity.entityId());
    var tableName = tableName(recordType);
    var query = INSERT_SQL.formatted(formatDBTableName(tenantId, tableName));
    return makeSaveCall(query, entity, tenantId)
      .onSuccess(rows -> LOGGER.info("save:: Saved Marc domain event with id: '{}' and entityId: '{}' in to table '{}'", entity.eventId(), entity.entityId(), tableName))
      .onFailure(e -> LOGGER.error("save:: Failed to save Marc domain event with id: '{}' and entityId: '{}' in to table '{}'", entity.eventId(), entity.entityId(), tableName, e));
  }

  @Override
  public Future<List<MarcAuditEntity>> get(UUID entityId, SourceRecordType recordType, String tenantId, LocalDateTime eventDate, int limit) {
    LOGGER.debug("get:: Retrieve records by tenantId: '{}', entityId: '{}' and record type '{}'", tenantId, entityId, recordType);
    var tableName = tableName(recordType);
    var query = SELECT_SQL.formatted(formatDBTableName(tenantId, tableName), eventDate == null ? "" : SEEK_BY_DATE_CLAUSE);
    var tuple = eventDate == null ? Tuple.of(entityId, limit) : Tuple.of(entityId, limit, eventDate);
    return pgClientFactory.createInstance(tenantId).execute(query, tuple)
      .map(this::mapRowToAuditEntityList);
  }

  @Override
  public Future<Integer> count(UUID entityId, SourceRecordType recordType, String tenantId) {
    LOGGER.debug("count:: Retrieving total count by tenantId: '{}', entityId: '{}', recordType '{}'", tenantId, entityId, recordType);
    var tableName = tableName(recordType);
    var query = COUNT_SQL.formatted(formatDBTableName(tenantId, tableName));
    return pgClientFactory.createInstance(tenantId).selectSingle(query, Tuple.of(entityId))
      .map(row -> row.getInteger(0));
  }

  @Override
  public Future<Void> deleteOlderThanDate(Timestamp eventDate, String tenantId, SourceRecordType recordType) {
    LOGGER.debug("deleteOlderThanDate:: Delete records by [tenantId: {}, eventDate: {}, recordType: {}]",
      tenantId, eventDate, recordType);
    var table = formatDBTableName(tenantId, tableName(recordType));
    var query = DELETE_OLDER_THAN_DATE_SQL.formatted(table);
    return pgClientFactory.createInstance(tenantId).execute(query, Tuple.of(
        LocalDateTime.ofInstant(eventDate.toInstant(), ZoneId.systemDefault())))
      .mapEmpty();
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

  private List<MarcAuditEntity> mapRowToAuditEntityList(RowSet<Row> rowSet) {
    LOGGER.debug("mapRowToAuditEntityList:: Mapping row set to List of Marc Audit Entities");
    if (rowSet.rowCount() == 0) {
      return new LinkedList<>();
    }
    var entities = new LinkedList<MarcAuditEntity>();
    rowSet.iterator().forEachRemaining(row ->
      entities.add(mapRowToAuditEntity(row)));
    LOGGER.debug("mapRowToAuditEntityList:: Mapped row set to List of Marc Audit Entities");
    return entities;
  }

  private MarcAuditEntity mapRowToAuditEntity(Row row) {
    LOGGER.debug("mapRowToAuditEntity:: Mapping row to Marc Audit Entity");
    var diffJson = row.getJsonObject(DIFF_FIELD);
    return new MarcAuditEntity(
      row.getUUID(EVENT_ID_FIELD).toString(),
      row.getLocalDateTime(EVENT_DATE_FIELD),
      row.getUUID(ENTITY_ID_FIELD).toString(),
      row.getString(ORIGIN_FIELD),
      row.getString(ACTION_FIELD),
      row.getUUID(USER_ID_FIELD).toString(),
      diffJson == null ? null : diffJson.mapTo(ChangeRecordDto.class)
    );
  }

  @Override
  public String tableName(SourceRecordType recordType) {
    return SourceRecordType.MARC_BIB.equals(recordType) ? MARC_BIB_TABLE : MARC_AUTHORITY_TABLE;
  }
}
