package org.folio.dao;

import io.vertx.core.Future;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

import java.util.Date;

public interface OrderEvenDao {
  Future<RowSet<Row>> save(String id, String action, String orderId, String userId,
                           Date eventDate, Date action_date, String modifiedContent, String tenantId);
}
