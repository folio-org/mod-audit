package org.folio.services.acquisition;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.OrderAuditEvent;

public interface OrderAuditEventsService {

  /**
   * Saves OrderAuditEvent
   *
   * @param orderAuditEvent
   * @param tenantId id of tenant
   * @return successful future if event has not been processed, or failed future otherwise
   */
  Future<RowSet<Row>> saveOrderAuditEvent(OrderAuditEvent orderAuditEvent, String tenantId);
}
