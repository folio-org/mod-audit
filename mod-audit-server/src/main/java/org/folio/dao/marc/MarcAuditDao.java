package org.folio.dao.marc;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

public interface MarcAuditDao {

  /**
   * Saves a MarcAuditEntity to the database.
   *
   * @param auditEntity the MarcAuditEntity to be saved
   * @param tenantId the tenant identifier
   * @return a Future representing the result of the save operation, containing a RowSet of Rows
   */
  Future<RowSet<Row>> save(MarcAuditEntity auditEntity, String tenantId);
}
