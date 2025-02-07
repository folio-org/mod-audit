package org.folio.services.marc;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.util.marc.SourceRecordDomainEvent;

public interface MarcAuditService {

  /**
   * Saves a Marc domain event data to the database.
   * The event is mapped to an entity and the difference between the old and new record in the event is calculated.
   * The difference will be stored as part of the entity.
   *
   * @param invoiceAuditEvent the SourceRecordDomainEvent to be saved
   * @return a Future representing the result of the save operation, containing a RowSet of Rows
   */
  Future<RowSet<Row>> saveMarcDomainEvent(SourceRecordDomainEvent invoiceAuditEvent);
}
