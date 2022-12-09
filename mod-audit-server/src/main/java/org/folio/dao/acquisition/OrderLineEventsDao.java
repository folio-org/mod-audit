package org.folio.dao.acquisition;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.OrderLineAuditEvent;

public interface OrderLineEventsDao {

  /**
   * Saves orderLineAuditEvent entity to DB
   *
   * @param orderLineAuditEvent OrderLineAuditEvent entity to save
   * @param tenantId tenant id
   * @return future with created row
   */
  Future<RowSet<Row>> save(OrderLineAuditEvent orderLineAuditEvent, String tenantId);

}
