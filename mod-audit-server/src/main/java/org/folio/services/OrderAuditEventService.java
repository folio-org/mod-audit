package org.folio.services;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

import java.util.Date;

public interface OrderAuditEventService {
  Future<RowSet<Row>> collectData(String id, String action, String orderId, String userId,
                                  Date eventDate, Date action_date, String modifiedContent, String tenantId);
}
