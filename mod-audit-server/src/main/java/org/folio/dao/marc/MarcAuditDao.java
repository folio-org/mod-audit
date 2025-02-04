package org.folio.dao.marc;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

public interface MarcAuditDao {

  Future<RowSet<Row>> save(MarcAuditEntity auditEntity, String tenantId);
}
