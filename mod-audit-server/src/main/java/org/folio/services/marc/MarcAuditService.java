package org.folio.services.marc;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.MarcAuditCollection;
import org.folio.util.marc.SourceRecordDomainEvent;
import org.folio.util.marc.SourceRecordType;

import java.util.UUID;

public interface MarcAuditService {

  /**
   * Persists a given MARC domain event in the system for auditing purposes.
   *
   * @param sourceRecordDomainEvent the domain event object representing a MARC record operation,
   *                                containing metadata, record type, event type, and payload.
   * @return a Future containing the result of the database operation, represented as a
   *         RowSet of Row objects.
   */
  Future<RowSet<Row>> saveMarcDomainEvent(SourceRecordDomainEvent sourceRecordDomainEvent);


  /**
   * Retrieves a collection of MARC audit records based on the specified parameters.
   *
   * @param entityId the unique identifier of the entity for which MARC audit records are requested.
   * @param recordType the type of source record (e.g., bibliographic or authority) as defined by the SourceRecordType enum.
   * @param tenantId the identifier of the tenant requesting the MARC audit records.
   * @param limit the maximum number of records to include in the result set.
   * @param offset the starting position of the result set for pagination purposes.
   * @return a Future containing a MarcAuditCollection object that includes the requested MARC audit records.
   */
  Future<MarcAuditCollection> getMarcAuditRecords(String entityId, SourceRecordType recordType, String tenantId, int limit, int offset);
}
