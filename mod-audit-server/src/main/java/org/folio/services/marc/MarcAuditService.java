package org.folio.services.marc;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.util.marc.SourceRecordDomainEvent;

public interface MarcAuditService {

  Future<RowSet<Row>> saveMarcDomainEvent(SourceRecordDomainEvent invoiceAuditEvent);
}
