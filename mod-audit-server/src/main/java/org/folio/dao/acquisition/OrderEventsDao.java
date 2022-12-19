package org.folio.dao.acquisition;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.jaxrs.model.OrderAuditEventCollection;

public interface OrderEventsDao {

  /**
   * Saves orderAuditEvent entity to DB
   *
   * @param orderAuditEvent OrderAuditEvent entity to save
   * @param tenantId tenant id
   * @return future with created row
   */
  Future<RowSet<Row>> save(OrderAuditEvent orderAuditEvent, String tenantId);

  /**
   * Searches for order audit events by id
   *
   * @param orderId OrderAuditEvent id
   * @param tenantId tenant id
   * @return future with OrderAuditEventCollection
   */
  Future<OrderAuditEventCollection> getAuditEventsByOrderId(String orderId, int limit, int offset, String tenantId);
}
