package org.folio.dao.user.impl;

import static org.folio.util.DbUtils.formatDBTableName;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.dao.user.UserAuditEntity;
import org.folio.dao.user.UserEventDao;
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

  private final PostgresClientFactory pgClientFactory;

  public UserEventDaoImpl(PostgresClientFactory pgClientFactory) {
    this.pgClientFactory = pgClientFactory;
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
  public String tableName() {
    return USER_AUDIT_TABLE;
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
}
