package org.folio.dao.user.impl;

import static org.folio.util.AuditEventDBConstants.ACTION_FIELD;
import static org.folio.util.AuditEventDBConstants.DIFF_FIELD;
import static org.folio.util.AuditEventDBConstants.EVENT_DATE_FIELD;
import static org.folio.util.AuditEventDBConstants.EVENT_ID_FIELD;
import static org.folio.util.AuditEventDBConstants.PERFORMED_BY_FIELD;
import static org.folio.util.AuditEventDBConstants.USER_ID_FIELD;
import static org.folio.util.DbUtils.formatDBTableName;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.user.UserAuditEntity;
import org.folio.dao.user.UserEventDao;
import org.folio.domain.diff.ChangeRecordDto;
import org.folio.rest.persist.Conn;
import org.folio.util.PostgresClientFactory;
import org.springframework.stereotype.Repository;

@Repository
public class UserEventDaoImpl implements UserEventDao {

  private static final Logger LOGGER = LogManager.getLogger();
  private static final String USER_AUDIT_TABLE = "user_audit";

  private static final String INSERT_SQL = """
    INSERT INTO %s (event_id, event_date, user_id, action, performed_by, diff)
    VALUES ($1, $2, $3, $4, $5, $6)
    """;

  private static final String DELETE_BY_USER_ID_SQL = """
    DELETE FROM %s
      WHERE user_id = $1
    """;

  private static final String DELETE_ALL_SQL = "DELETE FROM %s";

  private static final String SELECT_SQL = """
    SELECT * FROM %s
      WHERE user_id = $1 %s
      ORDER BY event_date DESC
      LIMIT $2
    """;

  private static final String COUNT_SQL = "SELECT COUNT(*) FROM %s WHERE user_id = $1";

  private static final String SEEK_BY_DATE_CLAUSE = "AND event_date < $3";

  private static final String ANONYMIZE_ALL_SQL = """
    UPDATE %s SET
      performed_by = NULL,
      diff = CASE WHEN diff IS NOT NULL THEN
        (SELECT CASE
           WHEN (filtered IS NULL OR filtered = '[]'::jsonb)
                AND (diff->>'collectionChanges' IS NULL OR diff->'collectionChanges' = '[]'::jsonb)
           THEN NULL
           ELSE jsonb_set(diff, '{fieldChanges}', COALESCE(filtered, '[]'::jsonb))
         END
         FROM (SELECT jsonb_agg(elem) AS filtered
               FROM jsonb_array_elements(
                 CASE WHEN jsonb_typeof(diff->'fieldChanges') = 'array'
                      THEN diff->'fieldChanges' ELSE '[]'::jsonb END) elem
               WHERE elem->>'fullPath' NOT IN ('metadata.createdByUserId', 'metadata.updatedByUserId')
         ) sub)
        ELSE NULL END
    WHERE performed_by IS NOT NULL
       OR diff @? '$.fieldChanges[*] ? (@.fullPath == "metadata.createdByUserId" || @.fullPath == "metadata.updatedByUserId")'
    """;

  private static final String DELETE_EMPTY_UPDATE_RECORDS_SQL =
    "DELETE FROM %s WHERE action = 'UPDATED' AND diff IS NULL";

  private final PostgresClientFactory pgClientFactory;

  public UserEventDaoImpl(PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
  }

  @Override
  public Future<List<UserAuditEntity>> get(UUID userId, Timestamp eventTs, int limit, String tenantId) {
    LOGGER.debug("get:: Retrieve records by [tenantId: {}, userId: {}, eventTs before: {}, limit: {}]",
      tenantId, userId, eventTs, limit);
    var table = formatDBTableName(tenantId, tableName());
    var query = SELECT_SQL.formatted(table, eventTs == null ? "" : SEEK_BY_DATE_CLAUSE);
    return pgClientFactory.createInstance(tenantId).execute(query, eventTs == null
                                                                   ? Tuple.of(userId, limit)
                                                                   : Tuple.of(userId, limit, LocalDateTime.ofInstant(eventTs.toInstant(), ZoneId.systemDefault())))
      .map(this::mapRowToUserAuditEntityList);
  }

  @Override
  public Future<Integer> count(UUID userId, String tenantId) {
    LOGGER.debug("count:: Count records by [tenantId: {}, userId: {}]", tenantId, userId);
    var table = formatDBTableName(tenantId, tableName());
    var query = COUNT_SQL.formatted(table);
    return pgClientFactory.createInstance(tenantId).selectSingle(query, Tuple.of(userId))
      .map(row -> row.getInteger(0));
  }

