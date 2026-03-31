package org.folio.dao.user;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;
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

  /**
   * Deletes all user audit records within a transaction.
   *
   * @param conn     transaction connection
   * @param tenantId tenant id
   * @return Void future
   */
  Future<Void> deleteAll(Conn conn, String tenantId);

  /**
   * Anonymizes all user audit records by nullifying performed_by and removing
   * anonymized field paths from diff within a transaction.
   *
   * @param conn     transaction connection
   * @param tenantId tenant id
   * @return Void future
   */
  Future<Void> anonymizeAll(Conn conn, String tenantId);

  /**
   * Removes excluded field paths from all user audit record diffs within a transaction.
   * Both fieldChanges and collectionChanges matching the given paths (or nested under them)
   * are stripped. Records whose diff becomes empty are set to null.
   *
   * @param excludedPaths set of dot-delimited field paths to exclude
   * @param conn          transaction connection
   * @param tenantId      tenant id
   * @return Void future
   */
  Future<Void> excludeFieldsFromAll(Set<String> excludedPaths, Conn conn, String tenantId);

  /**
   * Deletes UPDATE records that have a null diff (e.g. after retroactive anonymization
   * removed all meaningful field changes).
   *
   * @param conn     transaction connection
   * @param tenantId tenant id
   * @return Void future
   */
  Future<Void> deleteEmptyUpdateRecords(Conn conn, String tenantId);

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
