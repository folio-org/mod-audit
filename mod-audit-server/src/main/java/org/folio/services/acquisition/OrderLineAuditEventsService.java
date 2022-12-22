package org.folio.services.acquisition;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.OrderLineAuditEvent;
import org.folio.rest.jaxrs.model.OrderLineAuditEventCollection;

public interface OrderLineAuditEventsService {

  /**
   * Saves OrderLineAuditEvent
   *
   * @param orderLineAuditEvent order line event to save
   * @param tenantId id of tenant
   * @return successful future if event has not been processed, or failed future otherwise
   */
  Future<RowSet<Row>> saveOrderLineAuditEvent(OrderLineAuditEvent orderLineAuditEvent, String tenantId);

  /**
   * Searches for order line audit events by order line id
   *
   * @param orderLineId order line id
   * @param limit limit
   * @param sortBy action date
   * @param offset offset
   *
   * @return future with OrderLineAuditEventCollection
   */
  Future<OrderLineAuditEventCollection> getAuditEventsByOrderLineId(String orderLineId, int limit, String sortBy, int offset, String tenantId);
}