  @Override
  public Future<RowSet<Row>> save(UserAuditEntity event, String tenantId) {
    LOGGER.debug("save:: Trying to save UserAuditEntity with [tenantId: {}, eventId: {}, userId: {}]",
      tenantId, event.eventId(), event.userId());
    var promise = Promise.<RowSet<Row>>promise();
    var table = formatDBTableName(tenantId, tableName());
    var query = INSERT_SQL.formatted(table);
    makeSaveCall(promise, query, event, tenantId);
    return promise.future();
  }

  @Override
  public Future<Void> deleteByUserId(UUID userId, String tenantId) {
    LOGGER.debug("deleteByUserId:: Deleting user audit records with [tenantId: {}, userId: {}]",
      tenantId, userId);
    var table = formatDBTableName(tenantId, tableName());
    var query = DELETE_BY_USER_ID_SQL.formatted(table);
    return pgClientFactory.createInstance(tenantId).execute(query, Tuple.of(userId))
      .mapEmpty();
  }

  @Override
  public Future<Void> deleteAll(Conn conn, String tenantId) {
    LOGGER.debug("deleteAll:: Deleting all user audit records with [tenantId: {}]", tenantId);
    var table = formatDBTableName(tenantId, tableName());
    var query = DELETE_ALL_SQL.formatted(table);
    return conn.execute(query).mapEmpty();
  }

  @Override
  public Future<Void> anonymizeAll(Conn conn, String tenantId) {
    LOGGER.info("anonymizeAll:: Anonymizing all user audit records [tenantId: {}]", tenantId);
    return conn.execute(buildAnonymizeAllQuery(tenantId)).mapEmpty();
  }

  @Override
  public Future<Void> deleteEmptyUpdateRecords(Conn conn, String tenantId) {
    LOGGER.info("deleteEmptyUpdateRecords:: Deleting UPDATE records with null diff [tenantId: {}]", tenantId);
    return conn.execute(buildDeleteEmptyUpdateRecordsQuery(tenantId)).mapEmpty();
  }

  @Override
  public String tableName() {
    return USER_AUDIT_TABLE;
  }

  private String buildAnonymizeAllQuery(String tenantId) {
    return ANONYMIZE_ALL_SQL.formatted(formatDBTableName(tenantId, tableName()));
  }

  private String buildDeleteEmptyUpdateRecordsQuery(String tenantId) {
    return DELETE_EMPTY_UPDATE_RECORDS_SQL.formatted(formatDBTableName(tenantId, tableName()));
  }

  private void makeSaveCall(Promise<RowSet<Row>> promise, String query, UserAuditEntity event, String tenantId) {
    LOGGER.debug("makeSaveCall:: Making save call with query : {} and tenant id : {}", query, tenantId);
    try {
      pgClientFactory.createInstance(tenantId).execute(query, Tuple.of(event.eventId(),
          LocalDateTime.ofInstant(event.eventDate().toInstant(), ZoneId.systemDefault()),
          event.userId(),
          event.action(),
          event.performedBy(),
          event.diff() != null ? JsonObject.mapFrom(event.diff()) : null),
        promise);
      LOGGER.info("makeSaveCall:: Saving UserAuditEntity with [tenantId: {}, eventId:{}, userId:{}]",
        tenantId, event.eventId(), event.userId());
    } catch (Exception e) {
      LOGGER.error("Failed to save record with [eventId:{}, userId:{}, tableName: {}]",
        event.eventId(), event.userId(), tableName(), e);
      promise.fail(e);
    }
  }

  private List<UserAuditEntity> mapRowToUserAuditEntityList(RowSet<Row> rowSet) {
    LOGGER.debug("mapRowToUserAuditEntityList:: Mapping row set to List of User Audit Entities");
    if (rowSet.rowCount() == 0) {
      return new LinkedList<>();
    }
    var entities = new LinkedList<UserAuditEntity>();
    rowSet.iterator().forEachRemaining(row ->
      entities.add(mapRowToUserAuditEntity(row)));
    LOGGER.debug("mapRowToUserAuditEntityList:: Mapped row set to List of User Audit Entities");
    return entities;
  }

  private UserAuditEntity mapRowToUserAuditEntity(Row row) {
    LOGGER.debug("mapRowToUserAuditEntity:: Mapping row to User Audit Entity");
    var diffJson = row.getJsonObject(DIFF_FIELD);
    return new UserAuditEntity(
      row.getUUID(EVENT_ID_FIELD),
      new Timestamp(ZonedDateTime.of(row.getLocalDateTime(EVENT_DATE_FIELD), ZoneId.systemDefault()).toInstant().toEpochMilli()),
      row.getUUID(USER_ID_FIELD),
      row.getString(ACTION_FIELD),
      row.getUUID(PERFORMED_BY_FIELD),
      diffJson == null ? null : diffJson.mapTo(ChangeRecordDto.class)
    );
  }
}
