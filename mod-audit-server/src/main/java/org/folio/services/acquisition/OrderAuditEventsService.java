package org.folio.services.acquisition;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.OrderAuditEvent;

public interface OrderAuditEventsService {

  /**
   * Deduplication pattern implementation.
   * Collects deduplication data (information is event was already handled).
   * If events has not yet processed - future with Constraint violation exception will be returned.
   *
   * @param orderAuditEvent
   * @param tenantId id of tenant
   * @return successful future if event has not been processed, or failed future otherwise
   */
  Future<RowSet<Row>> collectData(OrderAuditEvent orderAuditEvent, String tenantId);
}
