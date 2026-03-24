package org.folio.dao.user;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;
import org.folio.rest.persist.Conn;

public interface UserEventDao {

  /**
   * Saves userAuditEntity to DB
   *
   * @param userAuditEntity UserAuditEntity to save
   * @param tenantId        tenant id
   * @return future with created row
   */
  Future<RowSet<Row>> save(UserAuditEntity userAuditEntity, String tenantId);

  /**
   * Retrieves user audit entities from DB filtered by userId
   * and seeking by eventDate descending, not including.
   *
   * @param userId   user id
   * @param eventTs  event date to seek from, or null for first page
   * @param limit    number of records to return
   * @param tenantId tenant id
   * @return future with result list
   */
  Future<List<UserAuditEntity>> get(UUID userId, Timestamp eventTs, int limit, String tenantId);

  /**
   * Counts user audit entities by userId
   *
   * @param userId   user id
   * @param tenantId tenant id
   * @return future with count
   */
  Future<Integer> count(UUID userId, String tenantId);

  /**
   * Deletes all audit records for a given user
   *
   * @param userId   user id
   * @param tenantId tenant id
   * @return Void future
   */
  Future<Void> deleteByUserId(UUID userId, String tenantId);

  Future<Void> deleteAll(Conn conn, String tenantId);

  /**
   * Deletes all audit records older than the given date
   *
   * @param eventDate date threshold
   * @param tenantId  tenant id
   * @return Void future
   */
  Future<Void> deleteOlderThanDate(Timestamp eventDate, String tenantId);

  Future<Void> deleteOlderThanDate(Timestamp eventDate, Conn conn, String tenantId);

  /**
   * Returns audit table name
   *
   * @return table name
   */
  String tableName();
}
