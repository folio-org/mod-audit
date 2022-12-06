package org.folio.dao;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import org.folio.rest.jaxrs.model.OrderAuditEvent;

import java.util.Date;

public interface OrderEventDao {
  Future<RowSet<Row>> save(OrderAuditEvent orderAuditEvent, String tenantId);
}
