package org.folio.dao.acquisition;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.OrderLineAuditEvent;
import org.folio.rest.jaxrs.model.OrderLineAuditEventCollection;

public interface OrderLineEventsDao {

  /**
   * Saves orderLineAuditEvent entity to DB
   *
   * @param orderLineAuditEvent OrderLineAuditEvent entity to save
   * @param tenantId tenant id
   * @return future with created row
   */
  Future<RowSet<Row>> save(OrderLineAuditEvent orderLineAuditEvent, String tenantId);

  /**
   * Searches for order_line audit events by id
   *
   * @param orderLineId OrderAuditEvent id
   * @param tenantId tenant id
   * @return future with OrderLineAuditEventCollection
   */
  Future<OrderLineAuditEventCollection> getAuditEventsByOrderLineId(String orderLineId, int limit, int offset, String tenantId);

}
