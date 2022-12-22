package org.folio.services.acquisition;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.OrderAuditEvent;
import org.folio.rest.jaxrs.model.OrderAuditEventCollection;

public interface OrderAuditEventsService {

  /**
   * Saves OrderAuditEvent
   *
   * @param orderAuditEvent
   * @param tenantId id of tenant
   * @return successful future if event has not been processed, or failed future otherwise
   */
  Future<RowSet<Row>> saveOrderAuditEvent(OrderAuditEvent orderAuditEvent, String tenantId);

  /**
   * Searches for order audit events by order id
   *
   * @param orderId order id
   * @param limit limit
   * @param sortBy action date
   * @param offset offset
   *
   * @return future with OrderAuditEventCollection
   */
  Future<OrderAuditEventCollection> getAuditEventsByOrderId(String orderId, int limit, String sortBy, int offset, String tenantId);
}
