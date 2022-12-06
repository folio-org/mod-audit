package org.folio.services;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.OrderAuditEvent;

public interface OrderAuditEventService {
  Future<RowSet<Row>> collectData(OrderAuditEvent orderAuditEvent, String tenantId);
}
