package org.folio.dao.marc;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.util.marc.SourceRecordType;

public interface MarcBibAuditDao {

  Future<RowSet<Row>> save(MarcAuditEntity auditEntity, SourceRecordType recordType, String tenantId);
}
