package org.folio.services.marc;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.MarcAuditCollection;
import java.sql.Timestamp;
import org.folio.util.marc.SourceRecordDomainEvent;
import org.folio.util.marc.SourceRecordType;

public interface MarcAuditService {

  /**
   * Persists a given MARC domain event in the system for auditing purposes.
   *
   * @param sourceRecordDomainEvent the domain event object representing a MARC record operation,
   *                                containing metadata, record type, event type, and payload.
   * @return a Future containing the result of the database operation, represented as a
   * RowSet of Row objects.
   */
  Future<RowSet<Row>> saveMarcDomainEvent(SourceRecordDomainEvent sourceRecordDomainEvent);


  /**
   * Retrieves a collection of MARC audit records based on the specified parameters.
   *
   * @param entityId   the unique identifier of the entity for which MARC audit records are requested.
   * @param recordType the type of source record (e.g., bibliographic or authority) as defined by the SourceRecordType enum.
   * @param tenantId   the identifier of the tenant requesting the MARC audit records.
   * @param eventsDateTime    event date time to seek from
   * @return a Future containing a MarcAuditCollection object that includes the requested MARC audit records.
   */
  Future<MarcAuditCollection> getMarcAuditRecords(String entityId, SourceRecordType recordType, String tenantId, String eventsDateTime);

  /**
   * Delete all MARC records which are expired
   *
   * @param tenantId id of tenant
   * @param expireOlderThan timestamp to expire records older than
   * @param recordType type of records to expire
   * @return Future void
   */
  Future<Void> expireRecords(String tenantId, Timestamp expireOlderThan, SourceRecordType recordType);
}
