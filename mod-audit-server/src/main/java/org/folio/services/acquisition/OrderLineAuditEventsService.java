package org.folio.services.acquisition;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.OrderLineAuditEvent;

public interface OrderLineAuditEventsService {

  /**
   * Saves OrderLineAuditEvent
   *
   * @param orderLineAuditEvent
   * @param tenantId id of tenant
   * @return successful future if event has not been processed, or failed future otherwise
   */
  Future<RowSet<Row>> saveOrderLineAuditEvent(OrderLineAuditEvent orderLineAuditEvent, String tenantId);

}
