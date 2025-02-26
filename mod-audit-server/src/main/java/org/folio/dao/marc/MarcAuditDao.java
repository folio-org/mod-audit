package org.folio.dao.marc;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import java.sql.Timestamp;
import org.folio.util.marc.SourceRecordType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MarcAuditDao {

  /**
   * Persists a MarcAuditEntity record in the database.
   *
   * @param auditEntity the MarcAuditEntity object containing the event details to be saved
   * @param recordType  the type of the source record (e.g., MARC_BIB or MARC_AUTHORITY)
   * @param tenantId    the identifier of the tenant
   * @return a Future containing a RowSet of Row objects representing the result of the database operation
   */
  Future<RowSet<Row>> save(MarcAuditEntity auditEntity, SourceRecordType recordType, String tenantId);

  /**
   * Retrieves a list of MarcAuditEntity records from the database based on the given parameters.
   *
   * @param entityId  the unique identifier of the entity for which audit records are retrieved
   * @param recordType the type of the source record (e.g., MARC_BIB or MARC_AUTHORITY)
   * @param tenantId   the identifier of the tenant
   * @param eventDate event date to seek from
   * @param limit      the maximum number of records to retrieve
   * @return a Future containing a List of MarcAuditEntity objects matching the specified parameters
   */
  Future<List<MarcAuditEntity>> get(UUID entityId, SourceRecordType recordType, String tenantId, LocalDateTime eventDate, int limit);

  /**
   * Deletes entity records from DB older than eventDate
   *
   * @param eventDate event date, before which records should be deleted
   * @param tenantId  tenant id
   * @return future with result list
   */
  Future<Void> deleteOlderThanDate(Timestamp eventDate, String tenantId, SourceRecordType recordType);
}
